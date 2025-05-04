package com.example.whereismysamaan;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

public class MessageDetailActivity extends AppCompatActivity {
    // Constants for intent extras
    public static final String EXTRA_MESSAGE_ID = "message_id";
    public static final String EXTRA_MESSAGE_TITLE = "message_title";
    public static final String EXTRA_MESSAGE_CONTENT = "message_content";
    public static final String EXTRA_SENDER_NAME = "sender_name";
    public static final String EXTRA_SAAMAN_NAME = "saaman_name";
    public static final String EXTRA_SAAMAN_IMAGE_URL = "saaman_image_url";
    public static final String EXTRA_LOCATION_NAME = "location_name";
    public static final String EXTRA_SUBLOCATION_NAME = "sublocation_name";
    
    // UI components
    private TextView tvTitle;
    private ImageView ivBack;
    private TextView tvSenderName;
    private TextView tvSaamanName;
    private TextView tvLocation;
    private ImageView ivSaamanImage;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_detail);
        
        // Set system UI behavior
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.message_detail_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialize views
        initViews();
        
        // Set click listeners
        setupClickListeners();
        
        // Load data from intent
        loadData();
    }
    
    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        ivBack = findViewById(R.id.iv_back);
        tvSenderName = findViewById(R.id.tv_sender_name);
        tvSaamanName = findViewById(R.id.tv_saaman_name);
        tvLocation = findViewById(R.id.tv_location);
        ivSaamanImage = findViewById(R.id.iv_saaman_image);
    }
    
    private void setupClickListeners() {
        // Back button - finish activity
        ivBack.setOnClickListener(v -> finish());
    }
    
    private void loadData() {
        // Get data from intent
        String messageTitle = getIntent().getStringExtra(EXTRA_MESSAGE_TITLE);
        String senderName = getIntent().getStringExtra(EXTRA_SENDER_NAME);
        String saamanName = getIntent().getStringExtra(EXTRA_SAAMAN_NAME);
        String saamanImageUrl = getIntent().getStringExtra(EXTRA_SAAMAN_IMAGE_URL);
        String locationName = getIntent().getStringExtra(EXTRA_LOCATION_NAME);
        String sublocationName = getIntent().getStringExtra(EXTRA_SUBLOCATION_NAME);
        
        // Set the title
        tvTitle.setText(messageTitle);
        
        // Set sender name
        tvSenderName.setText("From: " + senderName);
        
        // Set saaman name
        tvSaamanName.setText(saamanName);
        
        // Set location
        tvLocation.setText("Location: " + locationName + " > " + sublocationName);
        
        // Load saaman image if available
        if (saamanImageUrl != null && !saamanImageUrl.isEmpty()) {
            ivSaamanImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                .load(saamanImageUrl)
                .centerCrop()
                .into(ivSaamanImage);
        } else {
            ivSaamanImage.setVisibility(View.GONE);
        }
    }
} 