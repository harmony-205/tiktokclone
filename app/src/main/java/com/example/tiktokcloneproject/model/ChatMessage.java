package com.example.tiktokcloneproject.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String message;
    @ServerTimestamp
    private Date timestamp;

    public ChatMessage() {}

    public ChatMessage(String senderId, String receiverId, String message) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
