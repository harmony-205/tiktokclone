package com.example.tiktokcloneproject.model;

import java.util.HashMap;
import java.util.Map;

public class Video {
    private String videoId, videoUri, authorId, description, username;
    private int totalLikes, totalComments;
    private long watchCount;

    public Video() {
    }

    public Video(String videoId, String videoUri, String authorId, String username, String description) {
        this.videoId = videoId;
        this.videoUri = videoUri;
        this.authorId = authorId;
        this.username = username;
        this.description = description;
        this.totalLikes = 0;
        this.totalComments = 0;
        this.watchCount = 0;
    }

    public Video(String videoId, String videoUri, String authorId, String description, String username, int totalLikes, int totalComments, long watchCount) {
        this.videoId = videoId;
        this.videoUri = videoUri;
        this.authorId = authorId;
        this.description = description;
        this.username = username;
        this.totalLikes = totalLikes;
        this.totalComments = totalComments;
        this.watchCount = watchCount;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getVideoUri() {
        return videoUri;
    }

    public void setVideoUri(String videoUri) {
        this.videoUri = videoUri;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(int totalLikes) {
        this.totalLikes = totalLikes;
    }

    public int getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(int totalComments) {
        this.totalComments = totalComments;
    }

    public long getWatchCount() {
        return watchCount;
    }

    public void setWatchCount(long watchCount) {
        this.watchCount = watchCount;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("videoId", videoId);
        result.put("videoUri", videoUri);
        result.put("authorId", authorId);
        result.put("username", username);
        result.put("description", description);
        result.put("totalComments", totalComments);
        result.put("totalLikes", totalLikes);
        result.put("watchCount", watchCount);
        return result;
    }
}
