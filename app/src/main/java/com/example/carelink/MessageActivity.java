package com.example.carelink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carelink.adapters.ChatListAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MessageActivity extends AppCompatActivity {

    private ImageView btnSearch;
    private MaterialButton btnAll, btnGroup, btnPrivate;
    private RecyclerView rvChatList;
    private FloatingActionButton fabNewChat;
    private LinearLayout navHome, navMessages, navSchedule, navProfile;

    private ChatListAdapter chatAdapter;
    private List<ChatItem> chatList;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        initViews();
        setupFilterButtons();
        setupRecyclerView();
        setupClickListeners();
        setupBottomNavigation();
        loadChats();
    }

    private void initViews() {
        btnSearch = findViewById(R.id.btnSearch);
        btnAll = findViewById(R.id.btnAll);
        btnGroup = findViewById(R.id.btnGroup);
        btnPrivate = findViewById(R.id.btnPrivate);
        rvChatList = findViewById(R.id.rvChatList);
        fabNewChat = findViewById(R.id.fabNewChat);

        navHome = findViewById(R.id.navHome);
        navMessages = findViewById(R.id.navMessages);
        navSchedule = findViewById(R.id.navSchedule);
        navProfile = findViewById(R.id.navProfile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupFilterButtons() {
        btnAll.setOnClickListener(v -> selectFilter("all", btnAll));
        btnGroup.setOnClickListener(v -> selectFilter("group", btnGroup));
        btnPrivate.setOnClickListener(v -> selectFilter("private", btnPrivate));
    }

    private void selectFilter(String filter, MaterialButton selectedBtn) {
        currentFilter = filter;

        // Reset all buttons
        resetButton(btnAll);
        resetButton(btnGroup);
        resetButton(btnPrivate);

        // Highlight selected
        selectedBtn.setBackgroundTintList(getColorStateList(R.color.green_primary));
        selectedBtn.setTextColor(getColor(R.color.white));

        filterChats();
    }

    private void resetButton(MaterialButton btn) {
        btn.setBackgroundTintList(getColorStateList(R.color.gray_light));
        btn.setTextColor(getColor(R.color.gray));
    }

    private void setupRecyclerView() {
        chatList = new ArrayList<>();
        chatAdapter = new ChatListAdapter(chatList, this::onChatClick);
        rvChatList.setLayoutManager(new LinearLayoutManager(this));
        rvChatList.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        btnSearch.setOnClickListener(v -> {
            Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show();
        });

        fabNewChat.setOnClickListener(v -> {
            Toast.makeText(this, "New chat coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        navMessages.setOnClickListener(v -> {
            Toast.makeText(this, "Already on Messages", Toast.LENGTH_SHORT).show();
        });

        navSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void loadChats() {
        // Sample data only (no Firebase)
        chatList.add(new ChatItem("1", "Dr. Sarah", "Take your medicine, OK?...",
                "10:24", "Just now", 2, true, "private"));
        chatList.add(new ChatItem("2", "Dr. Rajesh Kumar", "Don't forget appointment...",
                "09:14", "1 hour ago", 0, true, "private"));
        chatList.add(new ChatItem("3", "Dr. Lim Mei Hua", "Feeling better today",
                "08:57", "2 hours ago", 1, false, "private"));
        chatList.add(new ChatItem("4", "Dr. Ahmad Abdullah", "Soft Reminder: Appointment today 10am.",
                "10:25", "Just now", 5, true, "group"));

        chatAdapter.notifyDataSetChanged();
    }

    private void filterChats() {
        List<ChatItem> filtered = new ArrayList<>();
        for (ChatItem chat : chatList) {
            if (currentFilter.equals("all") || chat.getType().equals(currentFilter)) {
                filtered.add(chat);
            }
        }
        chatAdapter.updateList(filtered);
    }

    private void onChatClick(ChatItem chat) {
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("CHAT_ID", chat.getId());
        intent.putExtra("CHAT_NAME", chat.getName());
        intent.putExtra("CHAT_TYPE", chat.getType());
        startActivity(intent);
    }

    // Chat Item Model
    public static class ChatItem {
        private String id, name, lastMessage, time, status, type;
        private int unreadCount;
        private boolean isOnline;

        public ChatItem(String id, String name, String lastMessage, String time,
                        String status, int unreadCount, boolean isOnline, String type) {
            this.id = id;
            this.name = name;
            this.lastMessage = lastMessage;
            this.time = time;
            this.status = status;
            this.unreadCount = unreadCount;
            this.isOnline = isOnline;
            this.type = type;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getLastMessage() { return lastMessage; }
        public String getTime() { return time; }
        public String getStatus() { return status; }
        public String getType() { return type; }
        public int getUnreadCount() { return unreadCount; }
        public boolean isOnline() { return isOnline; }
    }
}