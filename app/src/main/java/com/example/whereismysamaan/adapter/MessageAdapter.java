package com.example.whereismysamaan.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.whereismysamaan.R;
import com.example.whereismysamaan.model.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;
    private OnMessageClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public MessageAdapter() {
        this.messageList = new ArrayList<>();
    }

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList != null ? messageList : new ArrayList<>();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void setMessageList(List<Message> messageList) {
        this.messageList = messageList != null ? messageList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.listener = listener;
    }

    public interface OnMessageClickListener {
        void onMessageClick(Message message);
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivMessageIcon;
        private TextView tvMessageTitle;
        private TextView tvMessageContent;
        private TextView tvMessageSender;
        private View viewUnreadIndicator;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMessageIcon = itemView.findViewById(R.id.iv_message_icon);
            tvMessageTitle = itemView.findViewById(R.id.tv_message_title);
            tvMessageContent = itemView.findViewById(R.id.tv_message_content);
            tvMessageSender = itemView.findViewById(R.id.tv_message_sender);
            viewUnreadIndicator = itemView.findViewById(R.id.view_unread_indicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageClick(messageList.get(position));
                }
            });
        }

        public void bind(Message message) {
            // Set message title
            tvMessageTitle.setText(message.getTitle());
            
            // Set message content
            tvMessageContent.setText(message.getContent());
            
            // Set sender info
            tvMessageSender.setText("From: " + message.getSenderName());
            
            // Set read status indicator
            viewUnreadIndicator.setVisibility(message.isRead() ? View.INVISIBLE : View.VISIBLE);
        }
    }
} 