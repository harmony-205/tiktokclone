package com.example.tiktokcloneproject.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.NotificationAdapter;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class NotificationActivity extends AppCompatActivity {
    private static final String TAG = "NotificationActivity";
    private NotificationAdapter adapter;
    private ArrayList<Notification> notificationList;
    private DatabaseReference mDatabase;
    private String mode; // "follower" or "activity"
    private TextView tvEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        ListView lvNotifications = findViewById(R.id.lvNotifications);
        ImageView imvBack = findViewById(R.id.imvBack);
        tvEmpty = findViewById(R.id.tvEmpty); // Gán trực tiếp vào field
        TextView tvTitle = findViewById(R.id.tvTitle);

        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "activity";

        if ("follower".equals(mode)) {
            if (tvTitle != null) tvTitle.setText("Follower mới");
        } else {
            if (tvTitle != null) tvTitle.setText("Hoạt động");
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentUserId = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference().child(currentUserId);

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(this, R.layout.notification_row, notificationList);
        if (lvNotifications != null) {
            lvNotifications.setAdapter(adapter);
        }

        if (imvBack != null) {
            imvBack.setOnClickListener(v -> finish());
        }

        loadNotifications();
    }

    private void loadNotifications() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                notificationList.clear();
                Log.d(TAG, "Data changed, total nodes: " + snapshot.getChildrenCount());
                
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Notification notification = dataSnapshot.getValue(Notification.class);
                    if (notification != null) {
                        Log.d(TAG, "Notification: " + notification.getAction() + " from " + notification.getFromUsername());
                        if ("follower".equals(mode)) {
                            if (StaticVariable.FOLLOW.equals(notification.getAction())) {
                                notificationList.add(notification);
                            }
                        } else {
                            if (StaticVariable.LIKE.equals(notification.getAction()) || 
                                StaticVariable.COMMENT.equals(notification.getAction())) {
                                notificationList.add(notification);
                            }
                        }
                    }
                }
                
                Collections.reverse(notificationList);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }
}
