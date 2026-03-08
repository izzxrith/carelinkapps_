package com.example.carelink.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carelink.MessageActivity;
import com.example.carelink.R;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private List<MessageActivity.ChatItem> chats;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(MessageActivity.ChatItem chat);
    }

    public ChatListAdapter(List<MessageActivity.ChatItem> chats, OnChatClickListener listener) {
        this.chats = chats;
        this.listener = listener;
    }

    public void updateList(List<MessageActivity.ChatItem> newList) {
        this.chats = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageActivity.ChatItem chat = chats.get(position);
        holder.tvName.setText(chat.getName());
        holder.tvLastMessage.setText(chat.getLastMessage());
        holder.tvTime.setText(chat.getTime());
        holder.tvStatus.setText(chat.getStatus());

        if (chat.getUnreadCount() > 0) {
            holder.tvUnreadCount.setText(String.valueOf(chat.getUnreadCount()));
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        holder.onlineIndicator.setVisibility(chat.isOnline() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    @Override
    public int getItemCount() {
        return chats != null ? chats.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        View onlineIndicator;
        TextView tvName, tvLastMessage, tvTime, tvStatus, tvUnreadCount;

        ViewHolder(View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);  // Now matches
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }
    }
}