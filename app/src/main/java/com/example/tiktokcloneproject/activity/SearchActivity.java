package com.example.tiktokcloneproject.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.WrapContentLinearLayoutManager;
import com.example.tiktokcloneproject.adapters.UserAdapter;
import com.example.tiktokcloneproject.adapters.VideoSummaryAdapter;
import com.example.tiktokcloneproject.model.User;
import com.example.tiktokcloneproject.model.VideoSummary;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class SearchActivity extends Activity implements View.OnClickListener {

    final String USERNAME_LABEL = "username";
    RecyclerView rcv_users;
    UserAdapter userAdapter;
    SearchView searchView;

    ArrayList<VideoSummary> videoSummaries;
    VideoSummaryAdapter videoSummaryAdapter;
    RecyclerView rcvVideoSummary;

    TextView tvSubmitSearch;
    final String TAG = "SearchActivity";
    ArrayList<User> userArrayList = new ArrayList<User>();

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching);

        userArrayList.clear();
        db = FirebaseFirestore.getInstance();

        tvSubmitSearch = findViewById(R.id.tvSubmitSearch);

        // Setup User RecyclerView
        rcv_users = findViewById(R.id.rcv_users);
        rcv_users.setLayoutManager(new WrapContentLinearLayoutManager(SearchActivity.this, LinearLayoutManager.VERTICAL, false));
        userAdapter = new UserAdapter(this, userArrayList);
        rcv_users.setAdapter(userAdapter);
        rcv_users.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Setup Hashtag/Video RecyclerView
        videoSummaries = new ArrayList<>();
        rcvVideoSummary = findViewById(R.id.rcvVideoSummary);
        videoSummaryAdapter = new VideoSummaryAdapter(getApplicationContext(), videoSummaries);
        rcvVideoSummary.setLayoutManager(new GridLayoutManager(this, 3));
        rcvVideoSummary.addItemDecoration(new GridSpacingItemDecoration(3, 10, true));
        rcvVideoSummary.setAdapter(videoSummaryAdapter);

        if (tvSubmitSearch != null) {
            tvSubmitSearch.setOnClickListener(this);
        }

        searchView = findViewById(R.id.searchView);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    performSearch(newText);
                    return false;
                }
            });
        }

        // Kiểm tra Intent xem có truyền Hashtag từ Video hay không
        String intentHashtag = getIntent().getStringExtra("hashtag");
        if (intentHashtag != null && !intentHashtag.isEmpty()) {
            searchView.setQuery(intentHashtag, true);
        }
    }

    private void performSearch(String newText) {
        if (!newText.isEmpty()) {
            if (newText.startsWith("#")) {
                showHashtagResults(true);
                if (newText.length() > 1) {
                    setVideoSummaries(newText);
                } else {
                    videoSummaries.clear();
                    videoSummaryAdapter.notifyDataSetChanged();
                }
            } else {
                showHashtagResults(false);
                getData(newText);
            }
        } else {
            clearAllResults();
        }
    }

    private void showHashtagResults(boolean isHashtag) {
        if (isHashtag) {
            rcv_users.setVisibility(View.GONE);
            rcvVideoSummary.setVisibility(View.VISIBLE);
            userArrayList.clear();
            userAdapter.notifyDataSetChanged();
        } else {
            rcv_users.setVisibility(View.VISIBLE);
            rcvVideoSummary.setVisibility(View.GONE);
            videoSummaries.clear();
            videoSummaryAdapter.notifyDataSetChanged();
        }
    }

    private void clearAllResults() {
        userArrayList.clear();
        userAdapter.notifyDataSetChanged();
        videoSummaries.clear();
        videoSummaryAdapter.notifyDataSetChanged();
        rcv_users.setVisibility(View.VISIBLE);
        rcvVideoSummary.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userAdapter != null) {
            userAdapter.release();
        }
    }

    private void getData(String key) {
        userArrayList.clear();
        db.collection("users")
                .orderBy(USERNAME_LABEL)
                .startAt(key)
                .endAt(key + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            userArrayList.add(new User(document.getString("userId"), document.getString(USERNAME_LABEL)));
                            userAdapter.notifyItemInserted(userArrayList.size() - 1);
                        }
                    } else {
                        Toast.makeText(SearchActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setVideoSummaries(String hashtag) {
        videoSummaries.clear();
        db.collection("hashtags").document(hashtag).collection("video_summaries")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            VideoSummary vs = document.toObject(VideoSummary.class);
                            videoSummaries.add(vs);
                            videoSummaryAdapter.notifyItemInserted(videoSummaries.size() - 1);
                        }
                    } else {
                        Log.e(TAG, "Error getting hashtags", task.getException());
                    }
                });
    }

    @Override
    public void onClick(View view) {
        if (tvSubmitSearch != null && view.getId() == tvSubmitSearch.getId()) {
            searchView.clearFocus();
        }
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount, spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
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
