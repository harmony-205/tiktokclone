package com.example.tiktokcloneproject.fragment;

import android.content.Context;
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
import com.example.tiktokcloneproject.adapters.ConversationAdapter;
import com.example.tiktokcloneproject.model.ChatMessage;
import com.example.tiktokcloneproject.model.Conversation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
        
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupRecyclerView();
        if (currentUser != null) {
            listenForConversations();
        }

        // Click listeners for Followers and Activities (Placeholders)
        view.findViewById(R.id.rlFollowers).setOnClickListener(v -> {
            // Open Followers Activity
        });
        view.findViewById(R.id.rlActivities).setOnClickListener(v -> {
            // Open Activities Activity
        });

        return view;
    }

    private void setupRecyclerView() {
        conversationList = new ArrayList<>();
        conversationAdapter = new ConversationAdapter(conversationList, context);
        rvConversations.setLayoutManager(new LinearLayoutManager(context));
        rvConversations.setAdapter(conversationAdapter);
    }

    private void listenForConversations() {
        // We look for conversations where the current user is part of the chatId
        // chatId format is usually id1_id2
        db.collection("conversations")
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
                                String otherUserId = chatId.replace(currentUser.getUid(), "").replace("_", "");
                                
                                switch (dc.getType()) {
                                    case ADDED:
                                        addOrUpdateConversation(chatId, lastMsg, otherUserId);
                                        break;
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

    private void addOrUpdateConversation(String chatId, ChatMessage lastMsg, String otherUserId) {
        // Fetch other user info (username, avatar)
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
                    conversationAdapter.notifyItemChanged(index);
                } else {
                    conversationList.add(0, conversation);
                    conversationAdapter.notifyItemInserted(0);
                }
                
                // Sort list by timestamp
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
