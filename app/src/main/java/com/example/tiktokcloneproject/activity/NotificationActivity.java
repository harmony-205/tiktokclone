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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;

public class NotificationActivity extends AppCompatActivity {
    private static final String TAG = "NotificationActivity";
    private NotificationAdapter adapter;
    private ArrayList<Notification> notificationList;
    private FirebaseFirestore db;
    private String mode; 
    private TextView tvEmpty;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        ListView lvNotifications = findViewById(R.id.lvNotifications);
        ImageView imvBack = findViewById(R.id.imvBack);
        tvEmpty = findViewById(R.id.tvEmpty);
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

        db = FirebaseFirestore.getInstance();
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(this, R.layout.notification_row, notificationList);
        if (lvNotifications != null) {
            lvNotifications.setAdapter(adapter);
        }

        if (imvBack != null) {
            imvBack.setOnClickListener(v -> finish());
        }

        loadNotifications(user.getUid());
    }

    private void loadNotifications(String userId) {
        // Lấy tất cả thông báo của User này và lọc bằng code để tránh lỗi Index Firestore
        listenerRegistration = db.collection("notifications")
                .whereEqualTo("toUserId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Firestore error: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        notificationList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Notification notification = doc.toObject(Notification.class);
                            
                            // Lọc thông báo dựa trên Mode
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
                        
                        // Sắp xếp thông báo mới nhất lên trên
                        Collections.sort(notificationList, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                        
                        adapter.notifyDataSetChanged();
                        
                        if (tvEmpty != null) {
                            tvEmpty.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                        Log.d(TAG, "Notifications loaded: " + notificationList.size());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
