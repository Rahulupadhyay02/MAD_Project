package com.example.whereismysamaan;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    
    private void uploadSaamanImageAndCreateSaaman(String name, String description) {
        // Show loading dialog
        AlertDialog loadingDialog = new MaterialAlertDialogBuilder(this)
            .setTitle("Uploading Image")
            .setMessage("Please wait while your image is being uploaded...")
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
            
            Log.d(TAG, "Starting image upload process for URI: " + currentPhotoUri);
            
            // Generate a unique file name with timestamp
            String timestamp = String.valueOf(System.currentTimeMillis());
            String storageFileName = "saaman_" + timestamp + ".jpg";
            
            // Prepare Firebase Storage path
            String userId = firebaseHelper.getCurrentUserId();
            if (userId == null) {
                Log.e(TAG, "User not logged in - Firebase Storage rules will deny upload");
                Toast.makeText(this, "Error: You must be logged in to upload images", Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                addNewSaaman(name, description, null);
                return;
            }
            
            // Create a direct byte array for upload
            try {
                // Get file bytes
                byte[] fileData = getBytesFromUri(currentPhotoUri);
                if (fileData == null || fileData.length == 0) {
                    Log.e(TAG, "Failed to read file data");
                    Toast.makeText(this, "Unable to read image data", Toast.LENGTH_SHORT).show();
                    loadingDialog.dismiss();
                    addNewSaaman(name, description, null);
                    return;
                }
                
                Log.d(TAG, "Successfully read image data: " + fileData.length + " bytes");
                
                // Upload file bytes directly
                String imagePath = "saaman_images/" + userId + "/" + locationId + "/" + sublocationId + "/" + storageFileName;
                StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(imagePath);
                
                imageRef.putBytes(fileData)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get download URL
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            Log.d(TAG, "Upload successful. Image URL: " + imageUrl);
                            loadingDialog.dismiss();
                            addNewSaaman(name, description, imageUrl);
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL", e);
                            loadingDialog.dismiss();
                            addNewSaaman(name, description, null);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Upload failed", e);
                        loadingDialog.dismiss();
                        Toast.makeText(SaamanListActivity.this, 
                                "Failed to upload image: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        addNewSaaman(name, description, null);
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error during file conversion: " + e.getMessage(), e);
                loadingDialog.dismiss();
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
                addNewSaaman(name, description, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in uploadSaamanImageAndCreateSaaman: " + e.getMessage(), e);
            loadingDialog.dismiss();
            Toast.makeText(this, "Error uploading image", Toast.LENGTH_SHORT).show();
            addNewSaaman(name, description, null);
        }
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
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_saaman_details, null);
        
        TextView tvName = dialogView.findViewById(R.id.tv_saaman_name);
        TextView tvDescription = dialogView.findViewById(R.id.tv_saaman_description);
        ImageView ivSaamanImage = dialogView.findViewById(R.id.iv_saaman_image);
        Button btnShare = dialogView.findViewById(R.id.btn_share);
        
        tvName.setText(saaman.getName());
        
        if (saaman.getDescription() != null && !saaman.getDescription().isEmpty()) {
            tvDescription.setText(saaman.getDescription());
            tvDescription.setVisibility(View.VISIBLE);
        } else {
            tvDescription.setVisibility(View.GONE);
        }
        
        // Load image if available
        if (saaman.getImageUrl() != null && !saaman.getImageUrl().isEmpty()) {
            // Use Glide to load image
            ivSaamanImage.setVisibility(View.VISIBLE);
            com.bumptech.glide.Glide.with(this)
                .load(saaman.getImageUrl())
                .into(ivSaamanImage);
        } else {
            ivSaamanImage.setVisibility(View.GONE);
        }
        
        // Set up share button
        btnShare.setOnClickListener(v -> {
            shareSaaman(saaman);
        });
        
        new AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show();
    }
    
    private void shareSaaman(Saaman saaman) {
        // Get the current location name
        String locationName = this.sublocationName;
        
        // Show loading dialog
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
            .setTitle("Loading Users")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create();
        loadingDialog.show();
        
        // Get all app users
        firebaseHelper.getAllUsers(new FirebaseHelper.OnUsersLoadedListener() {
            @Override
            public void onUsersLoaded(List<User> users) {
                // Dismiss loading dialog
                loadingDialog.dismiss();
                
                if (users.isEmpty()) {
                    Toast.makeText(SaamanListActivity.this, "No users to share with", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Create and show user selection dialog
                showUserSelectionDialog(users, saaman, locationName);
            }
            
            @Override
            public void onError(String error) {
                // Dismiss loading dialog
                loadingDialog.dismiss();
                
                Toast.makeText(SaamanListActivity.this, "Error loading users: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showUserSelectionDialog(List<User> users, Saaman saaman, String locationName) {
        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_selection, null);
        RecyclerView recyclerUsers = dialogView.findViewById(R.id.recycler_users);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        
        // Set the title
        tvTitle.setText("Share \"" + saaman.getName() + "\" with:");
        
        // Setup RecyclerView
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        
        // Create user adapter
        UserAdapter adapter = new UserAdapter(users);
        adapter.setOnUserClickListener(user -> {
            // Share the Samaan with this user
            shareWithUser(user, saaman, locationName);
            
            // Dismiss the dialog
            if (userSelectionDialog != null) {
                userSelectionDialog.dismiss();
            }
        });
        
        recyclerUsers.setAdapter(adapter);
        
        // Create and show the dialog
        userSelectionDialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create();
        
        userSelectionDialog.show();
    }
    
    private void shareWithUser(User recipient, Saaman saaman, String locationName) {
        // Get current user info
        firebaseHelper.getUserProfile(new FirebaseHelper.OnUserProfileLoadedListener() {
            @Override
            public void onUserProfileLoaded(User sender) {
                // Create the message
                Message message = new Message(
                    sender.getId(),
                    sender.getName(),
                    recipient.getId(),
                    saaman.getId(),
                    saaman.getName(),
                    saaman.getImageUrl(),
                    locationName,
                    sublocationName
                );
                
                // Send the message
                firebaseHelper.sendMessage(message, task -> {
                    if (task != null && task.isSuccessful()) {
                        Toast.makeText(SaamanListActivity.this, 
                            "Shared with " + recipient.getName(), Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMsg = (task != null && task.getException() != null) ? 
                            task.getException().getMessage() : "Unknown error";
                        Toast.makeText(SaamanListActivity.this, 
                            "Failed to share: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(SaamanListActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
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