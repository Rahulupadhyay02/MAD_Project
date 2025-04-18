package com.example.whereismysamaan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "Starting SettingsActivity onCreate");
            
            // Set a simple layout to test if the issue is with the layout file
            setContentView(R.layout.activity_settings);
            
            // Add a back button click listener if it exists
            View backButton = findViewById(R.id.btn_back);
            if (backButton != null) {
                backButton.setOnClickListener(v -> finish());
            }
            
            Toast.makeText(this, "Settings loaded successfully", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            e.printStackTrace(); // Print full stack trace
            Toast.makeText(this, "Error initializing settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
} 