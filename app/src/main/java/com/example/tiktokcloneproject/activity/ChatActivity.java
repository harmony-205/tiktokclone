package com.example.tiktokcloneproject.activity;

import android.os.Bundle;
import android.text.TextUtils;
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
import java.util.List;

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

        tvReceiverName.setText(receiverName);
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

        ChatMessage message = new ChatMessage(senderId, receiverId, text);
        String chatId = getChatId(senderId, receiverId);

        db.collection("chats").document(chatId).collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    edtMessage.setText("");
                    updateLastMessage(chatId, message);
                    
                    // Đẩy thông báo vào Inbox của người nhận
                    if (senderUsername != null) {
                        Notification.pushNotification(senderUsername, receiverId, "sent you a message: " + text);
                    }
                });
    }

    private String getChatId(String id1, String id2) {
        if (id1.compareTo(id2) < 0) return id1 + "_" + id2;
        else return id2 + "_" + id1;
    }

    private void updateLastMessage(String chatId, ChatMessage message) {
        db.collection("conversations").document(chatId).set(message);
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
