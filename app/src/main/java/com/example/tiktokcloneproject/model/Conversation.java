package com.example.tiktokcloneproject.model;

public class Conversation {
    private String chatId;
    private String lastMessage;
    private long timestamp;
    private String otherUserId;
    private String otherUsername;
    private String otherAvatarUrl;

    public Conversation() {}

    public Conversation(String chatId, String lastMessage, long timestamp, String otherUserId, String otherUsername, String otherAvatarUrl) {
        this.chatId = chatId;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.otherUserId = otherUserId;
        this.otherUsername = otherUsername;
        this.otherAvatarUrl = otherAvatarUrl;
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getOtherUserId() { return otherUserId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }

    public String getOtherUsername() { return otherUsername; }
    public void setOtherUsername(String otherUsername) { this.otherUsername = otherUsername; }

    public String getOtherAvatarUrl() { return otherAvatarUrl; }
    public void setOtherAvatarUrl(String otherAvatarUrl) { this.otherAvatarUrl = otherAvatarUrl; }
}
