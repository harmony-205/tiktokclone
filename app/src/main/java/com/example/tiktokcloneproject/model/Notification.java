package com.example.tiktokcloneproject.model;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class Notification {

    private String fromUsername;
    private String toUserId;
    private String action;
    private long timestamp;
    private boolean read;

    public Notification() {
    }

    public Notification(String fromUsername, String toUserId, String action) {
        this.fromUsername = fromUsername;
        this.toUserId = toUserId;
        this.action = action;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    // Getters
    public String getFromUsername() { return fromUsername; }
    public String getToUserId() { return toUserId; }
    public String getAction() { return action; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }

    // Setters (BẮT BUỘC ĐỂ FIRESTORE HOẠT ĐỘNG)
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
    public void setAction(String action) { this.action = action; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { this.read = read; }

    public static void pushNotification(String fromUsername, String toUserId, String action) {
        if (toUserId == null || fromUsername == null) return;
        
        // Không gửi thông báo nếu tự tương tác với chính mình (ví dụ dùng Firebase Auth UID để kiểm tra)
        // Tuy nhiên ở đây chúng ta nhận toUserId và fromUsername từ ngoài vào nên hãy đảm bảo logic ở nơi gọi.

        Notification notification = new Notification(fromUsername, toUserId, action);
        FirebaseFirestore.getInstance().collection("notifications").add(notification);
    }
}
