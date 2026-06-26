package com.example.tiktokcloneproject.helper;

import android.app.Application;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.appcompat.app.AppCompatDelegate;

public class GlobalVariable extends Application {
    private Uri avatarUri ;

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply theme on application start
        SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public void setAvatarUri(Uri avatarUri) {
        this.avatarUri = avatarUri;
    }

    public Uri getAvatarUri() {
        return avatarUri;
    }
}
