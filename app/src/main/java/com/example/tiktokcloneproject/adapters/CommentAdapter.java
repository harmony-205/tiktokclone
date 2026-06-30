package com.example.tiktokcloneproject.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.ProfileActivity;
import com.example.tiktokcloneproject.model.Comment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CommentAdapter extends ArrayAdapter<Comment> {
    private Context context;
    private FirebaseFirestore db;
    private String currentUserId;

    public CommentAdapter(@NonNull Context context, int resource, ArrayList<Comment> comments) {
        super(context, resource, comments);
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        final View row;
        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(R.layout.layout_row_comment, parent, false);
        } else {
            row = convertView;
        }

        ImageView imvAvatarInComment = row.findViewById(R.id.imvAvatarInComment);
        TextView txvUsernameInComment = row.findViewById(R.id.txvUsernameInComment);
        TextView txvComment = row.findViewById(R.id.txvComment);
        TextView txvTotalLikeComment = row.findViewById(R.id.txvTotalLikeComment);
        ImageView imvLikeInComment = row.findViewById(R.id.imvLikeInComment);

        Comment comment = getItem(position);
        if (comment != null) {
            txvComment.setText(comment.getContent());
            txvTotalLikeComment.setText(String.valueOf(comment.getTotalLikes()));
            
            // Xử lý hiển thị trạng thái Like
            checkIfLiked(comment.getCommentId(), imvLikeInComment);

            // Xử lý sự kiện Like
            imvLikeInComment.setOnClickListener(v -> {
                if (currentUserId == null) {
                    Toast.makeText(context, "Please login to like", Toast.LENGTH_SHORT).show();
                    return;
                }
                toggleLike(comment, imvLikeInComment, txvTotalLikeComment);
            });

            // Các xử lý hiện có (Username, Avatar...)
            txvUsernameInComment.setText("@...");
            imvAvatarInComment.setImageResource(R.drawable.default_avatar);

            final String authorId = comment.getAuthorId();
            row.setTag(authorId); 

            if (authorId != null && !authorId.isEmpty()) {
                db.collection("users").document(authorId).get().addOnSuccessListener(document -> {
                    if (document.exists() && authorId.equals(row.getTag())) {
                        String username = document.getString("username");
                        txvUsernameInComment.setText(username != null ? "@" + username : "@unknown");
                        String avatarUrl = document.getString("avatarUrl");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(context).load(avatarUrl).placeholder(R.drawable.default_avatar).circleCrop().into(imvAvatarInComment);
                        }
                    }
                });
            }

            row.setOnClickListener(v -> {
                if (authorId != null) {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("id", authorId);
                    context.startActivity(intent);
                }
            });
        }
        return row;
    }

    private void checkIfLiked(String commentId, ImageView imvLike) {
        if (currentUserId == null) return;
        db.collection("comment_likes").document(commentId)
          .collection("users").document(currentUserId).get()
          .addOnSuccessListener(document -> {
              if (document.exists()) {
                  imvLike.setImageResource(R.drawable.ic_fill_favorite);
                  imvLike.setTag("liked");
              } else {
                  imvLike.setImageResource(R.drawable.ic_like);
                  imvLike.setTag("unliked");
              }
          });
    }

    private void toggleLike(Comment comment, ImageView imvLike, TextView txvCount) {
        DocumentReference likeRef = db.collection("comment_likes").document(comment.getCommentId())
                                      .collection("users").document(currentUserId);
        DocumentReference commentRef = db.collection("comments").document(comment.getCommentId());

        if ("liked".equals(imvLike.getTag())) {
            // Unlike
            likeRef.delete().addOnSuccessListener(aVoid -> {
                commentRef.update("totalLikes", FieldValue.increment(-1));
                comment.setTotalLikes(comment.getTotalLikes() - 1);
                txvCount.setText(String.valueOf(comment.getTotalLikes()));
                imvLike.setImageResource(R.drawable.ic_like);
                imvLike.setTag("unliked");
            });
        } else {
            // Like
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("timestamp", FieldValue.serverTimestamp());
            likeRef.set(likeData).addOnSuccessListener(aVoid -> {
                commentRef.update("totalLikes", FieldValue.increment(1));
                comment.setTotalLikes(comment.getTotalLikes() + 1);
                txvCount.setText(String.valueOf(comment.getTotalLikes()));
                imvLike.setImageResource(R.drawable.ic_fill_favorite);
                imvLike.setTag("liked");
            });
        }
    }
}
