package com.example.tiktokcloneproject.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.ChatAdapter;
import com.example.tiktokcloneproject.model.ChatMessage;
import com.example.tiktokcloneproject.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private String receiverId, receiverName;
    private String senderId, senderUsername;
    private EditText edtMessage;
    private ImageView btnSend, btnBack;
    private TextView tvReceiverName;
    private RecyclerView rvChat;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        receiverId = getIntent().getStringExtra("receiverId");
        receiverName = getIntent().getStringExtra("receiverName");
        senderId = FirebaseAuth.getInstance().getUid();
        db = FirebaseFirestore.getInstance();

        // Lấy tên của chính mình để gửi thông báo cho đối phương
        db.collection("users").document(senderId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                senderUsername = documentSnapshot.getString("username");
            }
        });

        initViews();
        setupRecyclerView();
        listenMessages();
    }

    private void initViews() {
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        tvReceiverName = findViewById(R.id.tvReceiverName);
        rvChat = findViewById(R.id.rvChat);

        if (receiverName != null) {
            tvReceiverName.setText(receiverName);
        } else {
            // Lấy tên từ Firestore nếu bundle không có
            db.collection("users").document(receiverId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    tvReceiverName.setText(documentSnapshot.getString("username"));
                }
            });
        }

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, senderId);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);
    }

    private void sendMessage() {
        String text = edtMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        edtMessage.setText("");

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", senderId);
        messageData.put("receiverId", receiverId);
        messageData.put("message", text);
        messageData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        String chatId = getChatId(senderId, receiverId);

        db.collection("chats").document(chatId).collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> {
                    updateLastMessage(chatId, text);
                    if (senderUsername != null) {
                        Notification.pushNotification(senderUsername, receiverId, "sent you a message: " + text);
                    }
                })
                .addOnFailureListener(e -> {
                    edtMessage.setText(text);
                    Toast.makeText(ChatActivity.this, "Lỗi Gửi Tin Nhắn: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("CHAT_ERROR", "Failed to add message", e);
                });
    }

    private String getChatId(String id1, String id2) {
        if (id1 == null || id2 == null) return "unknown_chat";
        if (id1.compareTo(id2) < 0) return id1 + "_" + id2;
        else return id2 + "_" + id1;
    }

    private void updateLastMessage(String chatId, String lastMessage) {
        Map<String, Object> convData = new HashMap<>();
        convData.put("senderId", senderId);
        convData.put("receiverId", receiverId);
        convData.put("message", lastMessage);
        convData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("conversations").document(chatId).set(convData, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener(e -> {
                Log.e("CHAT_ERROR", "Failed to update conversation", e);
                Toast.makeText(ChatActivity.this, "Lỗi cập nhật Inbox: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void listenMessages() {
        String chatId = getChatId(senderId, receiverId);
        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                messageList.add(dc.getDocument().toObject(ChatMessage.class));
                                chatAdapter.notifyItemInserted(messageList.size() - 1);
                                rvChat.scrollToPosition(messageList.size() - 1);
                            }
                        }
                    }
                });
    }
}
