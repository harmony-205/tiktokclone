package com.example.tiktokcloneproject.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Notification;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class NotificationAdapter extends ArrayAdapter<Notification> {

    private ArrayList<Notification> notifications;
    private Context context;

    public NotificationAdapter(@NonNull Context context, int resource, ArrayList<Notification> notifications) {
        super(context, resource, notifications);
        this.notifications = notifications;
        this.context = context;
    }

    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.notification_row, parent, false);
        }

        Notification notification = notifications.get(position);

        CircleImageView ivIcon = convertView.findViewById(R.id.ivNotificationIcon);
        TextView username = convertView.findViewById(R.id.txvUsername);
        TextView content = convertView.findViewById(R.id.txvContent);
        TextView time = convertView.findViewById(R.id.txvTime);

        username.setText("@" + notification.getFromUsername());
        time.setText(handleTime(notification.getTimestamp()));
        
        // Thiết lập nội dung và Icon dựa trên hành động
        setupActionUI(notification.getAction(), content, ivIcon);

        return convertView;
    }

    private void setupActionUI(String action, TextView tvContent, CircleImageView ivIcon) {
        switch (action) {
            case StaticVariable.FOLLOW:
                tvContent.setText("bắt đầu follow bạn.");
                ivIcon.setImageResource(R.drawable.ic_follow_notification);
                ivIcon.setCircleBackgroundColor(Color.parseColor("#00C2FF"));
                break;
            case StaticVariable.LIKE:
                tvContent.setText("đã thích video của bạn.");
                ivIcon.setImageResource(R.drawable.ic_activity_notification);
                ivIcon.setCircleBackgroundColor(Color.parseColor("#FF2C55"));
                break;
            case StaticVariable.COMMENT:
                tvContent.setText("đã bình luận về video của bạn.");
                ivIcon.setImageResource(R.drawable.ic_activity_notification);
                ivIcon.setCircleBackgroundColor(Color.parseColor("#FF2C55"));
                break;
        }
    }

    private String handleTime(long timeMs) {
        long diff = System.currentTimeMillis() - timeMs;
        long minutes = diff / (1000 * 60);
        long hours = diff / (1000 * 60 * 60);
        long days = diff / (1000 * 60 * 60 * 24);

        if (minutes < 1) return "vừa xong";
        if (minutes < 60) return minutes + "ph";
        if (hours < 24) return hours + "g";
        return days + "n";
    }
}
