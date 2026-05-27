package com.example.tiktokcloneproject.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.NotificationActivity;
import com.example.tiktokcloneproject.adapters.ConversationAdapter;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.ChatMessage;
import com.example.tiktokcloneproject.model.Conversation;
import com.example.tiktokcloneproject.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class InboxFragment extends Fragment {
    private Context context;
    private RecyclerView rvConversations;
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversationList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private View blankInbox;
    private TextView tvFollowerCount, tvActivityCount;
    private com.google.firebase.database.DatabaseReference mNotificationDatabase;
    private com.google.firebase.firestore.ListenerRegistration conversationListener;
    private com.google.firebase.database.ValueEventListener notificationListener;

    public static InboxFragment newInstance(String strArg) {
        InboxFragment fragment = new InboxFragment();
        Bundle args = new Bundle();
        args.putString("name", strArg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inbox, container, false);

        rvConversations = view.findViewById(R.id.rvConversations);
        blankInbox = view.findViewById(R.id.blank_inbox);
        tvFollowerCount = view.findViewById(R.id.tvFollowerCount);
        tvActivityCount = view.findViewById(R.id.tvActivityCount);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupRecyclerView();
        if (currentUser != null) {
            listenForConversations();
            setupNotificationListeners();
        }

        // Click listeners for Followers and Activities
        view.findViewById(R.id.rlFollowers).setOnClickListener(v -> {
            Intent intent = new Intent(context, NotificationActivity.class);
            intent.putExtra("mode", "follower");
            startActivity(intent);
        });
        view.findViewById(R.id.rlActivities).setOnClickListener(v -> {
            Intent intent = new Intent(context, NotificationActivity.class);
            intent.putExtra("mode", "activity");
            startActivity(intent);
        });

        return view;
    }

    private void setupRecyclerView() {
        conversationList = new ArrayList<>();
        conversationAdapter = new ConversationAdapter(conversationList, context);
        rvConversations.setLayoutManager(new LinearLayoutManager(context));
        rvConversations.setAdapter(conversationAdapter);
    }

    private void setupNotificationListeners() {
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child(currentUser.getUid());
        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int followerCount = 0;
                int activityCount = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Notification notification = ds.getValue(Notification.class);
                    if (notification != null) {
                        if (StaticVariable.FOLLOW.equals(notification.getAction())) {
                            followerCount++;
                        } else if (StaticVariable.LIKE.equals(notification.getAction()) ||
                                   StaticVariable.COMMENT.equals(notification.getAction())) {
                            activityCount++;
                        }
                    }
                }

                updateBadge(tvFollowerCount, followerCount);
                updateBadge(tvActivityCount, activityCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        mNotificationDatabase.addValueEventListener(notificationListener);
    }

    private void updateBadge(TextView tv, int count) {
        if (count > 0) {
            tv.setText(String.valueOf(count));
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private void listenForConversations() {
        conversationListener = db.collection("conversations")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("InboxFragment", "Error listening for conversations", error);
                        return;
                    }

                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            ChatMessage lastMsg = dc.getDocument().toObject(ChatMessage.class);
                            String chatId = dc.getDocument().getId();

                            if (chatId.contains(currentUser.getUid())) {
                                String[] ids = chatId.split("_");
                                if (ids.length < 2) continue;
                                String otherUserId = ids[0].equals(currentUser.getUid()) ? ids[1] : ids[0];
                                
                                switch (dc.getType()) {
                                    case ADDED:
                                    case MODIFIED:
                                        addOrUpdateConversation(chatId, lastMsg, otherUserId);
                                        break;
                                    case REMOVED:
                                        removeConversation(chatId);
                                        break;
                                }
                            }
                        }
                        
                        blankInbox.setVisibility(conversationList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (conversationListener != null) {
            conversationListener.remove();
        }
        if (mNotificationDatabase != null && notificationListener != null) {
            mNotificationDatabase.removeEventListener(notificationListener);
        }
    }

    private void addOrUpdateConversation(String chatId, ChatMessage lastMsg, String otherUserId) {
        db.collection("users").document(otherUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String avatarUrl = documentSnapshot.getString("avatarUrl");

                long timestamp = (lastMsg.getTimestamp() != null) ? lastMsg.getTimestamp().getTime() : System.currentTimeMillis();

                Conversation conversation = new Conversation(
                        chatId,
                        lastMsg.getMessage(),
                        timestamp,
                        otherUserId,
                        username,
                        avatarUrl
                );

                int index = -1;
                for (int i = 0; i < conversationList.size(); i++) {
                    if (conversationList.get(i).getChatId().equals(chatId)) {
                        index = i;
                        break;
                    }
                }

                if (index != -1) {
                    conversationList.set(index, conversation);
                } else {
                    conversationList.add(0, conversation);
                }
                
                conversationList.sort((c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));
                conversationAdapter.notifyDataSetChanged();
                blankInbox.setVisibility(View.GONE);
            }
        });
    }

    private void removeConversation(String chatId) {
        for (int i = 0; i < conversationList.size(); i++) {
            if (conversationList.get(i).getChatId().equals(chatId)) {
                conversationList.remove(i);
                conversationAdapter.notifyItemRemoved(i);
                break;
            }
        }
        if (conversationList.isEmpty()) {
            blankInbox.setVisibility(View.VISIBLE);
        }
    }
}
