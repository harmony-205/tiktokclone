package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.VideoSummaryAdapter;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Notification;
import com.example.tiktokcloneproject.model.VideoSummary;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends FragmentActivity implements View.OnClickListener {
    final String USERNAME_LABEL = "username";
    private TextView txvFollowing, txvFollowers, txvLikes, txvUserName;
    private EditText edtBio;
    private Button btnEditProfile, btnUpdateBio, btnCancelUpdateBio;
    private LinearLayout llFollowing, llFollowers;
    private ImageView imvAvatarProfile;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private String userId;
    private DocumentReference profileDocRef, userDocRef;
    private ListenerRegistration userListener, profileListener;
    private String oldBioText, currentUserID;
    private static final String TAG = "ProfileActivity";
    private RecyclerView recVideoSummary;
    private ArrayList<VideoSummary> videoSummaries;
    private int totalLikes = 0;
    private String currentAvatarUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("id")) {
            userId = intent.getStringExtra("id");
        } else if (intent != null && intent.getData() != null) {
            List<String> segmentsList = intent.getData().getPathSegments();
            userId = segmentsList.get(segmentsList.size() - 1);
        } else {
            userId = user != null ? user.getUid() : null;
        }

        if (userId == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        profileDocRef = db.collection("profiles").document(userId);
        userDocRef = db.collection("users").document(userId);
        
        initViews();
        setupProfileUI();

        videoSummaries = new ArrayList<>();
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        recVideoSummary.setLayoutManager(gridLayoutManager);
        recVideoSummary.addItemDecoration(new GridSpacingItemDecoration(3, 10, true));
        setVideoSummaries();
        setLikes(userId);
    }

    private void initViews() {
        txvFollowing = findViewById(R.id.text_following);
        txvFollowers = findViewById(R.id.text_followers);
        txvLikes = findViewById(R.id.text_likes);
        txvUserName = findViewById(R.id.txv_username);
        edtBio = findViewById(R.id.edt_bio);
        btnEditProfile = findViewById(R.id.button_edit_profile);
        imvAvatarProfile = findViewById(R.id.imvAvatarProfile);
        llFollowers = findViewById(R.id.ll_followers);
        llFollowing = findViewById(R.id.ll_following);
        recVideoSummary = findViewById(R.id.recycle_view_video_summary);
        btnUpdateBio = findViewById(R.id.btn_update_bio);
        btnCancelUpdateBio = findViewById(R.id.btn_cancel_update_bio);

        btnUpdateBio.setOnClickListener(this);
        btnCancelUpdateBio.setOnClickListener(this);
        llFollowers.setOnClickListener(this);
        llFollowing.setOnClickListener(this);
        imvAvatarProfile.setOnClickListener(this);
    }

    private void setupProfileUI() {
        if (user != null) {
            currentUserID = user.getUid();
            if (userId.equals(currentUserID)) {
                btnEditProfile.setVisibility(View.VISIBLE);
                edtBio.setVisibility(View.VISIBLE);
                oldBioText = edtBio.getText().toString();
                
                edtBio.setOnFocusChangeListener((view, hasFocus) -> {
                    findViewById(R.id.layout_bio).setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                });
                btnEditProfile.setOnClickListener(this);
            } else {
                handleFollow();
            }
        } else {
            handleFollow();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        startListeningToData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userListener != null) userListener.remove();
        if (profileListener != null) profileListener.remove();
    }

    private void startListeningToData() {
        profileListener = profileDocRef.addSnapshotListener((document, e) -> {
            if (e != null) return;
            if (document != null && document.exists()) {
                txvFollowing.setText(String.valueOf(document.getLong("following") != null ? document.getLong("following") : 0));
                txvFollowers.setText(String.valueOf(document.getLong("followers") != null ? document.getLong("followers") : 0));
                txvLikes.setText(String.valueOf(document.getLong("likes") != null ? document.getLong("likes") : 0));
            }
        });

        userListener = userDocRef.addSnapshotListener((document, e) -> {
            if (e != null) return;
            if (document != null && document.exists()) {
                txvUserName.setText("@" + document.getString("username"));
                String avatarUrl = document.getString("avatarUrl");
                
                if (avatarUrl != null) {
                    currentAvatarUrl = avatarUrl;
                    updateAvatarImage();
                }
            }
        });
    }

    private void updateAvatarImage() {
        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
            Glide.with(this)
                 .load(currentAvatarUrl)
                 .placeholder(R.drawable.default_avatar)
                 .error(R.drawable.default_avatar)
                 .circleCrop()
                 .diskCacheStrategy(DiskCacheStrategy.NONE)
                 .skipMemoryCache(true)
                 .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                 .listener(new RequestListener<Drawable>() {
                     @Override
                     public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                         Log.e(TAG, "Glide load failed", e);
                         return false;
                     }
                     @Override
                     public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                         return false;
                     }
                 })
                 .into(imvAvatarProfile);
        } else {
            imvAvatarProfile.setImageResource(R.drawable.default_avatar);
        }
    }

    private void handleFollow() {
        Button btnFollow = findViewById(R.id.button_follow);
        btnFollow.setVisibility(View.VISIBLE);

        if (user != null) {
            db.collection("profiles").document(currentUserID)
                    .collection("following").document(userId)
                    .addSnapshotListener((document, e) -> {
                        if (document != null) {
                            isFollowed = document.exists();
                            if (isFollowed) {
                                handleFollowedUI(btnFollow);
                            } else {
                                handleUnfollowedUI(btnFollow);
                            }
                        }
                    });
        } else {
            btnFollow.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, MainActivity.class)));
        }
    }

    boolean isFollowed = false;

    private void handleFollowedUI(Button btnFollow) {
        btnFollow.setText("Unfollow");
        btnFollow.setOnClickListener(v -> {
            db.collection("profiles").document(currentUserID).collection("following").document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("profiles").document(currentUserID).update("following", FieldValue.increment(-1));
                    db.collection("profiles").document(userId).update("followers", FieldValue.increment(-1));
                    handleUnfollowedUI(btnFollow);
                });
            db.collection("profiles").document(userId).collection("followers").document(currentUserID).delete();
        });
    }

    private void handleUnfollowedUI(Button btnFollow) {
        btnFollow.setText("Follow");
        btnFollow.setOnClickListener(v -> {
            Map<String, Object> data = new HashMap<>();
            data.put("userID", userId);
            db.collection("profiles").document(currentUserID).collection("following").document(userId).set(data)
                .addOnSuccessListener(aVoid -> {
                    db.collection("profiles").document(currentUserID).update("following", FieldValue.increment(1));
                    db.collection("profiles").document(userId).update("followers", FieldValue.increment(1));
                    notifyFollow();
                    handleFollowedUI(btnFollow);
                });
            Map<String, Object> data1 = new HashMap<>();
            data1.put("userID", currentUserID);
            db.collection("profiles").document(userId).collection("followers").document(currentUserID).set(data1);
        });
    }

    public void notifyFollow() {
        if (user == null) return;
        userDocRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String username = document.getString("username");
                Notification.pushNotification(username, userId, StaticVariable.FOLLOW);
            }
        });
    }

    protected void setVideoSummaries() {
        db.collection("profiles").document(userId).collection("public_videos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        videoSummaries.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            videoSummaries.add(new VideoSummary(document.getString("videoId"),
                                    document.getString("thumbnailUri"),
                                    document.getLong("watchCount")));
                        }
                        if (!videoSummaries.isEmpty()) {
                            VideoSummaryAdapter videoSummaryAdapter = new VideoSummaryAdapter(getApplicationContext(), videoSummaries);
                            recVideoSummary.setAdapter(videoSummaryAdapter);
                        }
                    }
                });
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount, spacing;
        private boolean includeEdge;
        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount; this.spacing = spacing; this.includeEdge = includeEdge;
        }
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;
            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
                if (position < spanCount) outRect.top = spacing;
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) outRect.top = spacing;
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.text_menu) {
            showDialog();
        } else if (id == R.id.imvAvatarProfile) {
            showShareAccountDialog();
        } else if (id == R.id.button_edit_profile) {
            moveToAnotherActivity(EditProfileActivity.class);
        } else if (id == R.id.btnBackProfile) {
            finish();
        } else if (id == R.id.btn_update_bio) {
            profileDocRef.update("bio", edtBio.getText().toString());
            oldBioText = edtBio.getText().toString();
            findViewById(R.id.layout_bio).setVisibility(View.GONE);
            hideKeyboard();
        } else if (id == R.id.btn_cancel_update_bio) {
            edtBio.setText(oldBioText);
            findViewById(R.id.layout_bio).setVisibility(View.GONE);
            hideKeyboard();
        } else if (id == R.id.ll_followers || id == R.id.ll_following) {
            if (userId.equals(currentUserID)) {
                Intent intent = new Intent(ProfileActivity.this, FollowListActivity.class);
                intent.putExtra("pageIndex", id == R.id.ll_followers ? 1 : 0);
                startActivity(intent);
            }
        }
    }

    private void hideKeyboard() {
        View current = getCurrentFocus();
        if (current != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
            current.clearFocus();
        }
    }

    private void showShareAccountDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.share_account_layout);
        
        ImageView imvAvatar = dialog.findViewById(R.id.imvAvatarInSharedPlace);
        TextView txvName = dialog.findViewById(R.id.txvUsernameInSharedPlace);
        
        if (currentAvatarUrl != null) {
            Glide.with(this).load(currentAvatarUrl).placeholder(R.drawable.default_avatar).circleCrop().into(imvAvatar);
        }
        txvName.setText(txvUserName.getText());

        dialog.findViewById(R.id.btnCopyURL).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("toptop-link", "http://toptoptoptop.com/" + userId));
            Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
        });

        dialog.findViewById(R.id.txvCancelInSharedPlace).setOnClickListener(v -> dialog.cancel());
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private void showDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.bottom_sheet_layout);
        dialog.findViewById(R.id.llSetting).setOnClickListener(v -> startActivity(new Intent(this, SettingsAndPrivacyActivity.class)));
        dialog.findViewById(R.id.llSignOut).setOnClickListener(v -> {
            signOut();
            finish();
        });
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        GoogleSignIn.getClient(this, gso).signOut();
        startActivity(new Intent(this, HomeScreenActivity.class));
    }

    private void moveToAnotherActivity(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }

    public void setLikes(String userId) {
        db.collection("profiles").document(userId).collection("public_videos").get().addOnSuccessListener(queryDocumentSnapshots -> {
            totalLikes = 0;
            List<String> userVideos = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots) userVideos.add(doc.getId());
            
            if (userVideos.isEmpty()) {
                txvLikes.setText("0");
                return;
            }

            db.collection("likes").get().addOnSuccessListener(likesSnapshot -> {
                for (DocumentSnapshot doc : likesSnapshot) {
                    if (userVideos.contains(doc.getId())) {
                        totalLikes += doc.getData() != null ? doc.getData().size() : 0;
                    }
                }
                txvLikes.setText(String.valueOf(totalLikes));
            });
        });
    }
}
