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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class CommentAdapter extends ArrayAdapter<Comment> {
    private Context context;

    public CommentAdapter(@NonNull Context context, int resource, ArrayList<Comment> comments) {
        super(context, resource, comments);
        this.context = context;
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

        Comment comment = getItem(position);
        if (comment != null) {
            txvComment.setText(comment.getContent());
            txvTotalLikeComment.setText(String.valueOf(comment.getTotalLikes()));
            
            // Clear recycled data immediately
            txvUsernameInComment.setText("@...");
            imvAvatarInComment.setImageResource(R.drawable.default_avatar);

            final String authorId = comment.getAuthorId();
            row.setTag(authorId); 

            if (authorId != null && !authorId.isEmpty()) {
                DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document(authorId);
                docRef.get().addOnSuccessListener(document -> {
                    // Check if the view is still intended for this author
                    if (document.exists() && authorId.equals(row.getTag())) {
                        String username = document.getString("username");
                        txvUsernameInComment.setText(username != null ? "@" + username : "@unknown");

                        String avatarUrl = document.getString("avatarUrl");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(context)
                                 .load(avatarUrl)
                                 .placeholder(R.drawable.default_avatar)
                                 .circleCrop()
                                 .into(imvAvatarInComment);
                        }
                    }
                });
            } else {
                txvUsernameInComment.setText("@unknown");
            }

            row.setOnClickListener(v -> {
                if (authorId != null) {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("id", authorId);
                    context.startActivity(intent);
                }
            });

            row.setOnLongClickListener(v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("comment", comment.getContent());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        return row;
    }
}
