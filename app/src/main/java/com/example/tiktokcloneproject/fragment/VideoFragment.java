package com.example.tiktokcloneproject.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.VideoAdapter;
import com.example.tiktokcloneproject.model.Video;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

public class VideoFragment extends Fragment {
    private Context context = null;
    private ViewPager2 viewPager2;
    private ArrayList<Video> videos;
    public VideoAdapter videoAdapter;
    private FirebaseFirestore db;

    public static VideoFragment newInstance(String strArg) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString("name", strArg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_video, null);

        db = FirebaseFirestore.getInstance();
        viewPager2 = layout.findViewById(R.id.viewPager);
        videos = new ArrayList<>();
        videoAdapter = new VideoAdapter(context, videos);
        VideoAdapter.setUser(FirebaseAuth.getInstance().getCurrentUser());
        viewPager2.setAdapter(videoAdapter);

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                videoAdapter.pauseVideo(videoAdapter.getCurrentPosition());
                videoAdapter.playVideo(position);
                videoAdapter.updateWatchCount(position);
                videoAdapter.updateCurrentPosition(position);
            }
        });

        loadVideos();
        return layout;
    }

    public void pauseVideo() {
        if (videoAdapter != null) {
            int currentPosition = videoAdapter.getCurrentPosition();
            videoAdapter.pauseVideo(currentPosition);
        }
    }

    public void continueVideo() {
        if (videoAdapter != null) {
            videoAdapter.playVideo(videoAdapter.getCurrentPosition());
        }
    }

    private void loadVideos() {
        db.collection("videos").addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;

            boolean isFirstLoad = videos.isEmpty();
            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                if (dc.getType() == DocumentChange.Type.ADDED) {
                    Video video = dc.getDocument().toObject(Video.class);
                    videos.add(0, video);
                    videoAdapter.notifyItemInserted(0);
                }
            }

            if (isFirstLoad && !videos.isEmpty()) {
                // SỬA LỖI MÀN ĐEN: Đảm bảo viewPager2 đã sẵn sàng hoàn toàn rồi mới phát
                viewPager2.post(() -> {
                    viewPager2.setCurrentItem(0, false);
                    videoAdapter.playVideo(0);
                });
            }
        });
    }
}
