package com.example.tiktokcloneproject.adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.model.ChatMessage;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private final List<ChatMessage> messages;
    private final String currentUserId;

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    public ChatAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).getSenderId().equals(currentUserId)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.tvMessage.setText(message.getMessage());
        
        if (message.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(message.getTimestamp()));
        }

        // Căn lề trái/phải tùy theo người gửi
        if (getItemViewType(position) == TYPE_SENT) {
            holder.llContainer.setGravity(Gravity.END);
            holder.llBubble.setBackgroundResource(R.drawable.btn_red_shape);
            holder.tvMessage.setTextColor(android.graphics.Color.WHITE);
            holder.tvTime.setTextColor(android.graphics.Color.parseColor("#E0E0E0"));
        } else {
            holder.llContainer.setGravity(Gravity.START);
            holder.llBubble.setBackgroundResource(R.drawable.btn_normal_shape);
            
            // Kiểm tra Dark Mode để đặt màu chữ phù hợp (Trắng cho nền tối, Đen cho nền sáng)
            boolean isDarkMode = (holder.itemView.getContext().getResources().getConfiguration().uiMode & 
                                 android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                                 android.content.res.Configuration.UI_MODE_NIGHT_YES;
            
            if (isDarkMode) {
                holder.tvMessage.setTextColor(android.graphics.Color.WHITE);
                holder.tvTime.setTextColor(android.graphics.Color.parseColor("#CCCCCC"));
            } else {
                holder.tvMessage.setTextColor(android.graphics.Color.BLACK);
                holder.tvTime.setTextColor(android.graphics.Color.DKGRAY);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        LinearLayout llBubble, llContainer;

        ChatViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            llBubble = itemView.findViewById(R.id.llMessageBubble);
            llContainer = itemView.findViewById(R.id.llMessageContainer);
        }
    }
}
