package com.example.tiktokcloneproject.activity;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.tiktokcloneproject.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsAndPrivacyActivity extends AppCompatActivity {

    private ImageView imvBackToProfile;
    private FrameLayout flAccountOption, flShareProfileOption;
    private SwitchMaterial switchDarkMode;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_and_privacy);
        
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        imvBackToProfile = findViewById(R.id.imvBackToProfile);
        flAccountOption = findViewById(R.id.flAccountOption);
        flShareProfileOption = findViewById(R.id.flShareProfileOption);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);

        // Set initial state of switch
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        switchDarkMode.setChecked(isDarkMode);

        imvBackToProfile.setOnClickListener(view -> finish());

        flAccountOption.setOnClickListener(view -> {
            // Intent intent = new Intent(SettingsAndPrivacyActivity.this, AccountSettingActivity.class);
            // startActivity(intent);
        });

        flShareProfileOption.setOnClickListener(view -> {
            if (user != null) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("toptop-link", "http://toptoptoptop.com/" + user.getUid());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Profile link copied", Toast.LENGTH_SHORT).show();
            }
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isDarkMode", isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }
}
