package com.example.tiktokcloneproject.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.CommentActivity;
import com.example.tiktokcloneproject.activity.ProfileActivity;
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
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            Video video = videos.get(position);
            for (Object payload : payloads) {
                if (payload.equals("UPDATE_COUNTS")) {
                    holder.updateCounts(video);
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
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

    public class VideoViewHolder extends RecyclerView.ViewHolder {
        StyledPlayerView videoView;
        ExoPlayer exoPlayer;
        ImageView imvAvatar, imvShare, imvMore, imvLike, imvComment;
        TextView tvTitle, txvDescription, tvCommentCount, tvLikeCount, tvShareCount;
        FirebaseFirestore db;
        boolean isLiked = false;
        String currentUri = "";
        
        private final Handler watchHandler = new Handler(Looper.getMainLooper());
        private Runnable watchRunnable;
        private boolean viewCountedForCurrentLoop = false;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            txvDescription = itemView.findViewById(R.id.txvDescription);
            tvCommentCount = itemView.findViewById(R.id.tvComment);
            tvLikeCount = itemView.findViewById(R.id.tvFavorites);
            tvShareCount = itemView.findViewById(R.id.tvShare);
            imvAvatar = itemView.findViewById(R.id.imvAvatar);
            imvShare = itemView.findViewById(R.id.imvShare);
            imvMore = itemView.findViewById(R.id.imvMore);
            imvLike = itemView.findViewById(R.id.imvLike);
            imvComment = itemView.findViewById(R.id.imvComment);

            db = FirebaseFirestore.getInstance();
            videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

            if (imvMore != null) {
                imvMore.setOnClickListener(v -> Toast.makeText(context, "More options", Toast.LENGTH_SHORT).show());
            }

            videoView.setOnTouchListener(new OnSwipeTouchListener(itemView.getContext()) {
                @Override public void onSwipeLeft() {}
            });
        }

        public void updateCounts(Video video) {
            tvCommentCount.setText(String.valueOf(video.getTotalComments()));
            tvLikeCount.setText(String.valueOf(video.getTotalLikes()));
            if (tvShareCount != null) {
                tvShareCount.setText(String.valueOf(video.getTotalShares()));
            }
        }

        public void playVideo() {
            if (exoPlayer != null) {
                exoPlayer.setPlayWhenReady(true);
                exoPlayer.play();
                
                videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                videoView.post(() -> {
                    videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                    videoView.requestLayout();
                });
                
                startWatchTimer();
            }
        }

        public void pauseVideo() {
            if (exoPlayer != null) {
                exoPlayer.setPlayWhenReady(false);
            }
            stopWatchTimer();
        }

        public void releasePlayer() {
            stopWatchTimer();
            if (exoPlayer != null) {
                exoPlayer.clearVideoSurface(); 
                videoView.setPlayer(null); 
                exoPlayer.stop();
                exoPlayer.release();
                exoPlayer = null;
                currentUri = "";
            }
        }

        private void startWatchTimer() {
            stopWatchTimer();
            if (exoPlayer == null || viewCountedForCurrentLoop) return;

            watchRunnable = new Runnable() {
                @Override
                public void run() {
                    if (exoPlayer != null && exoPlayer.getPlayWhenReady()) {
                        long duration = exoPlayer.getDuration();
                        long position = exoPlayer.getCurrentPosition();
                        
                        if (duration > 0 && position >= (duration * 0.2)) {
                            incrementViewCount();
                            viewCountedForCurrentLoop = true;
                        } else {
                            watchHandler.postDelayed(this, 500);
                        }
                    }
                }
            };
            watchHandler.postDelayed(watchRunnable, 500);
        }

        private void stopWatchTimer() {
            if (watchRunnable != null) {
                watchHandler.removeCallbacks(watchRunnable);
            }
        }

        private void incrementViewCount() {
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            
            Video video = videos.get(pos);
            String vid = video.getVideoId();
            String aid = video.getAuthorId();
            
            if (vid == null || vid.isEmpty()) return;

            db.collection("videos").document(vid).update("watchCount", FieldValue.increment(1));
            
            if (aid != null && !aid.isEmpty()) {
                db.collection("profiles").document(aid).collection("public_videos")
                    .document(vid).update("watchCount", FieldValue.increment(1));
            }
            
            video.setWatchCount(video.getWatchCount() + 1);
        }

        @SuppressLint("ClickableViewAccessibility")
        public void setVideoObjects(Video video) {
            tvTitle.setText("@" + video.getUsername());
            txvDescription.setText(video.getDescription());
            updateCounts(video);

            loadAuthorAvatar(video.getAuthorId());
            checkLikeStatus(video);

            imvLike.setOnClickListener(v -> handleLikeClick(video));
            imvComment.setOnClickListener(v -> openComments(video));
            imvShare.setOnClickListener(v -> handleShareClick(video));
            
            View.OnClickListener openProfileListener = v -> {
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("id", video.getAuthorId());
                context.startActivity(intent);
            };
            
            imvAvatar.setOnClickListener(openProfileListener);
            tvTitle.setOnClickListener(openProfileListener);

            if (exoPlayer != null && currentUri.equals(video.getVideoUri())) {
                return;
            }

            releasePlayer();
            currentUri = video.getVideoUri();
            viewCountedForCurrentLoop = false;

            View surfaceView = videoView.getVideoSurfaceView();
            if (surfaceView instanceof TextureView) {
                surfaceView.setRotation(0);
                ((TextureView) surfaceView).setTransform(new Matrix());
            }

            exoPlayer = new ExoPlayer.Builder(context).build();
            videoView.setPlayer(exoPlayer);
            
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                        videoView.requestLayout();
                    }
                }

                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_READY) {
                        startWatchTimer();
                    }
                }

                @Override
                public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                    viewCountedForCurrentLoop = false;
                    startWatchTimer();
                }

                @Override
                public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                    if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION || reason == Player.DISCONTINUITY_REASON_SEEK) {
                        if (newPosition.positionMs < oldPosition.positionMs) {
                            viewCountedForCurrentLoop = false;
                            startWatchTimer();
                        }
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

        private void loadAuthorAvatar(String authorId) {
            if (authorId == null || authorId.isEmpty()) return;
            
            db.collection("users").document(authorId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String avatarUrl = documentSnapshot.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(context)
                                .load(avatarUrl)
                                .circleCrop()
                                .placeholder(R.drawable.default_avatar)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(imvAvatar);
                    } else {
                        imvAvatar.setImageResource(R.drawable.default_avatar);
                    }
                }
            });
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
                        imvLike.setColorFilter(Color.parseColor("#FE2C55")); 
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
            DocumentReference profileDoc = db.collection("profiles").document(video.getAuthorId());

            if (isLiked) {
                likeDoc.delete().addOnSuccessListener(aVoid -> {
                    isLiked = false;
                    imvLike.setColorFilter(Color.WHITE);
                    videoDoc.update("totalLikes", FieldValue.increment(-1));
                    profileDoc.update("likes", FieldValue.increment(-1));
                    
                    // Cập nhật trong sub-collection public_videos của profile
                    profileDoc.collection("public_videos").document(video.getVideoId())
                        .update("totalLikes", FieldValue.increment(-1));

                    int currentLikes = Integer.parseInt(tvLikeCount.getText().toString());
                    tvLikeCount.setText(String.valueOf(Math.max(0, currentLikes - 1)));
                });
            } else {
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("timestamp", FieldValue.serverTimestamp());
                likeDoc.set(likeData).addOnSuccessListener(aVoid -> {
                    isLiked = true;
                    imvLike.setColorFilter(Color.parseColor("#FE2C55"));
                    videoDoc.update("totalLikes", FieldValue.increment(1));
                    profileDoc.update("likes", FieldValue.increment(1));
                    
                    // Cập nhật trong sub-collection public_videos của profile
                    profileDoc.collection("public_videos").document(video.getVideoId())
                        .update("totalLikes", FieldValue.increment(1));

                    int currentLikes = Integer.parseInt(tvLikeCount.getText().toString());
                    tvLikeCount.setText(String.valueOf(currentLikes + 1));
                });
            }
        }

        private void handleShareClick(Video video) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this video!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, video.getVideoUri());
            context.startActivity(Intent.createChooser(shareIntent, "Share via"));

            if (user == null) return;

            DocumentReference shareRef = db.collection("shares")
                    .document(video.getVideoId())
                    .collection("user_shares")
                    .document(user.getUid());

            shareRef.get().addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    Map<String, Object> shareData = new HashMap<>();
                    shareData.put("timestamp", FieldValue.serverTimestamp());
                    
                    shareRef.set(shareData).addOnSuccessListener(aVoid -> {
                        db.collection("videos").document(video.getVideoId())
                            .update("totalShares", FieldValue.increment(1))
                            .addOnSuccessListener(v -> {
                                video.setTotalShares(video.getTotalShares() + 1);
                                if (tvShareCount != null) {
                                    tvShareCount.setText(String.valueOf(video.getTotalShares()));
                                }
                                
                                String aid = video.getAuthorId();
                                if (aid != null && !aid.isEmpty()) {
                                    db.collection("profiles").document(aid).collection("public_videos")
                                        .document(video.getVideoId()).update("totalShares", FieldValue.increment(1));
                                }
                            });
                    });
                }
            });
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
