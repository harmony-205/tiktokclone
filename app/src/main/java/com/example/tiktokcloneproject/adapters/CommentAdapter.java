package com.example.tiktokcloneproject.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.ProfileActivity;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Comment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class CommentAdapter extends ArrayAdapter<Comment> {
    private Context context;

    public CommentAdapter(@NonNull Context context, int resource, ArrayList<Comment> comments) {
        super(context, resource, comments);
        this.context = context;
    }

    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(R.layout.layout_row_comment, parent, false);
        }

        ImageView imvAvatarInComment = row.findViewById(R.id.imvAvatarInComment);
        TextView txvUsernameInComment = row.findViewById(R.id.txvUsernameInComment);
        TextView txvComment = row.findViewById(R.id.txvComment);
        TextView txvTotalLikeComment = row.findViewById(R.id.txvTotalLikeComment);

        Comment comment = getItem(position);
        if (comment != null) {
            txvComment.setText(comment.getContent());
            txvTotalLikeComment.setText(String.valueOf(comment.getTotalLikes()));
            
            // Default values while loading
            txvUsernameInComment.setText("@loading...");
            imvAvatarInComment.setImageResource(R.drawable.group19onblack);

            String authorId = comment.getAuthorId();
            if (authorId != null && !authorId.isEmpty()) {
                DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document(authorId);
                docRef.get().addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String username = document.getString("username");
                        txvUsernameInComment.setText(username != null ? "@" + username : "@unknown");
                    }
                });
                loadAvatar(authorId, imvAvatarInComment);
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

    private void loadAvatar(String authorId, ImageView imv) {
        StorageReference download = FirebaseStorage.getInstance().getReference().child("/user_avatars").child(authorId);
        download.getBytes(StaticVariable.MAX_BYTES_AVATAR)
                .addOnSuccessListener(bytes -> {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    imv.setImageBitmap(bitmap);
                })
                .addOnFailureListener(e -> imv.setImageResource(R.drawable.group19onblack));
    }
}
