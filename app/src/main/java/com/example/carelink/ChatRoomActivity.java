package com.example.carelink;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carelink.adapters.MessageAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatRoomActivity extends AppCompatActivity {

    private ImageView btnBack, btnSend;
    private TextView tvChatName;
    private EditText etMessage;
    private RecyclerView rvMessages;

    private MessageAdapter messageAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private DatabaseReference messagesRef;
    
    private String chatId, chatName;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        // --- SK PINJI: REAL SESSION FIX ---
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            currentUserId = "demo_user";
        }

        chatId = getIntent().getStringExtra("CHAT_ID");
        chatName = getIntent().getStringExtra("CHAT_NAME");
        if (chatId == null) chatId = "default_chat";

        // Initialize DB reference immediately
        messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        listenForMessages();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvChatName = findViewById(R.id.tvChatName);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        rvMessages = findViewById(R.id.rvMessages);

        tvChatName.setText(chatName != null ? chatName : "Support Chat");
    }

    private void setupRecyclerView() {
        // Using our standardized ChatMessage list
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        String msgId = messagesRef.push().getKey();
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("id", msgId);
        msgMap.put("senderId", currentUserId);
        msgMap.put("text", text);
        msgMap.put("timestamp", time);
        msgMap.put("timestampLong", System.currentTimeMillis());

        if (msgId != null) {
            messagesRef.child(msgId).setValue(msgMap)
                .addOnSuccessListener(aVoid -> etMessage.setText(""))
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show());
        }
    }

    private void listenForMessages() {
        messagesRef.orderByChild("timestampLong").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null && !isDuplicate(message.getId())) {
                    messageList.add(message);
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    rvMessages.scrollToPosition(messageList.size() - 1);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private boolean isDuplicate(String id) {
        for (ChatMessage m : messageList) {
            if (m.getId().equals(id)) return true;
        }
        return false;
    }

    // Standardized Message Model
    public static class ChatMessage {
        private String id, senderId, text, timestamp;
        private long timestampLong;

        public ChatMessage() {}
        public String getId() { return id; }
        public String getSenderId() { return senderId; }
        public String getText() { return text; }
        public String getTimestamp() { return timestamp; }
        public long getTimestampLong() { return timestampLong; }
    }
}