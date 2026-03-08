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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatRoomActivity extends AppCompatActivity {

    private ImageView btnBack, imgAvatar, btnCall, btnMore, btnAttach, btnSend;
    private TextView tvChatName, tvOnlineStatus;
    private EditText etMessage;
    private RecyclerView rvMessages;

    private com.example.carelink.adapters.MessageAdapter messageAdapter;    private List<Message> messageList;
    private DatabaseReference messagesRef, chatRef;
    private String chatId, chatName, chatType;
    private String currentUserId = "user1"; // Replace with actual user ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        chatId = getIntent().getStringExtra("CHAT_ID");
        chatName = getIntent().getStringExtra("CHAT_NAME");
        chatType = getIntent().getStringExtra("CHAT_TYPE");

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadMessages();
        listenForNewMessages();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        imgAvatar = findViewById(R.id.imgAvatar);
        btnCall = findViewById(R.id.btnCall);
        btnMore = findViewById(R.id.btnMore);
        tvChatName = findViewById(R.id.tvChatName);
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus);
        etMessage = findViewById(R.id.etMessage);
        btnAttach = findViewById(R.id.btnAttach);
        btnSend = findViewById(R.id.btnSend);
        rvMessages = findViewById(R.id.rvMessages);

        tvChatName.setText(chatName);
        tvOnlineStatus.setText("online");
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new com.example.carelink.adapters.MessageAdapter(messageList, currentUserId);        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCall.setOnClickListener(v -> {
            Toast.makeText(this, "Voice call coming soon", Toast.LENGTH_SHORT).show();
        });

        btnMore.setOnClickListener(v -> {
            Toast.makeText(this, "More options", Toast.LENGTH_SHORT).show();
        });

        btnAttach.setOnClickListener(v -> {
            Toast.makeText(this, "Attach file", Toast.LENGTH_SHORT).show();
        });

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        String messageId = FirebaseDatabase.getInstance().getReference().push().getKey();
        String timestamp = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", messageId);
        messageData.put("senderId", currentUserId);
        messageData.put("senderName", "Me");
        messageData.put("text", text);
        messageData.put("timestamp", timestamp);
        messageData.put("timestampLong", System.currentTimeMillis());
        messageData.put("type", "text");

        messagesRef.child(messageId).setValue(messageData)
                .addOnSuccessListener(aVoid -> {
                    etMessage.setText("");
                    // Update last message in chat list
                    updateLastMessage(text);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLastMessage(String text) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", text);
        updates.put("timestamp", System.currentTimeMillis());
        chatRef.updateChildren(updates);
    }

    private void loadMessages() {
        messagesRef = FirebaseDatabase.getInstance()
                .getReference("messages").child(chatId);
        chatRef = FirebaseDatabase.getInstance()
                .getReference("chats").child(chatId);

        // Sample messages for demo
        messageList.add(new Message("1", "user2", "Dr. Ahmad Abdullah",
                "Soft Reminder: Appointment today 10am.", "10:24 AM", false));
        messageList.add(new Message("2", "user1", "Me",
                "OK, noted.", "10:25 AM", true));
        messageList.add(new Message("3", "user2", "Dr. Ahmad Abdullah",
                "Better today?", "10:26 AM", false));

        messageAdapter.notifyDataSetChanged();
        rvMessages.scrollToPosition(messageList.size() - 1);
    }

    private void listenForNewMessages() {
        messagesRef.orderByChild("timestampLong").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                if (message != null && !isMessageExists(message.getId())) {
                    messageList.add(message);
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    rvMessages.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private boolean isMessageExists(String messageId) {
        for (Message m : messageList) {
            if (m.getId().equals(messageId)) return true;
        }
        return false;
    }

    // Message Model
    public static class Message {
        private String id, senderId, senderName, text, timestamp;
        private boolean isSent;
        private long timestampLong;

        public Message() {}

        public Message(String id, String senderId, String senderName,
                       String text, String timestamp, boolean isSent) {
            this.id = id;
            this.senderId = senderId;
            this.senderName = senderName;
            this.text = text;
            this.timestamp = timestamp;
            this.isSent = isSent;
        }

        public String getId() { return id; }
        public String getSenderId() { return senderId; }
        public String getSenderName() { return senderName; }
        public String getText() { return text; }
        public String getTimestamp() { return timestamp; }
        public boolean isSent() { return isSent; }
        public long getTimestampLong() { return timestampLong; }
    }
}