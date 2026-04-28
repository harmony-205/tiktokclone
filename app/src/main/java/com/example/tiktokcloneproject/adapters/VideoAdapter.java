package com.example.tiktokcloneproject.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.CommentActivity;
import com.example.tiktokcloneproject.helper.OnSwipeTouchListener;
import com.example.tiktokcloneproject.model.Video;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.video.VideoSize;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private List<Video> videos;
    private Context context;
    private static FirebaseUser user = null;
    private int currentPosition = 0;
    private RecyclerView recyclerView;

    public VideoAdapter(Context context, List<Video> videos) {
        this.context = context;
        this.videos = videos;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VideoViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.releasePlayer();
    }

    public static void setUser(FirebaseUser user) {
        VideoAdapter.user = user;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VideoViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.video_container, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        holder.setVideoObjects(videos.get(position));
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void updateCurrentPosition(int pos) {
        this.currentPosition = pos;
    }

    public void playVideo(int position) {
        if (recyclerView != null) {
            VideoViewHolder holder = (VideoViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
            if (holder != null) {
                if (holder.exoPlayer == null) {
                    holder.setVideoObjects(videos.get(position));
                }
                holder.playVideo();
            }
        }
    }

    public void pauseVideo(int position) {
        if (recyclerView != null) {
            VideoViewHolder holder = (VideoViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
            if (holder != null) holder.pauseVideo();
        }
    }

    public void releaseAll() {
        if (recyclerView != null) {
            for (int i = 0; i < getItemCount(); i++) {
                VideoViewHolder holder = (VideoViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                if (holder != null) holder.releasePlayer();
            }
        }
    }

    public void updateWatchCount(int position) {
        if (position >= 0 && position < videos.size()) {
            Video video = videos.get(position);
            FirebaseFirestore.getInstance().collection("videos")
                    .document(video.getVideoId())
                    .update("watchCount", FieldValue.increment(1));
            video.setWatchCount(video.getWatchCount() + 1);
        }
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {
        StyledPlayerView videoView;
        ExoPlayer exoPlayer;
        ImageView imvAvatar, imvShare, imvMore, imvLike, imvComment;
        TextView tvTitle, txvDescription, tvCommentCount, tvLikeCount;
        FirebaseFirestore db;
        boolean isLiked = false;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            txvDescription = itemView.findViewById(R.id.txvDescription);
            tvCommentCount = itemView.findViewById(R.id.tvComment);
            tvLikeCount = itemView.findViewById(R.id.tvFavorites);
            imvAvatar = itemView.findViewById(R.id.imvAvatar);
            imvShare = itemView.findViewById(R.id.imvShare);
            imvMore = itemView.findViewById(R.id.imvMore);
            imvLike = itemView.findViewById(R.id.imvLike);
            imvComment = itemView.findViewById(R.id.imvComment);

            db = FirebaseFirestore.getInstance();
            videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

            if (imvShare != null) {
                imvShare.setOnClickListener(v -> Toast.makeText(context, "Sharing...", Toast.LENGTH_SHORT).show());
            }
            if (imvMore != null) {
                imvMore.setOnClickListener(v -> Toast.makeText(context, "More options", Toast.LENGTH_SHORT).show());
            }

            videoView.setOnTouchListener(new OnSwipeTouchListener(itemView.getContext()) {
                @Override public void onSwipeLeft() {}
            });
        }

        public void playVideo() {
            if (exoPlayer != null) {
                exoPlayer.setPlayWhenReady(true);
                exoPlayer.play();
                
                videoView.post(() -> {
                    videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
                    videoView.post(() -> {
                        videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                        videoView.requestLayout();
                    });
                });
            }
        }

        public void pauseVideo() {
            if (exoPlayer != null) {
                exoPlayer.setPlayWhenReady(false);
            }
        }

        public void releasePlayer() {
            if (exoPlayer != null) {
                exoPlayer.clearVideoSurface(); 
                videoView.setPlayer(null); 
                exoPlayer.stop();
                exoPlayer.release();
                exoPlayer = null;
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        public void setVideoObjects(Video video) {
            tvTitle.setText("@" + video.getUsername());
            txvDescription.setText(video.getDescription());
            tvCommentCount.setText(String.valueOf(video.getTotalComments()));
            tvLikeCount.setText(String.valueOf(video.getTotalLikes()));

            checkLikeStatus(video);

            imvLike.setOnClickListener(v -> handleLikeClick(video));
            imvComment.setOnClickListener(v -> openComments(video));

            releasePlayer();

            View surfaceView = videoView.getVideoSurfaceView();
            if (surfaceView instanceof TextureView) {
                ((TextureView) surfaceView).setTransform(new Matrix());
            }

            exoPlayer = new ExoPlayer.Builder(context).build();
            videoView.setPlayer(exoPlayer);
            
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                    }
                }
            });

            exoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            exoPlayer.setMediaItem(MediaItem.fromUri(video.getVideoUri()));
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            exoPlayer.prepare();

            if (getBindingAdapterPosition() == currentPosition) {
                playVideo();
            }
        }

        private void checkLikeStatus(Video video) {
            if (user == null) {
                imvLike.setColorFilter(Color.WHITE);
                isLiked = false;
                return;
            }
            db.collection("likes")
                .document(video.getVideoId())
                .collection("user_likes")
                .document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        isLiked = true;
                        imvLike.setColorFilter(Color.parseColor("#FE2C55")); // Pink/Red TikTok color
                    } else {
                        isLiked = false;
                        imvLike.setColorFilter(Color.WHITE);
                    }
                });
        }

        private void handleLikeClick(Video video) {
            if (user == null) {
                Toast.makeText(context, "Vui lòng đăng nhập để like", Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentReference likeDoc = db.collection("likes")
                .document(video.getVideoId())
                .collection("user_likes")
                .document(user.getUid());

            DocumentReference videoDoc = db.collection("videos").document(video.getVideoId());

            if (isLiked) {
                // Unlike
                likeDoc.delete().addOnSuccessListener(aVoid -> {
                    isLiked = false;
                    imvLike.setColorFilter(Color.WHITE);
                    videoDoc.update("totalLikes", FieldValue.increment(-1));
                    int currentLikes = Integer.parseInt(tvLikeCount.getText().toString());
                    tvLikeCount.setText(String.valueOf(Math.max(0, currentLikes - 1)));
                });
            } else {
                // Like
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("timestamp", FieldValue.serverTimestamp());
                likeDoc.set(likeData).addOnSuccessListener(aVoid -> {
                    isLiked = true;
                    imvLike.setColorFilter(Color.parseColor("#FE2C55"));
                    videoDoc.update("totalLikes", FieldValue.increment(1));
                    int currentLikes = Integer.parseInt(tvLikeCount.getText().toString());
                    tvLikeCount.setText(String.valueOf(currentLikes + 1));
                });
            }
        }

        private void openComments(Video video) {
            Intent intent = new Intent(context, CommentActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("videoId", video.getVideoId());
            bundle.putString("authorId", video.getAuthorId());
            bundle.putInt("totalComments", video.getTotalComments());
            intent.putExtras(bundle);
            context.startActivity(intent);
        }
    }
}
