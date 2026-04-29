package com.example.tiktokcloneproject.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.tiktokcloneproject.R;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropFragment;
import com.yalantis.ucrop.UCropFragmentCallback;
import com.yalantis.ucrop.model.AspectRatio;
import com.yalantis.ucrop.view.UCropView;

public class CropActivity extends AppCompatActivity implements UCropFragmentCallback {

    private UCropFragment fragment;
    private CheckBox cbPostToStory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        cbPostToStory = findViewById(R.id.cbPostToStory);
        ImageView btnBack = findViewById(R.id.btnBack);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnSaveAndPost = findViewById(R.id.btnSaveAndPost);

        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        
        btnSaveAndPost.setOnClickListener(v -> {
            if (fragment != null) {
                fragment.cropAndSaveImage();
            }
        });

        setupUCrop();
    }

    private void setupUCrop() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Toast.makeText(this, "No data found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fragment = UCropFragment.newInstance(extras);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.cropContainer, fragment, UCropFragment.TAG)
                .commitAllowingStateLoss();
    }

    @Override
    public void loadingProgress(boolean showLoader) {
        // Handle progress if needed
    }

    @Override
    public void onCropFinish(UCropFragment.UCropResult result) {
        switch (result.mResultCode) {
            case RESULT_OK:
                Intent resultIntent = result.mResultData;
                // You can add extra data here, e.g., checkbox state
                resultIntent.putExtra("post_to_story", cbPostToStory.isChecked());
                setResult(RESULT_OK, resultIntent);
                finish();
                break;
            case UCrop.RESULT_ERROR:
                handleCropError(result.mResultData);
                break;
            default:
                setResult(RESULT_CANCELED);
                finish();
                break;
        }
    }

    private void handleCropError(Intent data) {
        final Throwable cropError = UCrop.getError(data);
        if (cropError != null) {
            Toast.makeText(this, cropError.getMessage(), Toast.LENGTH_SHORT).show();
        }
        setResult(UCrop.RESULT_ERROR, data);
        finish();
    }
}
