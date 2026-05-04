package com.example.tiktokcloneproject.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.CommentAdapter;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Comment;
import com.example.tiktokcloneproject.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class CommentActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "CommentActivity";
    private ImageView imvBack, imvMyAvatarInComment;
    private LinearLayout llComment;
    private EditText edtComment;
    private ImageButton imbSendComment;
    private String videoId, userId;
    private ListView lvComment;
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;
    String username;
    String authorVideoId;
    CommentAdapter adapter;
    ArrayList<Comment> comments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        llComment = findViewById(R.id.llComment);
        imvBack = llComment.findViewById(R.id.imvBackToHomeScreen);
        imvMyAvatarInComment = llComment.findViewById(R.id.imvMyAvatarInComment);
        edtComment = llComment.findViewById(R.id.edtComment);
        imbSendComment = llComment.findViewById(R.id.imbSendComment);
        lvComment = llComment.findViewById(R.id.listViewComment);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            videoId = bundle.getString("videoId");
            authorVideoId = bundle.getString("authorId");
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        
        comments = new ArrayList<>();
        adapter = new CommentAdapter(this, R.layout.layout_row_comment, comments);
        lvComment.setAdapter(adapter);

        imvBack.setOnClickListener(this);
        imbSendComment.setOnClickListener(this);

        if (user != null) {
            userId = user.getUid();
            db.collection("users").document(userId)
                    .get().addOnSuccessListener(document -> {
                        if (document.exists()) {
                            username = document.getString("username");
                            String avatarUrl = document.getString("avatarUrl");
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(this)
                                     .load(avatarUrl)
                                     .placeholder(R.drawable.default_avatar)
                                     .circleCrop()
                                     .into(imvMyAvatarInComment);
                            } else {
                                imvMyAvatarInComment.setImageResource(R.drawable.default_avatar);
                            }
                        }
                    });
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Lắng nghe bình luận real-time và TỰ ĐỘNG CẬP NHẬT SỐ LƯỢNG
        db.collection("comments")
                .whereEqualTo("videoId", videoId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            Comment comment = dc.getDocument().toObject(Comment.class);
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                boolean exists = false;
                                for (Comment c : comments) {
                                    if (c.getCommentId().equals(comment.getCommentId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    comments.add(0, comment);
                                }
                            } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                                for (int i = 0; i < comments.size(); i++) {
                                    if (comments.get(i).getCommentId().equals(comment.getCommentId())) {
                                        comments.remove(i);
                                        break;
                                    }
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        
                        // CẬP NHẬT SỐ LƯỢNG DỰA TRÊN SỐ LƯỢNG THỰC TẾ TRONG SNAPSHOT
                        int actualCount = snapshots.size();
                        updateVideoCommentCount(actualCount);
                    }
                });
    }

    private void updateVideoCommentCount(int count) {
        if (videoId != null) {
            db.collection("videos").document(videoId)
                    .update("totalComments", count)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã đồng bộ số bình luận: " + count));
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == imvBack.getId()) {
            onBackPressed();
        } else if (v.getId() == imbSendComment.getId()) {
            String cmtContent = edtComment.getText().toString().trim();
            if (TextUtils.isEmpty(cmtContent)) return;
            
            String timeStamp = String.valueOf(System.currentTimeMillis());
            Comment comment = new Comment(timeStamp, videoId, user.getUid(), cmtContent);
            
            db.collection("comments").document(comment.getCommentId()).set(comment)
                .addOnSuccessListener(aVoid -> {
                    if (username != null) {
                        Notification.pushNotification(username, authorVideoId, StaticVariable.COMMENT);
                    }
                    edtComment.setText("");
                });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.slide_left_to_right, R.anim.slide_out_bottom);
    }
}
