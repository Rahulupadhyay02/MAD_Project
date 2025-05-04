package com.example.whereismysamaan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.whereismysamaan.adapter.MessageAdapter;
import com.example.whereismysamaan.firebase.FirebaseHelper;
import com.example.whereismysamaan.model.Message;

import java.util.List;

public class MessageActivity extends AppCompatActivity {
    private static final String TAG = "MessageActivity";
    
    // UI components
    private TextView tvTitle;
    private ImageView ivBack;
    private RecyclerView recyclerMessages;
    private View emptyView;
    private SwipeRefreshLayout swipeRefresh;
    
    // Adapter
    private MessageAdapter adapter;
    
    // Firebase helper
    private FirebaseHelper firebaseHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        
        // Set system UI behavior
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.message_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialize Firebase helper
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Initialize views
        initViews();
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Set click listeners
        setupClickListeners();
        
        // Load data
        loadMessages();
    }
    
    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        ivBack = findViewById(R.id.iv_back);
        recyclerMessages = findViewById(R.id.recycler_messages);
        emptyView = findViewById(R.id.empty_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        
        // Set title
        tvTitle.setText("Messages");
    }
    
    private void setupRecyclerView() {
        adapter = new MessageAdapter();
        adapter.setOnMessageClickListener(this::viewMessageDetails);
        
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(adapter);
    }
    
    private void setupClickListeners() {
        // Back button - finish activity
        ivBack.setOnClickListener(v -> finish());
        
        // Swipe refresh
        swipeRefresh.setOnRefreshListener(this::loadMessages);
    }
    
    private void loadMessages() {
        // Show loading indicator
        swipeRefresh.setRefreshing(true);
        
        firebaseHelper.getMessages(new FirebaseHelper.OnMessagesLoadedListener() {
            @Override
            public void onMessagesLoaded(List<Message> messages) {
                // Hide loading indicator
                swipeRefresh.setRefreshing(false);
                
                // Update adapter
                adapter.setMessageList(messages);
                
                // Update empty view visibility
                updateEmptyViewVisibility();
            }
            
            @Override
            public void onError(String error) {
                // Hide loading indicator
                swipeRefresh.setRefreshing(false);
                
                // Show error
                Toast.makeText(MessageActivity.this, "Error loading messages: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateEmptyViewVisibility() {
        if (adapter.getItemCount() == 0) {
            recyclerMessages.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerMessages.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
    
    private void viewMessageDetails(Message message) {
        // Mark the message as read
        firebaseHelper.markMessageAsRead(message.getId(), null);
        
        // Create intent for the message details activity
        Intent intent = new Intent(this, MessageDetailActivity.class);
        intent.putExtra(MessageDetailActivity.EXTRA_MESSAGE_ID, message.getId());
        intent.putExtra(MessageDetailActivity.EXTRA_MESSAGE_TITLE, message.getTitle());
        intent.putExtra(MessageDetailActivity.EXTRA_MESSAGE_CONTENT, message.getContent());
        intent.putExtra(MessageDetailActivity.EXTRA_SENDER_NAME, message.getSenderName());
        intent.putExtra(MessageDetailActivity.EXTRA_SAAMAN_NAME, message.getSaamanName());
        intent.putExtra(MessageDetailActivity.EXTRA_SAAMAN_IMAGE_URL, message.getSaamanImageUrl());
        intent.putExtra(MessageDetailActivity.EXTRA_LOCATION_NAME, message.getLocationName());
        intent.putExtra(MessageDetailActivity.EXTRA_SUBLOCATION_NAME, message.getSublocationName());
        
        startActivity(intent);
    }
} 