package com.example.tiktokcloneproject.helper;

import android.app.Application;
import android.net.Uri;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class GlobalVariable extends Application {
    private Uri avatarUri ;

    @Override
    public void onCreate() {
        super.onCreate();
        initCloudinary();
    }

    private void initCloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dcdea4ayl");
        config.put("api_key", "688684651561669");
        config.put("api_secret", "BWbsFrFWqpBH0jW755vmFATxBMg");
        try {
            MediaManager.init(this, config);
        } catch (IllegalStateException ignored) {
            // Đã khởi tạo trước đó
        }
    }

    public void setAvatarUri(Uri avatarUri) {
        this.avatarUri = avatarUri;
    }

    public Uri getAvatarUri() {
        return avatarUri;
    }
}
