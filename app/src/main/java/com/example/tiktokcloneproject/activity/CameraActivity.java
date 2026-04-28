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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class CameraActivity extends Activity implements View.OnClickListener {
    private CameraManager manager;
    private FrameLayout cameraFrameLayout;
    private TextureView textureFront;
    private boolean isPermissionRequested = false;

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
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraDevice.StateCallback cameraDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mainCamera = cameraDevice;
            startCameraPreview();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mainCamera = null;
        }
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            mainCamera = null;
        }
    };

    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        createVideoFolder();
        mediaRecorder = new MediaRecorder();
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
        } else if (!isPermissionRequested) {
            isPermissionRequested = true;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 101);
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void connectCamera() {
        if (!hasPermissions() || defaultId == null) return;
        manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            manager.openCamera(defaultId, cameraDeviceCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e("CameraActivity", "Cannot access camera", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            isPermissionRequested = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (textureFront.isAvailable()) {
                    setupCamera(textureFront.getWidth(), textureFront.getHeight());
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.imb_flip_camera) {
            flipCamera();
        } else if (view.getId() == R.id.button_record) {
            startRecordingProcess();
        } else if (view.getId() == R.id.button_stop) {
            stopRecordingProcess();
        } else if (view.getId() == R.id.button_close) {
            finish();
        }
    }

    private void flipCamera() {
        if (mainCamera != null) {
            mainCamera.close();
            mainCamera = null;
        }
        btnFlip.startAnimation(animRotate);
        defaultId = Objects.equals(defaultId, frontId) ? backId : frontId;
        connectCamera();
    }

    private void startRecordingProcess() {
        btnStopRecording.setVisibility(View.VISIBLE);
        btnFlip.setVisibility(View.GONE);
        btnStartRecording.setVisibility(View.GONE);
        isRecording = true;
        try {
            videoFileHolder = createVideoFileName();
            startRecord();
            mediaRecorder.start();
        } catch (IOException e) {
            Log.e("CameraActivity", "Record failed", e);
        }
    }

    private void stopRecordingProcess() {
        if (isRecording) {
            try {
                mediaRecorder.stop();
            } catch (Exception e) {
                Log.e("CameraActivity", "Stop fail", e);
            }
            mediaRecorder.reset();
            isRecording = false;
            MediaScannerConnection.scanFile(this, new String[]{videoFileHolder.getAbsolutePath()}, null, (path, uri) -> {
                // Sử dụng uri trả về từ MediaScanner thay vì Uri.fromFile(new File(path))
                runOnUiThread(() -> startUploadingActivity(uri != null ? uri : Uri.fromFile(new File(path))));
            });
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
            Log.e("CameraActivity", "Setup error", e);
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

    private void startCameraPreview() {
        SurfaceTexture st = textureFront.getSurfaceTexture();
        if (st == null || mainCamera == null) return;
        st.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface previewSurface = new Surface(st);
        try {
            CaptureRequest.Builder builder = mainCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(previewSurface);
            mainCamera.createCaptureSession(Collections.singletonList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(builder.build(), null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e("CameraActivity", "Session fail", e);
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e("CameraActivity", "Preview error", e);
        }
    }

    private void startBackgroundThread() {
        backgroundHandlerThread = new HandlerThread("CameraThread");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundHandlerThread != null) {
            backgroundHandlerThread.quitSafely();
            try {
                backgroundHandlerThread.join();
                backgroundHandlerThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e("CameraActivity", "Thread stop failed", e);
            }
        }
    }

    private void createVideoFolder() {
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "TopTopVideos");
        if (!folder.exists()) folder.mkdirs();
        videoFolder = folder.getAbsolutePath();
    }

    private File createVideoFileName() throws IOException {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File file = File.createTempFile(userId + "_" + ts, ".mp4", new File(videoFolder));
        videoFileName = file.getAbsolutePath();
        return file;
    }

    private void setupMediaRecorder() throws IOException {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(videoFileName);
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        mediaRecorder.setOrientationHint(ORIENTATIONS.get(rotation));
        mediaRecorder.prepare();
    }

    private void startRecord() {
        try {
            setupMediaRecorder();
            SurfaceTexture st = textureFront.getSurfaceTexture();
            st.setDefaultBufferSize(videoSize.getWidth(), videoSize.getHeight());
            Surface previewSurface = new Surface(st);
            Surface recordSurface = mediaRecorder.getSurface();
            captureRequestBuilder = mainCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.addTarget(recordSurface);
            mainCamera.createCaptureSession(Arrays.asList(previewSurface, recordSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e("CameraActivity", "Session fail", e);
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
            }, backgroundHandler);
        } catch (Exception e) {
            Log.e("CameraActivity", "Record fail", e);
        }
    }

    private void startUploadingActivity(Uri videoUri) {
        Intent i = new Intent(this, DescriptionVideoActivity.class);
        i.putExtra("videoUri", videoUri.toString());
        startActivity(i);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBackgroundThread();
        if (mainCamera != null) {
            mainCamera.close();
            mainCamera = null;
        }
    }
}
