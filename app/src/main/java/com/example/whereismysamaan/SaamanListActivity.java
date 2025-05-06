package com.example.whereismysamaan;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.whereismysamaan.adapter.SaamanAdapter;
import com.example.whereismysamaan.firebase.FirebaseHelper;
import com.example.whereismysamaan.model.Saaman;
import com.example.whereismysamaan.model.Message;
import com.example.whereismysamaan.model.User;
import com.example.whereismysamaan.adapter.UserAdapter;
import com.example.whereismysamaan.util.ImageCompressor;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.util.Base64;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import android.graphics.drawable.Drawable;

public class SaamanListActivity extends AppCompatActivity implements SaamanAdapter.OnSaamanClickListener {
    private static final String TAG = "SaamanListActivity";

    // Intent extras
    public static final String EXTRA_SUBLOCATION_ID = "sublocation_id";
    public static final String EXTRA_SUBLOCATION_NAME = "sublocation_name";
    public static final String EXTRA_LOCATION_ID = "location_id";

    // UI components
    private TextView tvSublocationTitle;
    private ImageView ivBack;
    private RecyclerView recyclerSaaman;
    private FloatingActionButton fabAddSaaman;
    private View emptyView;

    // Adapter
    private SaamanAdapter adapter;

    // Firebase helper
    private FirebaseHelper firebaseHelper;

    // Data
    private String sublocationId;
    private String sublocationName;
    private String locationId;
    
    // Camera & Image variables
    private Uri currentPhotoUri;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ImageView ivSaamanPreview;
    private TextView tvNoImage;
    private AlertDialog saamanDialog;
    private AlertDialog userSelectionDialog;

    // Add permission constants
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saaman_list);

        // Set system UI behavior
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.saaman_list_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get data from intent
        sublocationId = getIntent().getStringExtra(EXTRA_SUBLOCATION_ID);
        sublocationName = getIntent().getStringExtra(EXTRA_SUBLOCATION_NAME);
        locationId = getIntent().getStringExtra(EXTRA_LOCATION_ID);

        if (sublocationName == null) {
            sublocationName = "Sublocation";
        }

        // Initialize Firebase helper
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Check Firebase Storage permissions early to diagnose potential upload issues
        firebaseHelper.checkStorageRules();
        
        // Initialize permission launcher
        initPermissionLauncher();
        
        // Initialize camera result launcher
        initCameraLauncher();
        
        // Check storage permissions
        checkStoragePermissions();

        // Initialize views
        initViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Set click listeners
        setupClickListeners();

        // Load data
        loadSaamanItems();
    }
    
    private void initPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Camera permission granted");
                    takePicture();
                } else {
                    Log.e(TAG, "Camera permission denied");
                    Toast.makeText(this, "Camera permission is required to take pictures", Toast.LENGTH_LONG).show();
                }
            }
        );
    }

    private void initCameraLauncher() {
        takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Camera result received: resultCode=" + result.getResultCode());
                
                if (result.getResultCode() == RESULT_OK) {
                    try {
                        if (currentPhotoUri != null && ivSaamanPreview != null) {
                            Log.d(TAG, "Loading image from URI: " + currentPhotoUri);
                            
                            // For newer Android versions, using ImageDecoder
                            Bitmap bitmap;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), currentPhotoUri);
                                bitmap = ImageDecoder.decodeBitmap(source);
                                Log.d(TAG, "Image decoded with ImageDecoder");
                            } else {
                                // For older Android versions
                                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), currentPhotoUri);
                                Log.d(TAG, "Image decoded with MediaStore");
                            }
                            
                            // Check if bitmap is null
                            if (bitmap != null) {
                                Log.d(TAG, "Bitmap loaded successfully: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                                ivSaamanPreview.setImageBitmap(bitmap);
                                ivSaamanPreview.setVisibility(View.VISIBLE);
                                
                                // Hide the "No image" text
                                if (tvNoImage != null) {
                                    tvNoImage.setVisibility(View.GONE);
                                }
                            } else {
                                Log.e(TAG, "Bitmap is null after decoding");
                                Toast.makeText(SaamanListActivity.this, "Error loading image", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "PhotoUri or ImageView is null. PhotoUri: " + currentPhotoUri + ", ImageView: " + (ivSaamanPreview != null));
                            if (currentPhotoUri == null) {
                                Toast.makeText(SaamanListActivity.this, "No image captured", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing camera result: " + e.getMessage(), e);
                        Toast.makeText(SaamanListActivity.this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else if (result.getResultCode() == RESULT_CANCELED) {
                    Log.d(TAG, "Camera capture was canceled by user");
                    Toast.makeText(SaamanListActivity.this, "Image capture canceled", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Camera returned error code: " + result.getResultCode());
                    Toast.makeText(SaamanListActivity.this, "Error capturing image", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void initViews() {
        tvSublocationTitle = findViewById(R.id.tv_sublocation_title);
        ivBack = findViewById(R.id.iv_back);
        recyclerSaaman = findViewById(R.id.recycler_saaman);
        fabAddSaaman = findViewById(R.id.fab_add_saaman);
        emptyView = findViewById(R.id.empty_view);

        // Set sublocation name as title
        tvSublocationTitle.setText(sublocationName);
    }

    private void setupRecyclerView() {
        adapter = new SaamanAdapter();
        adapter.setOnSaamanClickListener(this);

        recyclerSaaman.setLayoutManager(new LinearLayoutManager(this));
        recyclerSaaman.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Back button - finish activity
        ivBack.setOnClickListener(v -> finish());

        // FAB - show add saaman dialog
        fabAddSaaman.setOnClickListener(v -> showAddSaamanDialog());
    }

    private void loadSaamanItems() {
        // Clear the adapter first to prevent duplicates
        if (adapter != null) {
            adapter.clearItems();
        }
        
        firebaseHelper.getSaamanItems(locationId, sublocationId, new FirebaseHelper.OnSaamanLoadedListener() {
            @Override
            public void onSaamanLoaded(List<Saaman> saamanList) {
                adapter.setSaamanList(saamanList);
                updateEmptyViewVisibility();
                Log.d(TAG, "Loaded " + saamanList.size() + " items");
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SaamanListActivity.this, "Error loading items: " + error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading items: " + error);
            }
        });
    }

    private void updateEmptyViewVisibility() {
        if (adapter.getItemCount() == 0) {
            recyclerSaaman.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerSaaman.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showAddSaamanDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_saaman, null);
        EditText etSaamanName = dialogView.findViewById(R.id.et_saaman_name);
        EditText etSaamanDescription = dialogView.findViewById(R.id.et_saaman_description);
        Button btnTakePhoto = dialogView.findViewById(R.id.btn_take_photo);
        ivSaamanPreview = dialogView.findViewById(R.id.iv_saaman_preview);
        tvNoImage = dialogView.findViewById(R.id.tv_no_image);

        saamanDialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnAdd = dialogView.findViewById(R.id.btn_add);
        
        // Set up take photo button
        btnTakePhoto.setOnClickListener(v -> {
            checkCameraPermissionAndTakePicture();
        });

        btnCancel.setOnClickListener(v -> saamanDialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String saamanName = etSaamanName.getText().toString().trim();
            String saamanDescription = etSaamanDescription.getText().toString().trim();

            if (!saamanName.isEmpty()) {
                if (currentPhotoUri != null) {
                    // Upload the image first, then add Saaman with image URL
                    uploadSaamanImageAndCreateSaaman(saamanName, saamanDescription);
                } else {
                    // Add Saaman without image
                    addNewSaaman(saamanName, saamanDescription, null);
                }
                saamanDialog.dismiss();
            } else {
                Toast.makeText(SaamanListActivity.this, "Please enter a saaman name", Toast.LENGTH_SHORT).show();
            }
        });

        saamanDialog.show();
    }
    
    private void checkCameraPermissionAndTakePicture() {
        try {
            // Check if we already have permission
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == 
                    PackageManager.PERMISSION_GRANTED) {
                // Permission already granted, proceed with taking picture
                Log.d(TAG, "Camera permission already granted");
                takePicture();
            } else {
                // Need to request permission
                Log.d(TAG, "Requesting camera permission");
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {
                    // Show rationale and then request
                    new AlertDialog.Builder(this)
                        .setTitle("Camera Permission Required")
                        .setMessage("This app needs camera permission to take pictures of your items.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                } else {
                    // No need to show rationale, request directly
                    requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking camera permission: " + e.getMessage(), e);
            Toast.makeText(this, "Error checking permissions", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void takePicture() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            
            // Create the file where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.d(TAG, "Created image file: " + photoFile.getAbsolutePath());
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "Error creating image file: " + ex.getMessage(), ex);
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Continue only if the File was successfully created
            if (photoFile != null) {
                try {
                    // Get content URI using FileProvider
                    currentPhotoUri = FileProvider.getUriForFile(this,
                            "com.example.whereismysamaan.fileprovider",
                            photoFile);
                    
                    Log.d(TAG, "Photo URI created: " + currentPhotoUri);
                    
                    // Add output URI to intent
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                    
                    // Grant permissions for the URI to the intent receiver
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    
                    // Check if there's an app that can handle this intent
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        Log.d(TAG, "Launching camera activity");
                        takePictureLauncher.launch(takePictureIntent);
                    } else {
                        Log.e(TAG, "No camera app found");
                        Toast.makeText(this, "No camera app found on this device", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error setting up camera intent: " + e.getMessage(), e);
                    Toast.makeText(this, "Error launching camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in takePicture: " + e.getMessage(), e);
            Toast.makeText(this, "Error launching camera", Toast.LENGTH_SHORT).show();
        }
    }
    
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",   /* suffix */
                storageDir      /* directory */
        );
    }
    
    private void uploadSaamanImageAndCreateSaaman(String name, String description) {
        // Show loading dialog
        AlertDialog loadingDialog = new MaterialAlertDialogBuilder(this)
            .setTitle("Processing Image")
            .setMessage("Please wait while your image is being processed...")
            .setCancelable(false)
            .create();
        loadingDialog.show();
        
        try {
            if (currentPhotoUri == null) {
                Log.e(TAG, "Photo URI is null");
                Toast.makeText(this, "Error: No image selected", Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                addNewSaaman(name, description, null);
                return;
            }
            
            Log.d(TAG, "Starting image processing for URI: " + currentPhotoUri);
            
            // Prepare Firebase Database path
            String userId = firebaseHelper.getCurrentUserId();
            if (userId == null) {
                Log.e(TAG, "User not logged in");
                Toast.makeText(this, "Error: You must be logged in to add items", Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                addNewSaaman(name, description, null);
                return;
            }
            
            try {
                // Compress and encode the image using ImageCompressor
                String base64Image = ImageCompressor.compressAndEncodeImage(
                    this, currentPhotoUri, 800, 70);
                
                if (base64Image == null) {
                    Log.e(TAG, "Failed to compress and encode image");
                    Toast.makeText(this, "Unable to process image data", Toast.LENGTH_SHORT).show();
                    loadingDialog.dismiss();
                    addNewSaaman(name, description, null);
                    return;
                }
                
                Log.d(TAG, "Successfully compressed and encoded image to base64, size: " + base64Image.length() + " characters");
                
                // Create and add saaman with the base64 encoded image
                Saaman saaman = new Saaman(name, description, sublocationId);
                saaman.setLocationId(locationId);
                saaman.setImageBase64(base64Image);
                
                // Add to Firebase
                firebaseHelper.addSaaman(saaman, task -> {
                    loadingDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(SaamanListActivity.this, "Added new saaman: " + name, Toast.LENGTH_SHORT).show();
                        
                        // Instead of reloading all items, just add the new one to the adapter
                        runOnUiThread(() -> {
                            adapter.addSaaman(saaman);
                            updateEmptyViewVisibility();
                        });
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(SaamanListActivity.this, 
                                "Failed to add saaman: " + errorMsg, 
                                Toast.LENGTH_SHORT).show();
                    }
                    
                    // Reset the current photo URI
                    currentPhotoUri = null;
                });
            } catch (Exception e) {
                Log.e(TAG, "Error during image compression: " + e.getMessage(), e);
                loadingDialog.dismiss();
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
                addNewSaaman(name, description, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
            loadingDialog.dismiss();
            Toast.makeText(this, "Error adding item", Toast.LENGTH_SHORT).show();
            addNewSaaman(name, description, null);
        }
    }
    
    private void addNewSaaman(String name, String description, String imageUrl) {
        // Create a new Saaman object
        Saaman saaman = new Saaman(name, description, sublocationId);
        saaman.setLocationId(locationId);
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            saaman.setImageUrl(imageUrl);
        }
        
        // Add to Firebase
        firebaseHelper.addSaaman(saaman, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(SaamanListActivity.this, "Added new saaman: " + name, Toast.LENGTH_SHORT).show();
                
                // Instead of reloading all items, just add the new one to the adapter
                runOnUiThread(() -> {
                    adapter.addSaaman(saaman);
                    updateEmptyViewVisibility();
                });
            } else {
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(SaamanListActivity.this, 
                        "Failed to add saaman: " + errorMsg, 
                        Toast.LENGTH_SHORT).show();
            }
        });
        
        // Reset the current photo URI
        currentPhotoUri = null;
    }

    // Helper method to convert URI to byte array
    private byte[] getBytesFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            return null;
        }
        
        try {
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, bytesRead);
            }
            return byteBuffer.toByteArray();
        } finally {
            inputStream.close();
        }
    }

    @Override
    public void onSaamanClick(Saaman saaman, int position) {
        // Show a dialog with options: View Details or Share
        new AlertDialog.Builder(this)
            .setTitle(saaman.getName())
            .setItems(new String[]{"View Details", "Share"}, (dialog, which) -> {
                if (which == 0) {
                    // View Details option
                    showSaamanDetailsDialog(saaman);
                } else if (which == 1) {
                    // Share option
                    shareSaaman(saaman);
                }
            })
            .show();
    }
    
    private void showSaamanDetailsDialog(Saaman saaman) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_saaman_details, null);
        
        TextView tvName = view.findViewById(R.id.tv_saaman_name);
        TextView tvDescription = view.findViewById(R.id.tv_saaman_description);
        ImageView ivImage = view.findViewById(R.id.iv_saaman_image);
        TextView tvNoImage = view.findViewById(R.id.tv_no_image_details);
        
        tvName.setText(saaman.getName());
        tvDescription.setText(saaman.getDescription());
        
        // Try to load base64 encoded image first, fallback to URL if available
        if (saaman.getImageBase64() != null && !saaman.getImageBase64().isEmpty()) {
            try {
                // Decode base64 string to bitmap
                byte[] decodedBytes = Base64.decode(saaman.getImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                
                if (bitmap != null) {
                    ivImage.setImageBitmap(bitmap);
                    ivImage.setVisibility(View.VISIBLE);
                    tvNoImage.setVisibility(View.GONE);
                } else {
                    // Failed to decode bitmap
                    ivImage.setVisibility(View.GONE);
                    tvNoImage.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error decoding base64 image: " + e.getMessage(), e);
                ivImage.setVisibility(View.GONE);
                tvNoImage.setVisibility(View.VISIBLE);
            }
        } else if (saaman.getImageUrl() != null && !saaman.getImageUrl().isEmpty()) {
            // Fallback to URL image if available
            Glide.with(this)
                .load(saaman.getImageUrl())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Failed to load image: " + e.getMessage(), e);
                        ivImage.setVisibility(View.GONE);
                        tvNoImage.setVisibility(View.VISIBLE);
                        return false;
                    }
                    
                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        ivImage.setVisibility(View.VISIBLE);
                        tvNoImage.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(ivImage);
        } else {
            // No image available
            ivImage.setVisibility(View.GONE);
            tvNoImage.setVisibility(View.VISIBLE);
        }
        
        builder.setView(view);
        builder.setPositiveButton("Close", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void shareSaaman(Saaman saaman) {
        // First check if we have a logged-in user
        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "You must be logged in to share items", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get the current user's profile to get the user name
        firebaseHelper.getUserProfile(new FirebaseHelper.OnUserProfileLoadedListener() {
            @Override
            public void onUserProfileLoaded(User user) {
                if (user == null) {
                    Toast.makeText(SaamanListActivity.this, 
                            "Failed to load your profile", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Get location name
                firebaseHelper.getLocationName(locationId, new FirebaseHelper.OnNameLoadedListener() {
                    @Override
                    public void onNameLoaded(String locationName) {
                        if (locationName == null) {
                            Toast.makeText(SaamanListActivity.this, 
                                    "Failed to load location information", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Get sublocation name
                        firebaseHelper.getSublocationName(locationId, sublocationId, new FirebaseHelper.OnNameLoadedListener() {
                            @Override
                            public void onNameLoaded(String sublocationName) {
                                if (sublocationName == null) {
                                    Toast.makeText(SaamanListActivity.this, 
                                            "Failed to load sublocation information", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                
                                // Show user selection dialog
                                showUserSelectionDialog(saaman, user.getUsername(), locationName, sublocationName);
                            }
                            
                            @Override
                            public void onError(String error) {
                                Toast.makeText(SaamanListActivity.this, 
                                        "Error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        Toast.makeText(SaamanListActivity.this, 
                                "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(SaamanListActivity.this, 
                        "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showUserSelectionDialog(Saaman saaman, String senderName, String locationName, String sublocationName) {
        // Load list of users to share with
        firebaseHelper.getAllUsers(new FirebaseHelper.OnUsersLoadedListener() {
            @Override
            public void onUsersLoaded(List<User> users) {
                if (users == null || users.isEmpty()) {
                    Toast.makeText(SaamanListActivity.this, 
                            "No users found to share with", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Filter out the current user
                String currentUserId = firebaseHelper.getCurrentUserId();
                List<User> filteredUsers = new ArrayList<>();
                
                for (User user : users) {
                    if (!user.getId().equals(currentUserId)) {
                        filteredUsers.add(user);
                    }
                }
                
                if (filteredUsers.isEmpty()) {
                    Toast.makeText(SaamanListActivity.this, 
                            "No other users found to share with", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Create a list of usernames for the dialog
                final CharSequence[] usernames = new CharSequence[filteredUsers.size()];
                for (int i = 0; i < filteredUsers.size(); i++) {
                    usernames[i] = filteredUsers.get(i).getUsername();
                }
                
                // Show dialog with user list
                new AlertDialog.Builder(SaamanListActivity.this)
                    .setTitle("Share with")
                    .setItems(usernames, (dialog, position) -> {
                        User selectedUser = filteredUsers.get(position);
                        
                        // Create a unique message ID
                        String messageId = UUID.randomUUID().toString();
                        
                        // Determine which image to use
                        String imageToShare = null;
                        boolean isBase64 = false;
                        
                        if (saaman.getImageBase64() != null && !saaman.getImageBase64().isEmpty()) {
                            // Use the base64 image directly (already compressed during capture)
                            imageToShare = saaman.getImageBase64();
                            isBase64 = true;
                        } else if (saaman.getImageUrl() != null && !saaman.getImageUrl().isEmpty()) {
                            imageToShare = saaman.getImageUrl();
                            isBase64 = false;
                        }
                        
                        // Create message object with the selected user's ID
                        Message message = new Message(
                                currentUserId,
                                senderName,
                                selectedUser.getId(),
                                saaman.getId(),
                                saaman.getName(),
                                imageToShare,
                                locationName,
                                sublocationName);
                        
                        // Set message ID and image type
                        message.setId(messageId);
                        message.setIsImageBase64(isBase64);

                        // Send the message
                        firebaseHelper.sendMessage(message, task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(SaamanListActivity.this, 
                                        "Shared with " + selectedUser.getUsername(), 
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                String errorMsg = task.getException() != null ? 
                                        task.getException().getMessage() : "Unknown error";
                                
                                Toast.makeText(SaamanListActivity.this, 
                                        "Failed to share: " + errorMsg, 
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(SaamanListActivity.this, 
                        "Error loading users: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkStoragePermissions() {
        // For Android 10+ (API 29+), we don't need READ_EXTERNAL_STORAGE permission for our own app's files
        // But for older versions, check and request if needed
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 
                        102);
            }
        }
    }

    /**
     * Helper method to check if a file exists and is readable
     * @param uri The URI to check
     * @return true if the file exists and is readable
     */
    private boolean isFileReadable(Uri uri) {
        try {
            if (uri == null) {
                Log.e(TAG, "isFileReadable: URI is null");
                return false;
            }
            
            // For file URIs, check if the file exists
            if ("file".equals(uri.getScheme())) {
                File file = new File(uri.getPath());
                boolean exists = file.exists();
                boolean canRead = file.canRead();
                Log.d(TAG, "File check - exists: " + exists + ", canRead: " + canRead + ", path: " + uri.getPath());
                return exists && canRead;
            }
            
            // For content URIs, try to open an input stream
            ContentResolver contentResolver = getContentResolver();
            try (InputStream stream = contentResolver.openInputStream(uri)) {
                boolean result = stream != null;
                Log.d(TAG, "Content URI check: " + (result ? "readable" : "not readable"));
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking file readability: " + e.getMessage(), e);
            return false;
        }
    }
    
    private void diagnoseSaamanUploadIssue(Uri imageUri) {
        // Log URI details
        Log.d(TAG, "Image URI: " + imageUri);
        Log.d(TAG, "URI scheme: " + (imageUri != null ? imageUri.getScheme() : "null"));
        
        // Check if file is readable
        boolean isReadable = isFileReadable(imageUri);
        Log.d(TAG, "URI is readable: " + isReadable);
        
        // Check Firebase configuration
        String uid = firebaseHelper.getCurrentUserId();
        Log.d(TAG, "Firebase User ID: " + uid);
        Log.d(TAG, "Is user logged in: " + firebaseHelper.isUserLoggedIn());
        
        // Check storage permissions
        boolean hasReadPermission = ContextCompat.checkSelfPermission(this, 
                android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasWritePermission = ContextCompat.checkSelfPermission(this, 
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        
        Log.d(TAG, "Storage permissions - Read: " + hasReadPermission + ", Write: " + hasWritePermission);
    }
} 