package com.example.tiktokcloneproject.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.VideoActivity;
import com.example.tiktokcloneproject.model.VideoSummary;

import java.util.ArrayList;

public class VideoSummaryAdapter extends RecyclerView.Adapter<VideoSummaryAdapter.ViewHolder> {

    private ArrayList<VideoSummary> mData;
    private LayoutInflater mInflater;
    private Context mainContext;

    public VideoSummaryAdapter(Context context, ArrayList<VideoSummary> data) {
        this.mainContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.video_summary_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoSummary video = mData.get(position);
        holder.viewCount.setText(video.getWatchCount().toString());
        
        // SỬA LỖI VĂNG APP: Sử dụng Glide để tải ảnh từ URL (Cloudinary hoặc Firebase)
        Glide.with(mainContext)
                .load(video.getThumbnailUri())
                .placeholder(R.drawable.splash_background) // Ảnh hiển thị trong lúc chờ
                .centerCrop()
                .into(holder.thumbnail);

        holder.setOnItemClickListener(new ItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(view.getContext(), VideoActivity.class);
                Bundle bundle =  new Bundle();
                bundle.putString("videoId", video.getVideoId());
                intent.putExtras(bundle);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                view.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView viewCount;
        ImageView thumbnail;
        private ItemClickListener itemClickListener;

        ViewHolder(View itemView) {
            super(itemView);
            viewCount = itemView.findViewById(R.id.view_count);
            thumbnail = itemView.findViewById(R.id.image_thumbnail);
            itemView.setOnClickListener(this);
        }

        void setOnItemClickListener(ItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(view, getBindingAdapterPosition());
            }
        }
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
