package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.model.Video;
import com.example.tiktokcloneproject.model.VideoSummary;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DescriptionVideoActivity extends FragmentActivity implements View.OnClickListener {
    EditText edtDescription;
    Button btnDescription;
    ImageView imvShortCutVideo;
    final String REGEX_HASHTAG = "#([A-Za-z0-9_-]+)";

    String username;
    Uri videoUri;
    final long maximumDuration = 60000;

    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;

    ArrayList<String> hashtags;
    String Id;
    final String TAG = "DescriptionVideoActivity";

    Bitmap thumbnail;
    NotificationManagerCompat mNotifyManager;
    NotificationCompat.Builder mBuilder;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int lastProgress = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description_video);

        // Khởi tạo Cloudinary
        initCloudinary();

        edtDescription = findViewById(R.id.edtDescription);
        btnDescription = findViewById(R.id.btnDescription);
        imvShortCutVideo = findViewById(R.id.imvShortCutVideo);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        String videoPath = intent.getStringExtra("videoUri");
        if (videoPath != null) {
            videoUri = Uri.parse(videoPath);
            loadVideoMetadata();
        } else {
            Toast.makeText(this, "Video not found!", Toast.LENGTH_SHORT).show();
            finish();
        }
        hashtags = new ArrayList<>();

        createNotificationChannel();
        mNotifyManager = NotificationManagerCompat.from(this);
        mBuilder = new NotificationCompat.Builder(this, "Video")
                .setContentTitle("Toptop Upload")
                .setContentText("Uploading to Cloudinary...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_download)
                .setOnlyAlertOnce(true);

        btnDescription.setOnClickListener(this);
    }

    private void initCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dmygicxxy");
            config.put("api_key", "172799859182813");
            config.put("api_secret", "PAtcEgMVj8evo3bGG4oayayKf20");
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // Đã khởi tạo
        }
    }

    private void loadVideoMetadata() {
        executorService.execute(() -> {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(this, videoUri);
                thumbnail = mmr.getScaledFrameAtTime(1000000, MediaMetadataRetriever.OPTION_NEXT_SYNC, 500, 500);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (thumbnail != null) imvShortCutVideo.setImageBitmap(thumbnail);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading thumbnail", e);
            } finally {
                try { mmr.release(); } catch (IOException ignored) {}
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Video", "Video Upload", NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnDescription) {
            if (user == null) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }
            btnDescription.setEnabled(false);
            btnDescription.setText("Uploading...");
            startUploadProcess();
        }
    }

    private void startUploadProcess() {
        Id = String.valueOf(System.currentTimeMillis());

        Matcher matcher = Pattern.compile(REGEX_HASHTAG).matcher(edtDescription.getText().toString());
        hashtags.clear();
        while (matcher.find()) hashtags.add(matcher.group(0));

        db.collection("profiles").document(user.getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                username = task.getResult().getString("username");
            } else {
                username = "user_" + Id.substring(Id.length() - 4);
            }
            uploadToCloudinary();
        });
    }

    private void uploadToCloudinary() {
        MediaManager.get().upload(videoUri)
                .option("resource_type", "video")
                .option("public_id", "video_" + Id)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Cloudinary upload started");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int progress = (int) (100.0 * bytes / totalBytes);
                        if (progress >= lastProgress + 5) {
                            lastProgress = progress;
                            mBuilder.setProgress(100, progress, false);
                            notifyProgress();
                        }
                    }

                    @Override
                    @SuppressWarnings("rawtypes")
                    public void onSuccess(String requestId, Map resultData) {
                        String videoUrl = (String) resultData.get("secure_url");
                        saveToFirestore(videoUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Cloudinary error: " + error.getDescription());
                        runOnUiThread(() -> {
                            btnDescription.setEnabled(true);
                            btnDescription.setText("Post");
                            Toast.makeText(DescriptionVideoActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveToFirestore(String videoUrl) {
        String thumbUrl = videoUrl.substring(0, videoUrl.lastIndexOf(".")) + ".jpg";

        Video video = new Video(Id, videoUrl, user.getUid(), username, edtDescription.getText().toString());
        
        // Đảm bảo Map có chứa totalLikes
        Map<String, Object> videoData = video.toMap();
        
        Map<String, Object> vsData = new HashMap<>();
        vsData.put("videoId", Id);
        vsData.put("thumbnailUri", thumbUrl);
        vsData.put("watchCount", 0L);
        vsData.put("totalLikes", 0); // Quan trọng: Khởi tạo số like là 0

        db.collection("videos").document(Id).set(videoData).addOnSuccessListener(aVoid -> {
            db.collection("profiles").document(user.getUid()).collection("public_videos").document(Id).set(vsData);
            for (String tag : hashtags) {
                db.collection("hashtags").document(tag).collection("video_summaries").document(Id).set(vsData);
            }
            Toast.makeText(this, "Post successful!", Toast.LENGTH_SHORT).show();
            goHome();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Firestore error: " + e.getMessage());
            runOnUiThread(() -> btnDescription.setEnabled(true));
        });
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void notifyProgress() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            mNotifyManager.notify(0, mBuilder.build());
        }
    }
}
