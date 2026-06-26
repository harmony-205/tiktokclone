package com.example.tiktokcloneproject.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.tiktokcloneproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

public class CameraActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_PERMISSIONS = 101;
    private static final int PICK_VIDEO_REQUEST = 102;
    
    private CameraManager manager;
    private FrameLayout cameraFrameLayout;
    private TextureView textureFront;

    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            setupCamera(i, i1);
        }
        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {}
        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) { return false; }
        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {}
    };

    private String frontId, backId, defaultId;
    private Size previewSize, videoSize;
    private Button btnUploadVideo, btnClose, btnStopRecording, btnStartRecording;
    private ImageButton btnFlip;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private MediaRecorder mediaRecorder;
    private File videoFileHolder;
    private boolean isRecording = false;
    private String videoFileName, userId, videoFolder;
    private Animation animRotate;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private CameraDevice mainCamera;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    
    private CameraDevice.StateCallback cameraDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mainCamera = cameraDevice;
            try {
                setupMediaRecorder();
                startCameraSession();
            } catch (IOException e) {
                Log.e(TAG, "MediaRecorder setup failed", e);
            }
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            closeCamera();
        }
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            closeCamera();
        }
    };

    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        createVideoFolder();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        userId = (user != null) ? user.getUid() : "anonymous";

        btnUploadVideo = findViewById(R.id.btnUploadVideo);
        btnStartRecording = findViewById(R.id.button_record);
        btnClose = findViewById(R.id.button_close);
        btnFlip = findViewById(R.id.imb_flip_camera);
        btnStopRecording = findViewById(R.id.button_stop);

        btnUploadVideo.setOnClickListener(this);
        btnStartRecording.setOnClickListener(this);
        btnClose.setOnClickListener(this);
        btnFlip.setOnClickListener(this);
        btnStopRecording.setOnClickListener(this);

        cameraFrameLayout = findViewById(R.id.camera_frame);
        textureFront = findViewById(R.id.texture_view_front);
        animRotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
        
        if (!hasPermissions()) {
            requestPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureFront.isAvailable()) {
            setupCamera(textureFront.getWidth(), textureFront.getHeight());
        } else {
            textureFront.setSurfaceTextureListener(textureListener);
        }
    }

    private void setupCamera(int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (hasPermissions()) {
            getCameraIds();
            connectCamera();
        }
    }

    private boolean hasPermissions() {
        boolean base = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return base && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return base && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        String[] perms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_VIDEO};
        } else {
            perms = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        ActivityCompat.requestPermissions(this, perms, REQUEST_PERMISSIONS);
    }

    @SuppressLint("MissingPermission")
    private void connectCamera() {
        if (!hasPermissions() || defaultId == null || mainCamera != null) return;
        manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            manager.openCamera(defaultId, cameraDeviceCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot access camera", e);
        }
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (mainCamera != null) {
            mainCamera.close();
            mainCamera = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                if (textureFront.isAvailable()) setupCamera(textureFront.getWidth(), textureFront.getHeight());
            } else {
                Toast.makeText(this, "Permissions required!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.imb_flip_camera) {
            flipCamera();
        } else if (id == R.id.button_record) {
            startRecording();
        } else if (id == R.id.button_stop) {
            stopRecording();
        } else if (id == R.id.button_close) {
            finish();
        } else if (id == R.id.btnUploadVideo) {
            pickVideoFromGallery();
        }
    }

    private void pickVideoFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedVideo = data.getData();
            startUploadingActivity(selectedVideo);
        }
    }

    private void flipCamera() {
        closeCamera();
        btnFlip.startAnimation(animRotate);
        defaultId = Objects.equals(defaultId, frontId) ? backId : frontId;
        connectCamera();
    }

    private void startRecording() {
        if (isRecording || mediaRecorder == null || cameraCaptureSession == null) return;
        try {
            Surface recordSurface = mediaRecorder.getSurface();
            captureRequestBuilder.addTarget(recordSurface);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
            mediaRecorder.start();
            isRecording = true;
            btnStopRecording.setVisibility(View.VISIBLE);
            btnFlip.setVisibility(View.GONE);
            btnStartRecording.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "Recording start failed", e);
        }
    }

    private void stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder.stop();
            } catch (Exception e) {
                Log.e(TAG, "Stop fail", e);
            }
            mediaRecorder.reset();
            isRecording = false;
            MediaScannerConnection.scanFile(this, new String[]{videoFileName}, null, null);
            runOnUiThread(() -> startUploadingActivity(Uri.fromFile(new File(videoFileName))));
        }
    }

    private void getCameraIds() {
        manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(id);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) frontId = id;
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) backId = id;
            }
            if (defaultId == null) defaultId = backId != null ? backId : frontId;
            StreamConfigurationMap map = manager.getCameraCharacteristics(defaultId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                previewSize = map.getOutputSizes(SurfaceTexture.class)[0];
                videoSize = map.getOutputSizes(MediaRecorder.class)[0];
                runOnUiThread(() -> setTextureViewSize(previewSize));
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Setup error", e);
        }
    }

    private void setTextureViewSize(Size size) {
        cameraFrameLayout.post(() -> {
            int currentWidth = cameraFrameLayout.getWidth();
            if (currentWidth == 0) return;
            float ratio = (float) size.getWidth() / size.getHeight();
            int newHeight = Math.round(currentWidth * ratio);
            cameraFrameLayout.setLayoutParams(new FrameLayout.LayoutParams(currentWidth, newHeight));
        });
    }

    private void startCameraSession() {
        SurfaceTexture st = textureFront.getSurfaceTexture();
        if (st == null || mainCamera == null || mediaRecorder == null) return;
        st.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface previewSurface = new Surface(st);
        Surface recordSurface = mediaRecorder.getSurface();
        try {
            captureRequestBuilder = mainCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(previewSurface);
            mainCamera.createCaptureSession(Arrays.asList(previewSurface, recordSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    try {
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Session fail", e);
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Capture session error", e);
        }
    }

    private void setupMediaRecorder() throws IOException {
        if (mediaRecorder == null) mediaRecorder = new MediaRecorder();
        else mediaRecorder.reset();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        videoFileHolder = File.createTempFile(userId + "_" + ts, ".mp4", getExternalFilesDir(Environment.DIRECTORY_MOVIES));
        videoFileName = videoFileHolder.getAbsolutePath();
        mediaRecorder.setOutputFile(videoFileName);
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        
        // Sửa lỗi hướng video: Tính toán lại góc xoay
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int orientationHint = ORIENTATIONS.get(rotation);
        mediaRecorder.setOrientationHint(orientationHint);

        mediaRecorder.prepare();
    }

    private void startBackgroundThread() {
        if (backgroundHandlerThread == null) {
            backgroundHandlerThread = new HandlerThread("CameraThread");
            backgroundHandlerThread.start();
            backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        if (backgroundHandlerThread != null) {
            backgroundHandlerThread.quitSafely();
            try {
                backgroundHandlerThread.join();
                backgroundHandlerThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {}
        }
    }

    private void createVideoFolder() {
        File folder = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (folder != null && !folder.exists()) folder.mkdirs();
        videoFolder = folder != null ? folder.getAbsolutePath() : getFilesDir().getAbsolutePath();
    }

    private void startUploadingActivity(Uri videoUri) {
        Intent i = new Intent(this, DescriptionVideoActivity.class);
        i.putExtra("videoUri", videoUri.toString());
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(i);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
}
