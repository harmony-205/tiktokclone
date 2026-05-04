package com.example.tiktokcloneproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class FollowingAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater inflater;
    private ArrayList<String> followingIdList;
    private ArrayList<String> followingUserNameList;

    public FollowingAdapter(Context context, ArrayList<String> followingIdList, ArrayList<String> followingUserNameList) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.followingIdList = followingIdList;
        this.followingUserNameList = followingUserNameList;
    }

    @Override
    public int getCount() {
        return followingIdList.size();
    }

    @Override
    public Object getItem(int i) {
        return followingIdList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(R.layout.layout_following_item, viewGroup, false);
        }

        String followingId = followingIdList.get(i);
        ImageView imvAvatar = view.findViewById(R.id.imv_following_avatar);
        TextView tvUserName = view.findViewById(R.id.tv_following_userMame);

        tvUserName.setText(followingUserNameList.get(i));
        imvAvatar.setImageResource(R.drawable.default_avatar);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Load avatar using avatarUrl from user document
        db.collection("users").document(followingId).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String avatarUrl = document.getString("avatarUrl");
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(context)
                         .load(avatarUrl)
                         .placeholder(R.drawable.default_avatar)
                         .circleCrop()
                         .into(imvAvatar);
                }
            }
        });

        return view;
    }
}
