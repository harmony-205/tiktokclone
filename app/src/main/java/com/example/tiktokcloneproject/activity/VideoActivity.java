package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.VideoAdapter;
import com.example.tiktokcloneproject.model.Video;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class VideoActivity extends Activity {
    private String videoId;
    private FirebaseFirestore db;
    private ViewPager2 viewPager2;
    private ArrayList<Video> videos;
    private VideoAdapter videoAdapter;
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (intent.hasExtra("videoId")) {
            videoId = bundle.getString("videoId");
        } else {
            Uri data = intent.getData();
            if (data != null) {
                List<String> segmentsList = data.getPathSegments();
                videoId = segmentsList.get(segmentsList.size() - 1);
            }
        }

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        viewPager2 = findViewById(R.id.viewPager);
        videos = new ArrayList<>();
        videoAdapter = new VideoAdapter(this, videos);
        VideoAdapter.setUser(user);
        viewPager2.setAdapter(videoAdapter);
        
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                videoAdapter.playVideo(position);
                videoAdapter.updateCurrentPosition(position);
            }
        });

        db = FirebaseFirestore.getInstance();
        if (videoId != null) {
            db.collection("videos").document(videoId)
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            Video video = task.getResult().toObject(Video.class);
                            if (video != null) {
                                videos.add(video);
                                videoAdapter.notifyItemInserted(0);
                            }
                        }
                    });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoAdapter != null) {
            videoAdapter.releaseAll(); // Giải phóng RAM ngay khi tạm dừng
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (videoAdapter != null) {
            videoAdapter.releaseAll();
        }
    }

    @Override
    protected void onDestroy() {
        if (videoAdapter != null) {
            videoAdapter.releaseAll();
        }
        super.onDestroy();
    }

    public void onClick(View v) {
        if (v.getId() == R.id.btnBackVideo) {
            onBackPressed(); // Sử dụng hàm chuẩn của Android
        }
    }

    @Override
    public void onBackPressed() {
        if (videoAdapter != null) {
            videoAdapter.releaseAll();
        }
        super.onBackPressed();
        finish();
    }
}
