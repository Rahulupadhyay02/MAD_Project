package com.example.whereismysamaan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.whereismysamaan.firebase.FirebaseHelper;
import com.example.whereismysamaan.model.Message;

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
    public static final String EXTRA_IS_IMAGE_BASE64 = "is_image_base64";
    
    private static final String TAG = "MessageDetailActivity";
    
    // UI components
    private TextView tvTitle;
    private ImageView ivBack;
    private TextView tvSenderName;
    private TextView tvSaamanName;
    private TextView tvLocation;
    private ImageView ivSaamanImage;
    
    // Firebase helper
    private FirebaseHelper firebaseHelper;
    
    // Message data
    private String messageId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_detail);
        
        // Initialize Firebase helper
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Get message ID from intent
        messageId = getIntent().getStringExtra(EXTRA_MESSAGE_ID);
        if (messageId == null) {
            Toast.makeText(this, "Error: Message ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize views
        tvTitle = findViewById(R.id.tv_title);
        tvSenderName = findViewById(R.id.tv_sender_name);
        tvSaamanName = findViewById(R.id.tv_saaman_name);
        tvLocation = findViewById(R.id.tv_location);
        ivSaamanImage = findViewById(R.id.iv_saaman_image);
        ivBack = findViewById(R.id.iv_back);
        
        // Set system UI behavior
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.message_detail_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Set up back button
        ivBack.setOnClickListener(v -> finish());
        
        // Load message data
        loadMessageData();
    }
    
    private void loadMessageData() {
        try {
            // Get data from intent
            String messageTitle = getIntent().getStringExtra(EXTRA_MESSAGE_TITLE);
            String senderName = getIntent().getStringExtra(EXTRA_SENDER_NAME);
            String saamanName = getIntent().getStringExtra(EXTRA_SAAMAN_NAME);
            String locationName = getIntent().getStringExtra(EXTRA_LOCATION_NAME);
            String sublocationName = getIntent().getStringExtra(EXTRA_SUBLOCATION_NAME);
            boolean isImageBase64 = getIntent().getBooleanExtra(EXTRA_IS_IMAGE_BASE64, false);
            String saamanImageUrl = getIntent().getStringExtra(EXTRA_SAAMAN_IMAGE_URL);

            // Check if we're missing critical data
            boolean missingData = messageTitle == null || senderName == null || 
                                 saamanName == null || locationName == null || 
                                 sublocationName == null ||
                                 (isImageBase64 && (saamanImageUrl == null || saamanImageUrl.isEmpty()));
            
            if (missingData) {
                Log.w(TAG, "Missing some message data in intent, fetching entire message from Firebase");
                fetchEntireMessageFromFirebase();
                return;
            }
            
            // If we have all the data, display it
            displayMessageData(messageTitle, senderName, saamanName, locationName, sublocationName);
            
            // Handle image loading
            if (isImageBase64 && (saamanImageUrl == null || saamanImageUrl.isEmpty())) {
                // If we don't have the base64 image in the intent, fetch it from Firebase
                fetchImageFromFirebase();
            } else if (isImageBase64) {
                // If we have the base64 image in the intent, load it directly
                loadBase64Image(saamanImageUrl);
            } else if (saamanImageUrl != null && !saamanImageUrl.isEmpty()) {
                // For regular URL images, load directly
                loadImageFromUrl(saamanImageUrl);
            } else {
                // No image to load
                ivSaamanImage.setVisibility(View.GONE);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading data: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading message details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            // Try to fetch from Firebase as fallback
            fetchEntireMessageFromFirebase();
        }
    }
    
    private void displayMessageData(String messageTitle, String senderName, String saamanName, 
                                   String locationName, String sublocationName) {
        // Set the title
        tvTitle.setText(messageTitle);
        
        // Set sender name
        tvSenderName.setText("From: " + senderName);
        
        // Set saaman name
        tvSaamanName.setText(saamanName);
        
        // Set location
        tvLocation.setText("Location: " + locationName + " > " + sublocationName);
    }
    
    private void fetchEntireMessageFromFirebase() {
        Log.d(TAG, "Fetching entire message data from Firebase for ID: " + messageId);
        
        // Show a loading toast
        Toast.makeText(this, "Loading message data...", Toast.LENGTH_SHORT).show();
        
        // Show loading indicator for image
        ivSaamanImage.setVisibility(View.VISIBLE);
        ivSaamanImage.setImageResource(R.drawable.ic_loading);
        
        // Use the FirebaseHelper to get the message by ID
        firebaseHelper.getMessageById(messageId, new FirebaseHelper.OnMessageLoadedListener() {
            @Override
            public void onMessageLoaded(Message message) {
                if (message != null) {
                    Log.d(TAG, "Successfully loaded message data from Firebase");
                    
                    // Display the message data
                    displayMessageData(
                        message.getTitle(),
                        message.getSenderName(),
                        message.getSaamanName(),
                        message.getLocationName(),
                        message.getSublocationName()
                    );
                    
                    // Load image if available
                    if (message.isImageBase64() && message.getSaamanImageUrl() != null && 
                        !message.getSaamanImageUrl().isEmpty()) {
                        loadBase64Image(message.getSaamanImageUrl());
                    } else if (message.getSaamanImageUrl() != null && !message.getSaamanImageUrl().isEmpty()) {
                        loadImageFromUrl(message.getSaamanImageUrl());
                    } else {
                        ivSaamanImage.setVisibility(View.GONE);
                    }
                } else {
                    Log.e(TAG, "Message not found in Firebase");
                    Toast.makeText(MessageDetailActivity.this, 
                            "Error: Message not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching message: " + error);
                Toast.makeText(MessageDetailActivity.this, 
                        "Error loading message: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    private void fetchImageFromFirebase() {
        Log.d(TAG, "Fetching image from Firebase for message ID: " + messageId);
        
        // Show loading indicator
        ivSaamanImage.setVisibility(View.VISIBLE);
        ivSaamanImage.setImageResource(R.drawable.ic_loading);
        
        // Use FirebaseHelper to get the message by ID
        firebaseHelper.getMessageById(messageId, new FirebaseHelper.OnMessageLoadedListener() {
            @Override
            public void onMessageLoaded(Message message) {
                if (message != null && message.getSaamanImageUrl() != null && 
                    !message.getSaamanImageUrl().isEmpty() && message.isImageBase64()) {
                    // Load the base64 image
                    loadBase64Image(message.getSaamanImageUrl());
                } else {
                    Log.e(TAG, "Message or image data not found");
                    ivSaamanImage.setVisibility(View.GONE);
                    Toast.makeText(MessageDetailActivity.this, 
                            "Image not available", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching image: " + error);
                ivSaamanImage.setVisibility(View.GONE);
                Toast.makeText(MessageDetailActivity.this, 
                        "Error loading image: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadBase64Image(String base64Image) {
        // Show loading state
        ivSaamanImage.setVisibility(View.VISIBLE);
        ivSaamanImage.setImageResource(R.drawable.ic_loading);
        
        new Thread(() -> {
            try {
                if (base64Image == null || base64Image.isEmpty()) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Base64 image string is null or empty");
                        ivSaamanImage.setVisibility(View.GONE);
                        Toast.makeText(MessageDetailActivity.this, "Image data is missing", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                Log.d(TAG, "Decoding base64 image, length: " + base64Image.length());
                
                // Decode in background thread
                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    if (bitmap != null) {
                        ivSaamanImage.setImageBitmap(bitmap);
                        ivSaamanImage.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Base64 image loaded successfully");
                    } else {
                        Log.e(TAG, "Failed to decode base64 image");
                        ivSaamanImage.setVisibility(View.GONE);
                        Toast.makeText(MessageDetailActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error decoding base64 image: " + e.getMessage(), e);
                    ivSaamanImage.setVisibility(View.GONE);
                    Toast.makeText(MessageDetailActivity.this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void loadImageFromUrl(String imageUrl) {
        Log.d(TAG, "Loading image from URL: " + imageUrl);
        
        // Check if this is actually a URL and not a base64 string
        if (imageUrl != null && imageUrl.length() > 500) {  // Base64 strings are typically very long
            Log.w(TAG, "This doesn't look like a URL (too long). Trying to load as base64 instead.");
            loadBase64Image(imageUrl);
            return;
        }
        
        // Show loading indicator
        ivSaamanImage.setVisibility(View.VISIBLE);
        ivSaamanImage.setImageResource(R.drawable.ic_loading);
        
        Glide.with(this)
            .load(imageUrl)
            .centerCrop()
            .error(R.drawable.ic_loading) // Use existing ic_loading drawable instead of non-existent ic_error
            .into(ivSaamanImage);
    }
} 