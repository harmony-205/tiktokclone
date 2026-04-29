package com.example.tiktokcloneproject.fragment;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.EditProfileActivity;
import com.example.tiktokcloneproject.activity.FollowListActivity;
import com.example.tiktokcloneproject.activity.HomeScreenActivity;
import com.example.tiktokcloneproject.activity.SettingsAndPrivacyActivity;
import com.example.tiktokcloneproject.adapters.VideoSummaryAdapter;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Notification;
import com.example.tiktokcloneproject.model.VideoSummary;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment implements View.OnClickListener {
    private Context context = null;
    private TextView txvFollowing, txvFollowers, txvLikes, txvUserName, txvMenu;
    private EditText edtBio;
    private Button btnEditProfile, btnUpdateBio, btnCancelUpdateBio;
    private LinearLayout llFollowing, llFollowers;
    private ImageView imvAvatarProfile;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private String userId;
    private DocumentReference profileDocRef, userDocRef;
    private ListenerRegistration userListener, profileListener, videosListener;
    private String oldBioText, currentUserID;
    private static final String TAG = "ProfileFragment";
    private RecyclerView recVideoSummary;
    private ArrayList<VideoSummary> videoSummaries;
    private VideoSummaryAdapter videoSummaryAdapter;
    private LinearLayout layout;
    private int totalLikes = 0;
    private String currentAvatarUrl;

    public static ProfileFragment newInstance(String strArg, String profileLinkId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("name", strArg);
        args.putString("id", profileLinkId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle idBundle = getArguments();
        userId = idBundle != null ? idBundle.getString("id") : "";
        context = getActivity();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if (userId == null || userId.isEmpty()) {
            if (user != null) {
                userId = user.getUid();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layout = (LinearLayout) inflater.inflate(R.layout.fragment_profile, container, false);

        txvFollowing = layout.findViewById(R.id.text_following);
        txvFollowers = layout.findViewById(R.id.text_followers);
        txvLikes = layout.findViewById(R.id.text_likes);
        txvUserName = layout.findViewById(R.id.txv_username);
        txvMenu = layout.findViewById(R.id.text_menu);
        edtBio = layout.findViewById(R.id.edt_bio);
        btnEditProfile = layout.findViewById(R.id.button_edit_profile);
        imvAvatarProfile = layout.findViewById(R.id.imvAvatarProfile);
        llFollowers = layout.findViewById(R.id.ll_followers);
        llFollowing = layout.findViewById(R.id.ll_following);
        recVideoSummary = layout.findViewById(R.id.recycle_view_video_summary);
        btnUpdateBio = layout.findViewById(R.id.btn_update_bio);
        btnCancelUpdateBio = layout.findViewById(R.id.btn_cancel_update_bio);

        btnUpdateBio.setOnClickListener(this);
        btnCancelUpdateBio.setOnClickListener(this);
        llFollowers.setOnClickListener(this);
        llFollowing.setOnClickListener(this);
        txvMenu.setOnClickListener(this);
        imvAvatarProfile.setOnClickListener(this);

        db = FirebaseFirestore.getInstance();

        if (user != null) {
            currentUserID = user.getUid();
            if (userId != null && userId.equals(currentUserID)) {
                edtBio.setVisibility(View.VISIBLE);
                btnEditProfile.setVisibility(View.VISIBLE);
                oldBioText = edtBio.getText().toString();
                edtBio.setOnFocusChangeListener((view, b) -> {
                    layout.findViewById(R.id.layout_bio).setVisibility(b ? View.VISIBLE : View.GONE);
                });
                btnEditProfile.setOnClickListener(this);
            } else {
                handleFollow();
            }
            setLikes(userId);
        }

        profileDocRef = db.collection("profiles").document(userId);
        userDocRef = db.collection("users").document(userId);

        videoSummaries = new ArrayList<>();
        videoSummaryAdapter = new VideoSummaryAdapter(context, videoSummaries);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 3);
        recVideoSummary.setLayoutManager(gridLayoutManager);
        recVideoSummary.setAdapter(videoSummaryAdapter);
        recVideoSummary.addItemDecoration(new GridSpacingItemDecoration(3, 10, true));
        
        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();
        startListeningToData();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userListener != null) userListener.remove();
        if (profileListener != null) profileListener.remove();
        if (videosListener != null) videosListener.remove();
    }

    private void startListeningToData() {
        if (userId == null || userId.isEmpty()) return;

        profileListener = profileDocRef.addSnapshotListener((document, e) -> {
            if (e != null || document == null || !document.exists()) return;
            txvFollowing.setText(String.valueOf(document.getLong("following") != null ? document.getLong("following") : 0));
            txvFollowers.setText(String.valueOf(document.getLong("followers") != null ? document.getLong("followers") : 0));
            txvLikes.setText(String.valueOf(document.getLong("likes") != null ? document.getLong("likes") : 0));
            String bio = document.getString("bio");
            if (bio != null) {
                edtBio.setText(bio);
                oldBioText = bio;
            }
        });

        userListener = userDocRef.addSnapshotListener((document, e) -> {
            if (e != null || document == null || !document.exists()) return;
            txvUserName.setText("@" + document.getString("username"));
            String avatarUrl = document.getString("avatarUrl");
            if (avatarUrl != null) {
                currentAvatarUrl = avatarUrl;
                updateAvatarImage();
            }
        });

        // Real-time videos update
        videosListener = db.collection("profiles").document(userId).collection("public_videos")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    videoSummaries.clear();
                    for (QueryDocumentSnapshot document : snapshots) {
                        videoSummaries.add(new VideoSummary(document.getString("videoId"),
                                document.getString("thumbnailUri"),
                                document.getLong("watchCount")));
                    }
                    videoSummaryAdapter.notifyDataSetChanged();
                });
    }

    private void updateAvatarImage() {
        if (getActivity() == null || isDetached()) return;
        
        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
            Glide.with(this)
                 .load(currentAvatarUrl)
                 .placeholder(R.drawable.default_avatar)
                 .error(R.drawable.default_avatar)
                 .circleCrop()
                 .diskCacheStrategy(DiskCacheStrategy.NONE)
                 .skipMemoryCache(true)
                 .signature(new ObjectKey(System.currentTimeMillis()))
                 .into(imvAvatarProfile);
        } else {
            imvAvatarProfile.setImageResource(R.drawable.default_avatar);
        }
    }

    private void handleFollow() {
        Button btnFollow = layout.findViewById(R.id.button_follow);
        if (btnFollow != null) btnFollow.setVisibility(View.VISIBLE);

        if (user != null && userId != null) {
            db.collection("profiles").document(currentUserID)
                    .collection("following").document(userId)
                    .addSnapshotListener((document, e) -> {
                        if (document != null) {
                            if (document.exists()) handleFollowedUI(btnFollow);
                            else handleUnfollowedUI(btnFollow);
                        }
                    });
        }
    }

    private void handleFollowedUI(Button btnFollow) {
        btnFollow.setText("Unfollow");
        btnFollow.setOnClickListener(v -> {
            db.collection("profiles").document(currentUserID).collection("following").document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("profiles").document(currentUserID).update("following", FieldValue.increment(-1));
                    db.collection("profiles").document(userId).update("followers", FieldValue.increment(-1));
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
                });
            Map<String, Object> data1 = new HashMap<>();
            data1.put("userID", currentUserID);
            db.collection("profiles").document(userId).collection("followers").document(currentUserID).set(data1);
        });
    }

    public void notifyFollow() {
        if (user == null) return;
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String username = document.getString("username");
                Notification.pushNotification(username, userId, StaticVariable.FOLLOW);
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.text_menu) {
            showDialog();
        } else if (id == R.id.imvAvatarProfile) {
            showShareAccountDialog();
        } else if (id == R.id.button_edit_profile) {
            startActivity(new Intent(context, EditProfileActivity.class));
        } else if (id == R.id.btn_update_bio) {
            profileDocRef.update("bio", edtBio.getText().toString());
            oldBioText = edtBio.getText().toString();
            layout.findViewById(R.id.layout_bio).setVisibility(View.GONE);
            hideKeyboard();
        } else if (id == R.id.btn_cancel_update_bio) {
            edtBio.setText(oldBioText);
            layout.findViewById(R.id.layout_bio).setVisibility(View.GONE);
            hideKeyboard();
        } else if (id == R.id.ll_followers || id == R.id.ll_following) {
            Intent intent = new Intent(context, FollowListActivity.class);
            intent.putExtra("pageIndex", id == R.id.ll_followers ? 1 : 0);
            startActivity(intent);
        }
    }

    private void hideKeyboard() {
        View current = getActivity().getCurrentFocus();
        if (current != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
            current.clearFocus();
        }
    }

    private void showShareAccountDialog() {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.share_account_layout);
        
        ImageView imvAvatar = dialog.findViewById(R.id.imvAvatarInSharedPlace);
        TextView txvName = dialog.findViewById(R.id.txvUsernameInSharedPlace);
        
        if (currentAvatarUrl != null) {
            Glide.with(this).load(currentAvatarUrl).placeholder(R.drawable.default_avatar).circleCrop().into(imvAvatar);
        }
        txvName.setText(txvUserName.getText());

        dialog.findViewById(R.id.btnCopyURL).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("toptop-link", "http://toptoptoptop.com/" + userId));
            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show();
        });

        dialog.findViewById(R.id.txvCancelInSharedPlace).setOnClickListener(v -> dialog.cancel());
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private void showDialog() {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.bottom_sheet_layout);
        dialog.findViewById(R.id.llSetting).setOnClickListener(v -> startActivity(new Intent(context, SettingsAndPrivacyActivity.class)));
        dialog.findViewById(R.id.llSignOut).setOnClickListener(v -> {
            signOut();
            getActivity().finish();
        });
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken("996817465542-qt39vo4n1u3i2ilrnp0vi36s18h2smvb.apps.googleusercontent.com").requestEmail().build();
        GoogleSignIn.getClient(getActivity(), gso).signOut();
        startActivity(new Intent(context, HomeScreenActivity.class));
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
}
