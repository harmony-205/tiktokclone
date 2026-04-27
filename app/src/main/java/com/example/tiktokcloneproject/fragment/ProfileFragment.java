package com.example.tiktokcloneproject.fragment;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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

import com.example.tiktokcloneproject.activity.EditProfileActivity;
import com.example.tiktokcloneproject.activity.FollowListActivity;
import com.example.tiktokcloneproject.activity.FullScreenAvatarActivity;
import com.example.tiktokcloneproject.activity.HomeScreenActivity;
import com.example.tiktokcloneproject.activity.MainActivity;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.SettingsAndPrivacyActivity;
import com.example.tiktokcloneproject.activity.SigninChoiceActivity;
import com.example.tiktokcloneproject.activity.SignupChoiceActivity;
import com.example.tiktokcloneproject.adapters.VideoSummaryAdapter;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.VideoSummary;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment implements View.OnClickListener {
    final String USERNAME_LABEL = "username";
    private Context context = null;
    private TextView txvFollowing, txvFollowers, txvLikes, txvUserName, txvMenu;
    private EditText edtBio;
    private Button btn, btnEditProfile, btnUpdateBio, btnCancelUpdateBio;
    private LinearLayout llFollowing, llFollowers;
    ImageView imvAvatarProfile;
    Uri avatarUri;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseStorage storage;
    StorageReference storageReference;
    Bitmap bitmap;
    String userId;
    DocumentReference docRef;
    String oldBioText, currentUserID;
    String TAG="test";
    RecyclerView recVideoSummary;
    ArrayList<VideoSummary> videoSummaries;
    LinearLayout layout;
    int totalLikes = 0;
    public static ProfileFragment newInstance(String strArg,  String profileLinkId) {
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
        try {
            context = getActivity();
        }
        catch (IllegalStateException e) {
            throw new IllegalStateException();
        }
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
        layout = (LinearLayout) inflater.inflate(R.layout.fragment_profile, null);

        txvFollowing = (TextView)layout.findViewById(R.id.text_following);
        txvFollowers = (TextView)layout.findViewById(R.id.text_followers);
        txvLikes = (TextView)layout.findViewById(R.id.text_likes);
        txvUserName = (TextView)layout.findViewById(R.id.txv_username);
        txvMenu = (TextView)layout.findViewById(R.id.text_menu);
        edtBio = (EditText)layout.findViewById(R.id.edt_bio);
        btnEditProfile =(Button)layout.findViewById(R.id.button_edit_profile);
        imvAvatarProfile = (ImageView) layout.findViewById(R.id.imvAvatarProfile);
        llFollowers = (LinearLayout) layout.findViewById(R.id.ll_followers);
        llFollowing = (LinearLayout) layout.findViewById(R.id.ll_following);
        recVideoSummary = (RecyclerView)layout.findViewById(R.id.recycle_view_video_summary);
        btnUpdateBio = (Button) layout.findViewById(R.id.btn_update_bio);
        btnCancelUpdateBio = (Button) layout.findViewById(R.id.btn_cancel_update_bio);

        btnUpdateBio.setOnClickListener(this);
        btnCancelUpdateBio.setOnClickListener(this);
        llFollowers.setOnClickListener(this);
        llFollowing.setOnClickListener(this);
        txvMenu.setOnClickListener(this);
        imvAvatarProfile.setOnClickListener(this);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        db = FirebaseFirestore.getInstance();

        if (user != null) {
            setLikes(user.getUid());
            currentUserID = user.getUid();
            if (userId != null && userId.equals(currentUserID)) {
                btn = (Button)layout.findViewById(R.id.button_edit_profile);
                edtBio.setVisibility(View.VISIBLE);
                btn.setVisibility(View.VISIBLE);
                docRef = db.collection("profiles").document(userId);
                oldBioText = edtBio.getText().toString();
                edtBio.setOnFocusChangeListener((view, b) -> {
                    if (b) {
                        layout.findViewById(R.id.layout_bio).setVisibility(View.VISIBLE);
                    } else {
                        layout.findViewById(R.id.layout_bio).setVisibility(View.GONE);
                    }
                });
                btnEditProfile.setOnClickListener(this);
            } else {
                handleFollow();
            }
        } else {
            handleFollow();
        }

        videoSummaries = new ArrayList<>();
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 3);
        recVideoSummary.setLayoutManager(gridLayoutManager);
        recVideoSummary.addItemDecoration(new ProfileFragment.GridSpacingItemDecoration(3, 10, true));
        if (userId != null && !userId.isEmpty()) {
            setVideoSummaries();
        }

        return layout;
    }

    private void handleFollow() {
        btn = (Button)layout.findViewById(R.id.button_follow);
        if (btn != null) btn.setVisibility(View.VISIBLE);

        if (userId != null && !userId.isEmpty()) {
            docRef = db.collection("profiles").document(userId);
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        txvFollowing.setText(String.valueOf(document.get("following")));
                        txvFollowers.setText(String.valueOf(document.get("followers")));
                        txvLikes.setText(String.valueOf(document.get("likes")));
                        txvUserName.setText("@" + document.getString(USERNAME_LABEL));
                    }
                }
            });

            if (user != null) {
                db.collection("profiles").document(currentUserID)
                        .collection("following").document(userId).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            handleFollowed();
                        } else {
                            handleUnfollowed();
                        }
                    }
                });
            } else {
                btn.setOnClickListener(view -> {
                    Intent intentMain = new Intent(context, MainActivity.class);
                    startActivity(intentMain);
                });
            }
        }
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
                                    (Long)document.get("watchCount")));
                        }
                        if (videoSummaries.size() > 0) {
                            VideoSummaryAdapter videoSummaryAdapter = new VideoSummaryAdapter(context, videoSummaries);
                            recVideoSummary.setAdapter(videoSummaryAdapter);
                        }
                    }
                });
    }

    @Override public void onStart() {
        super.onStart();
        if (docRef != null) {
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    DocumentSnapshot document = task.getResult();
                    txvFollowing.setText(String.valueOf(document.get("following")));
                    txvFollowers.setText(String.valueOf(document.get("followers")));
                    txvLikes.setText(String.valueOf(document.get("likes")));
                    txvUserName.setText("@" + document.getString(USERNAME_LABEL));
                    oldBioText = document.getString("bio");
                    edtBio.setText(oldBioText);
                }
            });
        }
    }

    void updateBio() {
        if (docRef != null) {
            docRef.update("bio", edtBio.getText().toString());
            oldBioText = edtBio.getText().toString();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.text_menu) {
            showDialog();
            return;
        }
        if (v.getId() == R.id.imvAvatarProfile) {
            showShareAccountDialog();
            return;
        }
        if (v.getId() == R.id.button_edit_profile) {
            moveToAnotherActivity(EditProfileActivity.class);
        }
        if(v.getId() == R.id.btn_update_bio) {
            updateBio();
            layout.findViewById(R.id.layout_bio).setVisibility(View.GONE);
            hideKeyboard();
        }
        if(v.getId() == R.id.btn_cancel_update_bio) {
            edtBio.setText(oldBioText);
            layout.findViewById(R.id.layout_bio).setVisibility(View.GONE);
            hideKeyboard();
        }
        if (v.getId() == R.id.ll_followers) {
            Intent intent = new Intent(context, FollowListActivity.class);
            intent.putExtra("pageIndex", 1);
            startActivity(intent);
        }
        if (v.getId() == R.id.ll_following) {
            Intent intent = new Intent(context, FollowListActivity.class);
            intent.putExtra("pageIndex", 0);
            startActivity(intent);
        }
    }

    private void hideKeyboard() {
        View current = getActivity().getCurrentFocus();
        if (current != null) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
            current.clearFocus();
        }
    }

    private void showShareAccountDialog() {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.share_account_layout);
        TextView txvUsernameInSharedPlace = dialog.findViewById(R.id.txvUsernameInSharedPlace);
        ImageView imvAvatarInSharedPlace = dialog.findViewById(R.id.imvAvatarInSharedPlace);
        Button btnCopyURL = dialog.findViewById(R.id.btnCopyURL);
        imvAvatarInSharedPlace.setImageBitmap(bitmap);
        txvUsernameInSharedPlace.setText(txvUserName.getText());
        btnCopyURL.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("toptop-link", "http://toptoptoptop.com/" + (user != null ? user.getUid() : ""));
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Profile link has been saved to clipboard", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private void showDialog() {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_layout);
        dialog.findViewById(R.id.llSetting).setOnClickListener(view -> startActivity(new Intent(context, SettingsAndPrivacyActivity.class)));
        dialog.findViewById(R.id.llSignOut).setOnClickListener(view -> {
            signOut();
            getActivity().finish();
        });
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("996817465542-qt39vo4n1u3i2ilrnp0vi36s18h2smvb.apps.googleusercontent.com")
                .requestEmail()
                .build();
        GoogleSignIn.getClient(getActivity(), gso).signOut();
        startActivity(new Intent(context, HomeScreenActivity.class));
    }

    private void moveToAnotherActivity(Class<?> cls) {
        startActivity(new Intent(context, cls));
    }

    private void handleUnfollowed() {
        btn.setText("Follow");
        btn.setOnClickListener(view -> {
            Map<String, Object> Data = new HashMap<>();
            Data.put("userID", userId);
            db.collection("profiles").document(currentUserID).collection("following").document(userId).set(Data).addOnSuccessListener(aVoid -> {
                db.collection("profiles").document(currentUserID).update("following", FieldValue.increment(1));
                handleFollowed();
            });
            Map<String, Object> Data1 = new HashMap<>();
            Data1.put("userID", currentUserID);
            db.collection("profiles").document(userId).collection("followers").document(currentUserID).set(Data1).addOnSuccessListener(aVoid -> {
                db.collection("profiles").document(userId).update("followers", FieldValue.increment(1));
            });
        });
    }

    private void handleFollowed() {
        btn.setText("Unfollow");
        btn.setOnClickListener(view -> {
            db.collection("profiles").document(currentUserID).collection("following").document(userId).delete().addOnSuccessListener(aVoid -> {
                db.collection("profiles").document(currentUserID).update("following", FieldValue.increment(-1));
                handleUnfollowed();
            });
            db.collection("profiles").document(userId).collection("followers").document(currentUserID).delete().addOnSuccessListener(aVoid -> {
                db.collection("profiles").document(userId).update("followers", FieldValue.increment(-1));
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != null && !userId.isEmpty() && storageReference != null) {
            StorageReference download = storageReference.child("/user_avatars").child(userId);
            download.getBytes(StaticVariable.MAX_BYTES_AVATAR).addOnSuccessListener(bytes -> {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imvAvatarProfile.setImageBitmap(bitmap);
            });
        }
    }

    public void setLikes(String userId) {
        db.collection("profiles").document(userId).collection("public_videos").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ArrayList<String> userVideos = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    userVideos.add(String.valueOf(document.get("videoId")));
                }
                db.collection("likes").get().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        totalLikes = 0;
                        for (QueryDocumentSnapshot document : task1.getResult()) {
                            if (userVideos.contains(document.getId())) {
                                totalLikes += document.getData().size();
                            }
                        }
                        txvLikes.setText(String.valueOf(totalLikes));
                    }
                });
            }
        });
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;
        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
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