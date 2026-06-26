package com.example.tiktokcloneproject.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends Activity implements View.OnClickListener {
    private TextView tvUsername, tvPhone, tvEmail, tvBirthdate;
    private ImageView imbPhoto, imbUsername, imbBirthdate;
    private LinearLayout llPhone, llEmail;
    private FirebaseFirestore db;
    private Uri avatarUri;
    private final int SELECT_IMAGE_CODE = 10;
    private final int CROP_IMAGE_CODE = 11;
    private ImageView imvBackToProfile;
    private static final String TAG = "EditProfileActivity";
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        initViews();
        loadUserData();
    }

    private void initViews() {
        llPhone = findViewById(R.id.llPhone);
        llEmail = findViewById(R.id.llEmail);
        tvUsername = findViewById(R.id.tvUsername);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvBirthdate = findViewById(R.id.tvBirthdate);
        imbPhoto = findViewById(R.id.imbPhoto);
        imvBackToProfile = findViewById(R.id.imvBackToProfile);
        imbUsername = findViewById(R.id.imbUsername);
        imbBirthdate = findViewById(R.id.imbBirthdate);

        imbPhoto.setOnClickListener(this);
        imvBackToProfile.setOnClickListener(this);
        imbUsername.setOnClickListener(this);
        imbBirthdate.setOnClickListener(this);
    }

    private void loadUserData() {
        if (user == null) return;
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                tvUsername.setText(document.getString("username"));
                tvPhone.setText(document.getString("phone"));
                tvEmail.setText(document.getString("email"));
                tvBirthdate.setText(document.getString("birthdate"));
                
                String phone = document.getString("phone");
                if (phone == null || phone.isEmpty()) {
                    llPhone.setVisibility(View.GONE);
                    llEmail.setVisibility(View.VISIBLE);
                } else {
                    llPhone.setVisibility(View.VISIBLE);
                    llEmail.setVisibility(View.GONE);
                }
                
                String avatarUrl = document.getString("avatarUrl");
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(this).load(avatarUrl)
                         .diskCacheStrategy(DiskCacheStrategy.ALL)
                         .placeholder(R.drawable.default_avatar)
                         .into(imbPhoto);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == SELECT_IMAGE_CODE) {
                Uri sourceUri = data.getData();
                if (sourceUri != null) {
                    startCrop(sourceUri);
                }
            } else if (requestCode == CROP_IMAGE_CODE) {
                avatarUri = UCrop.getOutput(data);
                if (avatarUri != null) {
                    Glide.with(this).load(avatarUri).circleCrop().into(imbPhoto);
                    uploadAvatarToCloudinary();
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Toast.makeText(this, cropError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCrop(Uri uri) {
        String destinationFileName = "cropped_avatar_" + System.currentTimeMillis() + ".jpg";
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        
        // TikTok style UI for uCrop (Light Mode)
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setShowCropGrid(false);
        options.setToolbarColor(Color.WHITE);
        options.setToolbarWidgetColor(Color.BLACK);
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.color_primary_tiktok));
        options.setToolbarTitle("Cắt");
        options.setStatusBarLight(true); // Ensure status bar icons are dark

        // Use our custom CropActivity
        Intent intent = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)))
                .withAspectRatio(1, 1)
                .withMaxResultSize(1080, 1080)
                .withOptions(options)
                .getIntent(this);
        
        intent.setClass(this, CropActivity.class);
        startActivityForResult(intent, CROP_IMAGE_CODE);
    }

    private void uploadAvatarToCloudinary() {
        if (user == null || avatarUri == null) return;

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Updating Avatar...");
        progress.setCancelable(false);
        progress.show();

        MediaManager.get().upload(avatarUri)
                .option("folder", "user_avatars")
                .option("public_id", user.getUid())
                .option("overwrite", true)
                .option("invalidate", true)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String downloadUrl = (String) resultData.get("secure_url");
                        Map<String, Object> update = new HashMap<>();
                        update.put("avatarUrl", downloadUrl);

                        db.collection("users").document(user.getUid()).set(update, SetOptions.merge());
                        db.collection("profiles").document(user.getUid()).set(update, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    progress.dismiss();
                                    Toast.makeText(EditProfileActivity.this, "Avatar updated successfully!", Toast.LENGTH_SHORT).show();
                                });
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progress.dismiss();
                        Toast.makeText(EditProfileActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.imbPhoto) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGE_CODE);
        } else if (id == R.id.imvBackToProfile) {
            finish();
        } else if (id == R.id.imbUsername) {
            moveToEdit(StaticVariable.USERNAME, tvUsername.getText().toString());
        } else if (id == R.id.imbBirthdate) {
            moveToEdit(StaticVariable.BIRTHDATE, tvBirthdate.getText().toString());
        }
    }

    private void moveToEdit(String mode, String content) {
        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra("mode", mode);
        intent.putExtra("content", content);
        startActivity(intent);
    }
}
