package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.tiktokcloneproject.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeleteVideoSettingActivity extends AppCompatActivity {

    private ImageView imvBackToVideo;
    private FrameLayout flDeleteVideo;

    private String videoId;
    private String authorVideoId;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_video_setting);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        imvBackToVideo = (ImageView) findViewById(R.id.imvBackToVideo);
        flDeleteVideo = (FrameLayout) findViewById(R.id.flDeleteVideo);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            videoId = bundle.getString("videoId");
            authorVideoId = bundle.getString("authorId");
        }

        imvBackToVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
        });

        flDeleteVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Sử dụng androidx.appcompat.app.AlertDialog và áp dụng theme chuẩn để hiện nút bấm
                AlertDialog.Builder builder1 = new AlertDialog.Builder(DeleteVideoSettingActivity.this, R.style.AlertDialogTheme);
                builder1.setTitle("Delete Video");
                builder1.setMessage("Are you sure you want to delete this video?");
                builder1.setCancelable(true);

                builder1.setPositiveButton(
                        "Delete",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String url = "videos/" + videoId + ".mp4";
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                StorageReference desertRef = storage.getReference().child(url);

                                desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        deleteVideoIdOnHashTag(videoId);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Vẫn tiếp tục xóa metadata dù file vật lý có thể đã bị xóa trước đó
                                        deleteVideoIdOnHashTag(videoId);
                                    }
                                });
                                dialog.dismiss();
                            }
                        });

                builder1.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                AlertDialog alert11 = builder1.create();
                alert11.show();

                // Đảm bảo nút Delete có màu đỏ để cảnh báo và dễ nhìn thấy
                Button pBtn = alert11.getButton(DialogInterface.BUTTON_POSITIVE);
                if (pBtn != null) pBtn.setTextColor(Color.RED);
                Button nBtn = alert11.getButton(DialogInterface.BUTTON_NEGATIVE);
                if (nBtn != null) nBtn.setTextColor(Color.BLACK);
            }
        });
    }

    void deleteVideoIdOnVideoCollection(String videoId) {
        db.collection("videos").document(videoId).delete().addOnSuccessListener(unused -> deleteVideoIdOnPublicVideos(videoId, authorVideoId));
    }

    void deleteVideoIdOnPublicVideos(String videoId, String userId) {
        db.collection("profiles").document(userId).collection("public_videos").document(videoId).delete().addOnSuccessListener(unused -> deleteVideoIdOnVideoSummaries(videoId));
    }

    void deleteVideoIdOnVideoSummaries(String videoId) {
        db.collection("video_summaries").document(videoId).delete().addOnSuccessListener(unused -> deleteVideoIdOnLikes(videoId));
    }

    void deleteVideoIdOnLikes(String videoId) {
        db.collection("likes").document(videoId).delete().addOnSuccessListener(unused -> deleteCommentsOfVideo(videoId));
    }

    // SỬA LỖI: Xóa toàn bộ bình luận của Video thay vì xóa document có ID là videoId
    void deleteCommentsOfVideo(String videoId) {
        db.collection("comments")
            .whereEqualTo("videoId", videoId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String commentId = doc.getId();
                    // Xóa các lượt like của bình luận này
                    db.collection("comment_likes").document(commentId).delete();
                    // Xóa chính bình luận
                    doc.getReference().delete();
                }
                
                Toast.makeText(DeleteVideoSettingActivity.this, "Delete successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(DeleteVideoSettingActivity.this, HomeScreenActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(DeleteVideoSettingActivity.this, "Error deleting comments", Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    List<String> getHashTagListFromDescription(String description) {
        Pattern MY_PATTERN = Pattern.compile("#(\\S+)");
        Matcher mat = MY_PATTERN.matcher(description);
        List<String> hashTagList = new ArrayList<>();
        while (mat.find()) {
            hashTagList.add("#" + mat.group(1));
        }
        return hashTagList;
    }

    void deleteVideoIdOnHashTag(String videoId) {
        db.collection("videos").document(videoId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String description = task.getResult().getString("description");
                if (description != null) {
                    List<String> hashTagsList = getHashTagListFromDescription(description);
                    for (String tag : hashTagsList) {
                        db.collection("hashtags").document(tag).collection("video_summaries").document(videoId).delete();
                    }
                }
            }
            deleteVideoIdOnVideoCollection(videoId);
        });
    }
}
