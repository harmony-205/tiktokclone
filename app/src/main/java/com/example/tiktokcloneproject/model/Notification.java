package com.example.tiktokcloneproject.model;

import com.google.firebase.database.FirebaseDatabase;

public class Notification {

    private String fromUsername;
    private String action;
    private long timestamp;

    public Notification() {
    }

    public Notification(String fromUser, String action) {
        this.fromUsername = fromUser;
        this.action = action;
        this.timestamp = System.currentTimeMillis();
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public String getAction() {
        return action;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static void pushNotification(String fromUsername, String toUserId, String action) {
        if (toUserId == null || fromUsername == null) return;

        FirebaseDatabase.getInstance()
                        .getReference()
                        .child(toUserId)
                        .push()
                        .setValue(new Notification(fromUsername, action)
                        );
    }
}
