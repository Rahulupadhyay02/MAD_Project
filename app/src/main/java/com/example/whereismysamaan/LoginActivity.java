package com.example.whereismysamaan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.whereismysamaan.firebase.FirebaseHelper;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    // UI Components
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private Button btnLogin;
    private Button btnSignUp;
    private TextView tvToggleAuth;
    
    // Firebase helper
    private FirebaseHelper firebaseHelper;
    
    // Auth mode
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Set system UI behavior
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase helper
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Check if user is already logged in
        if (firebaseHelper.isUserLoggedIn()) {
            startMainActivity();
            return;
        }

        // Initialize views
        initViews();
        
        // Set click listeners
        setupClickListeners();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        btnLogin = findViewById(R.id.btn_login);
        btnSignUp = findViewById(R.id.btn_signup);
        tvToggleAuth = findViewById(R.id.tv_toggle_auth);
        
        updateAuthMode();
    }

    private void setupClickListeners() {
        // Login button click
        btnLogin.setOnClickListener(v -> handleAuth());
        
        // Sign up button click
        btnSignUp.setOnClickListener(v -> handleAuth());
        
        // Toggle auth mode text click
        tvToggleAuth.setOnClickListener(v -> toggleAuthMode());
    }

    private void toggleAuthMode() {
        isLoginMode = !isLoginMode;
        updateAuthMode();
    }

    private void updateAuthMode() {
        if (isLoginMode) {
            btnLogin.setVisibility(View.VISIBLE);
            btnSignUp.setVisibility(View.GONE);
            tvToggleAuth.setText(R.string.create_account);
        } else {
            btnLogin.setVisibility(View.GONE);
            btnSignUp.setVisibility(View.VISIBLE);
            tvToggleAuth.setText(R.string.already_have_account);
        }
    }

    private void handleAuth() {
        String email = tilEmail.getEditText().getText().toString().trim();
        String password = tilPassword.getEditText().getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLoginMode) {
            firebaseHelper.signIn(email, password, task -> {
                if (task.isSuccessful()) {
                    startMainActivity();
                } else {
                    String errorMessage = task.getException() != null ? 
                        task.getException().getMessage() : "Login failed";
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            firebaseHelper.signUp(email, password, task -> {
                if (task.isSuccessful()) {
                    startMainActivity();
                } else {
                    String errorMessage = task.getException() != null ? 
                        task.getException().getMessage() : "Sign up failed";
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
} 