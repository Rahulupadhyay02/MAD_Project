package com.example.whereismysamaan.firebase;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import com.example.whereismysamaan.model.Location;
import com.example.whereismysamaan.model.Message;
import com.example.whereismysamaan.model.Saaman;
import com.example.whereismysamaan.model.Sublocation;
import com.example.whereismysamaan.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.storage.StorageException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static FirebaseHelper instance;
    private final FirebaseAuth auth;
    private final DatabaseReference database;
    private final FirebaseStorage storage;

    // Private constructor for singleton pattern
    private FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        
        // Use the specific database URL for the Asia Southeast 1 region
        database = FirebaseDatabase.getInstance("https://madprj-42949-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                  .getReference();
        storage = FirebaseStorage.getInstance();
    }

    // Singleton instance getter
    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    // =========================
    // Authentication Methods
    // =========================
    
    public void signIn(String email, String password, OnTaskCompleteListener<AuthResult> listener) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }

    public void signUp(String email, String password, OnTaskCompleteListener<AuthResult> listener) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }

    /**
     * Signs out the current user
     */
    public void signOut() {
        Log.d(TAG, "Signing out user");
        if (auth != null) {
            auth.signOut();
            Log.d(TAG, "User signed out successfully");
        } else {
            Log.e(TAG, "Unable to sign out: auth is null");
        }
    }

    /**
     * Deletes the current user account and all associated data
     * @param listener Listener to handle completion of the task
     */
    public void deleteUserAccount(OnTaskCompleteListener<Void> listener) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Cannot delete user account: User not logged in");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }

        String userId = user.getUid();
        Log.d(TAG, "Attempting to delete user account: " + userId);
        
        // First, delete all user data from the database
        database.child("users").child(userId).removeValue()
            .addOnCompleteListener(databaseTask -> {
                if (databaseTask.isSuccessful()) {
                    Log.d(TAG, "Successfully deleted user data from database");
                    
                    // Then, delete any profile images from storage
                    StorageReference storageRef = storage.getReference().child("profile_images").child(userId);
                    storageRef.delete().addOnCompleteListener(storageTask -> {
                        Log.d(TAG, "Profile image deletion " + 
                            (storageTask.isSuccessful() ? "successful" : "failed or not found"));
                        
                        // Finally, delete the authentication account
                        user.delete().addOnCompleteListener(authTask -> {
                            if (authTask.isSuccessful()) {
                                Log.d(TAG, "Successfully deleted user authentication account");
                            } else {
                                Log.e(TAG, "Failed to delete user authentication account", 
                                    authTask.getException());
                            }
                            
                            if (listener != null) {
                                listener.onComplete(authTask);
                            }
                        });
                    });
                } else {
                    Log.e(TAG, "Failed to delete user data from database", databaseTask.getException());
                    if (listener != null) {
                        listener.onComplete(databaseTask);
                    }
                }
            });
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // =========================
    // Location Methods
    // =========================
    
    public void addLocation(Location location, OnTaskCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "addLocation: User not logged in");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }

        Log.d(TAG, "addLocation: Adding location with ID: " + location.getId() + ", name: " + location.getName());
        
        DatabaseReference locationRef = database.child("users").child(userId).child("locations").child(location.getId());
        locationRef.setValue(location)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "addLocation: Successfully added location: " + location.getName());
                } else {
                    Log.e(TAG, "addLocation: Failed to add location", task.getException());
                }
                
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }

    /**
     * Get all locations for the current user, optionally excluding default locations
     *
     * @param listener Listener to handle the loaded locations
     * @param excludeDefaults Whether to exclude default locations (e.g., "My Home", "Office", etc.)
     */
    public void getLocations(OnLocationsLoadedListener listener, boolean excludeDefaults) {
        Log.d(TAG, "Getting locations for user, excludeDefaults: " + excludeDefaults);
        if (!isUserLoggedIn()) {
            Log.e(TAG, "Cannot get locations: User not logged in");
            if (listener != null) {
                listener.onError("User not logged in");
            }
            return;
        }

        // Get current user ID
        String userId = auth.getCurrentUser().getUid();
        
        // Reference to the user's locations
        database.child("users").child(userId).child("locations").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Location> locationList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Location location = snapshot.getValue(Location.class);
                    if (location != null) {
                        // Set the ID from the database key
                        location.setId(snapshot.getKey());
                        
                        // Add to the list if we're not excluding default locations or if it's not a default location
                        if (!excludeDefaults || !isDefaultLocation(location.getName())) {
                            locationList.add(location);
                        }
                    }
                }
                Log.d(TAG, "Retrieved " + locationList.size() + " locations");
                if (listener != null) {
                    listener.onLocationsLoaded(locationList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to get locations: " + databaseError.getMessage());
                if (listener != null) {
                    listener.onError(databaseError.getMessage());
                }
            }
        });
    }

    /**
     * Get all locations for the current user, including default locations
     *
     * @param listener Listener to handle the loaded locations
     */
    public void getLocations(OnLocationsLoadedListener listener) {
        getLocations(listener, false);
    }

    /**
     * Checks if a location name is one of the default locations
     *
     * @param name The location name to check
     * @return True if it's a default location name, false otherwise
     */
    private boolean isDefaultLocation(String name) {
        return name.equals("My Home") || 
               name.equals("Office") || 
               name.equals("Warehouse") || 
               name.equals("Shop") || 
               name.equals("Car");
    }

    // =========================
    // Sublocation Methods
    // =========================
    
    public void addSublocation(Sublocation sublocation, OnTaskCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "addSublocation: User not logged in");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }

        if (sublocation.getLocationId() == null || sublocation.getLocationId().isEmpty()) {
            Log.e(TAG, "addSublocation: Invalid location ID");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }

        Log.d(TAG, "addSublocation: Adding sublocation with ID: " + sublocation.getId() + 
               ", name: " + sublocation.getName() + 
               ", to locationId: " + sublocation.getLocationId());
        
        DatabaseReference sublocationRef = database.child("users").child(userId)
            .child("locations").child(sublocation.getLocationId())
            .child("sublocations").child(sublocation.getId());
            
        sublocationRef.setValue(sublocation)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "addSublocation: Successfully added sublocation: " + sublocation.getName());
                } else {
                    Log.e(TAG, "addSublocation: Failed to add sublocation", task.getException());
                }
                
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }

    public void getSublocations(String locationId, OnSublocationsLoadedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null) {
                listener.onError("User not logged in");
            }
            return;
        }

        DatabaseReference sublocationsRef = database.child("users").child(userId)
            .child("locations").child(locationId)
            .child("sublocations");
            
        sublocationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Sublocation> sublocations = new ArrayList<>();
                for (DataSnapshot sublocationSnapshot : snapshot.getChildren()) {
                    Sublocation sublocation = sublocationSnapshot.getValue(Sublocation.class);
                    if (sublocation != null) {
                        sublocations.add(sublocation);
                    }
                }
                if (listener != null) {
                    listener.onSublocationsLoaded(sublocations);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) {
                    listener.onError(error.getMessage());
                }
            }
        });
    }

    // =========================
    // Saaman Methods
    // =========================
    
    public void addSaaman(Saaman saaman, OnTaskCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }

        DatabaseReference saamanRef = database.child("users").child(userId)
            .child("locations").child(saaman.getLocationId())
            .child("sublocations").child(saaman.getSublocationId())
            .child("saaman").child(saaman.getId());
            
        saamanRef.setValue(saaman)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }

    public void getSaamanItems(String locationId, String sublocationId, OnSaamanLoadedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null) {
                listener.onError("User not logged in");
            }
            return;
        }

        DatabaseReference saamanRef = database.child("users").child(userId)
            .child("locations").child(locationId)
            .child("sublocations").child(sublocationId)
            .child("saaman");
            
        saamanRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Saaman> saamanList = new ArrayList<>();
                for (DataSnapshot saamanSnapshot : snapshot.getChildren()) {
                    Saaman saaman = saamanSnapshot.getValue(Saaman.class);
                    if (saaman != null) {
                        saamanList.add(saaman);
                    }
                }
                if (listener != null) {
                    listener.onSaamanLoaded(saamanList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) {
                    listener.onError(error.getMessage());
                }
            }
        });
    }

    // =========================
    // User Profile Methods
    // =========================
    
    public void createUserProfile(User user, OnTaskCompleteListener<Void> listener) {
        if (user == null || user.getId() == null) {
            Log.e(TAG, "createUserProfile: User or User ID is null");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        Log.d(TAG, "createUserProfile: Creating user profile for: " + user.getEmail());
        
        DatabaseReference userRef = database.child("users").child(user.getId()).child("profile");
        userRef.setValue(user)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "createUserProfile: Successfully created profile for: " + user.getEmail());
                } else {
                    Log.e(TAG, "createUserProfile: Failed to create profile", task.getException());
                }
                
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }

    public void getUserProfile(OnUserProfileLoadedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "getUserProfile: User not logged in");
            if (listener != null) {
                listener.onError("User not logged in");
            }
            return;
        }
        
        Log.d(TAG, "getUserProfile: Getting profile for user ID: " + userId);
        
        DatabaseReference userRef = database.child("users").child(userId).child("profile");
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user == null) {
                    // Create a new user profile if not exists
                    FirebaseUser firebaseUser = getCurrentUser();
                    user = new User(userId, firebaseUser.getEmail());
                    createUserProfile(user, null);
                }
                
                if (listener != null) {
                    listener.onUserProfileLoaded(user);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "getUserProfile: Error loading profile", error.toException());
                if (listener != null) {
                    listener.onError(error.getMessage());
                }
            }
        });
    }
    
    public void updateUserProfile(User user, OnTaskCompleteListener<Void> listener) {
        if (user == null || user.getId() == null) {
            Log.e(TAG, "updateUserProfile: User or User ID is null");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        String userId = getCurrentUserId();
        if (userId == null || !userId.equals(user.getId())) {
            Log.e(TAG, "updateUserProfile: User not logged in or user ID mismatch");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        Log.d(TAG, "updateUserProfile: Updating profile for: " + user.getEmail());
        
        DatabaseReference userRef = database.child("users").child(userId).child("profile");
        userRef.setValue(user)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "updateUserProfile: Successfully updated profile for: " + user.getEmail());
                } else {
                    Log.e(TAG, "updateUserProfile: Failed to update profile", task.getException());
                }
                
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }
    
    public void uploadProfileImage(Uri imageUri, OnTaskCompleteListener<String> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "uploadProfileImage: User not logged in");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        if (imageUri == null) {
            Log.e(TAG, "uploadProfileImage: Image URI is null");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        Log.d(TAG, "uploadProfileImage: Uploading profile image for user ID: " + userId);
        
        StorageReference storageRef = storage.getReference().child("profile_images").child(userId + ".jpg");
        UploadTask uploadTask = storageRef.putFile(imageUri);
        
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            
            // Continue with the task to get the download URL
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                String downloadUrl = downloadUri.toString();
                
                // Create a wrapper Task with the String URL
                Task<String> stringTask = Tasks.forResult(downloadUrl);
                
                // Update user profile with image URL
                getUserProfile(new OnUserProfileLoadedListener() {
                    @Override
                    public void onUserProfileLoaded(User user) {
                        if (user != null) {
                            user.setProfileImageUrl(downloadUrl);
                            updateUserProfile(user, null);
                        }
                        
                        if (listener != null) {
                            listener.onComplete(stringTask);
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "uploadProfileImage: Error getting user profile: " + error);
                        if (listener != null) {
                            listener.onComplete(null);
                        }
                    }
                });
            } else {
                Log.e(TAG, "uploadProfileImage: Failed to get download URL", task.getException());
                if (listener != null) {
                    listener.onComplete(null);
                }
            }
        });
    }

    /**
     * Upload any image to Firebase Storage and get the download URL
     * 
     * @param imageUri The URI of the image to upload
     * @param storagePath The path in Firebase Storage where to store the image
     * @param listener Callback for when the upload is complete
     */
    public void uploadImage(Uri imageUri, String storagePath, OnTaskCompleteListener<String> listener) {
        if (imageUri == null) {
            Log.e(TAG, "uploadImage: Image URI is null");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "uploadImage: User not logged in");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        Log.d(TAG, "uploadImage: Starting upload for URI: " + imageUri);
        Log.d(TAG, "uploadImage: Uploading to path: " + storagePath);
        
        try {
            // Verify Firebase Storage is initialized
            if (storage == null) {
                Log.e(TAG, "uploadImage: Firebase Storage is null");
                if (listener != null) {
                    listener.onComplete(null);
                }
                return;
            }
            
            // Log Firebase configuration
            Log.d(TAG, "uploadImage: Firebase Storage bucket: " + storage.getReference().getBucket());
            
            // Create a reference to the storage location
            StorageReference storageRef = storage.getReference().child(storagePath);
            Log.d(TAG, "uploadImage: Storage reference path: " + storageRef.getPath());
            
            // Start upload task
            UploadTask uploadTask;
            try {
                uploadTask = storageRef.putFile(imageUri);
                Log.d(TAG, "uploadImage: Upload task started successfully");
            } catch (Exception e) {
                Log.e(TAG, "uploadImage: Failed to start upload task", e);
                if (listener != null) {
                    listener.onComplete(null);
                }
                return;
            }
            
            // Add progress listener for debugging
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG, "uploadImage: Upload is " + progress + "% done");
            });
            
            // Handle failures specifically
            uploadTask.addOnFailureListener(e -> {
                Log.e(TAG, "uploadImage: Upload failed with error: " + e.getMessage(), e);
                
                // Identify specific Firebase Storage error
                if (e instanceof StorageException) {
                    StorageException storageException = (StorageException) e;
                    int errorCode = storageException.getErrorCode();
                    String errorMessage = "Unknown storage error";
                    
                    // Map error codes to human-readable messages
                    switch (errorCode) {
                        case StorageException.ERROR_BUCKET_NOT_FOUND:
                            errorMessage = "Storage bucket not found";
                            break;
                        case StorageException.ERROR_NOT_AUTHENTICATED:
                            errorMessage = "User not authenticated";
                            break;
                        case StorageException.ERROR_NOT_AUTHORIZED:
                            errorMessage = "Not authorized to access storage";
                            break;
                        case StorageException.ERROR_RETRY_LIMIT_EXCEEDED:
                            errorMessage = "Retry limit exceeded";
                            break;
                        case StorageException.ERROR_QUOTA_EXCEEDED:
                            errorMessage = "Storage quota exceeded";
                            break;
                        default:
                            errorMessage = "Storage error code: " + errorCode;
                    }
                    
                    Log.e(TAG, "uploadImage: " + errorMessage);
                }
                
                if (listener != null) {
                    listener.onComplete(null);
                }
            });
            
            // On success, get the download URL
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "uploadImage: Upload task unsuccessful", task.getException());
                    throw task.getException();
                }
                
                // Continue with the task to get the download URL
                return storageRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Uri downloadUri = task.getResult();
                    String downloadUrl = downloadUri.toString();
                    
                    Log.d(TAG, "uploadImage: Upload successful, URL: " + downloadUrl);
                    
                    // Create a wrapper Task with the String URL
                    Task<String> stringTask = Tasks.forResult(downloadUrl);
                    
                    if (listener != null) {
                        listener.onComplete(stringTask);
                    }
                } else {
                    Log.e(TAG, "uploadImage: Failed to get download URL", task.getException());
                    if (listener != null) {
                        listener.onComplete(null);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "uploadImage: Unexpected error during upload", e);
            if (listener != null) {
                listener.onComplete(null);
            }
        }
    }

    // =========================
    // Message Methods
    // =========================
    
    /**
     * Send a message to another user
     * 
     * @param message Message object to send
     * @param listener Listener to handle completion
     */
    public void sendMessage(Message message, OnTaskCompleteListener<Void> listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Log.e(TAG, "sendMessage: User not logged in");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        Log.d(TAG, "sendMessage: Sending message to user: " + message.getReceiverId());
        
        // Set content
        message.setContent("Shared " + message.getSaamanName() + " with you.");
        
        // Set as unread initially
        message.setRead(false);

        if (message.getTitle() == null || message.getTitle().isEmpty()) {
            message.setTitle("Shared Saaman");
        }
        
        // Set timestamp
        message.setTimestamp(System.currentTimeMillis());
        
        // Save to sender's list (for reference)
        database.child("messages").child(message.getId()).setValue(message)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "sendMessage: Message saved successfully");
                    
                    // Also add to recipient's list
                    database.child("user_messages").child(message.getReceiverId()).child(message.getId()).setValue(true)
                        .addOnCompleteListener(recipientTask -> {
                            if (recipientTask.isSuccessful()) {
                                Log.d(TAG, "sendMessage: Message added to recipient's list");
                            } else {
                                Log.e(TAG, "sendMessage: Failed to add message to recipient's list", 
                                    recipientTask.getException());
                            }
                            
                            if (listener != null) {
                                listener.onComplete(recipientTask);
                            }
                        });
                } else {
                    Log.e(TAG, "sendMessage: Failed to save message", task.getException());
                    if (listener != null) {
                        listener.onComplete(task);
                    }
                }
            });
    }
    
    /**
     * Get all messages for the current user
     * 
     * @param listener Listener to handle loaded messages
     */
    public void getMessages(OnMessagesLoadedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "getMessages: User not logged in");
            if (listener != null) {
                listener.onError("User not logged in");
            }
            return;
        }
        
        Log.d(TAG, "getMessages: Getting messages for user ID: " + userId);
        
        // Get references to user's messages
        DatabaseReference userMessagesRef = database.child("user_messages").child(userId);
        userMessagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    Log.d(TAG, "getMessages: No messages found for user");
                    if (listener != null) {
                        listener.onMessagesLoaded(new ArrayList<>());
                    }
                    return;
                }
                
                // Count how many messages we need to load
                final int totalMessages = (int) dataSnapshot.getChildrenCount();
                final List<Message> messages = new ArrayList<>();
                final int[] loadedCount = {0}; // Use array to modify in inner class
                
                // Iterate through message IDs
                for (DataSnapshot messageIdSnapshot : dataSnapshot.getChildren()) {
                    String messageId = messageIdSnapshot.getKey();
                    
                    // Get the actual message data
                    database.child("messages").child(messageId).addListenerForSingleValueEvent(
                            new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot messageSnapshot) {
                            loadedCount[0]++;
                            
                            if (messageSnapshot.exists()) {
                                Message message = messageSnapshot.getValue(Message.class);
                                if (message != null) {
                                    messages.add(message);
                                }
                            }
                            
                            // If we've loaded all messages, return the result
                            if (loadedCount[0] >= totalMessages) {
                                // Sort by timestamp in descending order (newest first)
                                Collections.sort(messages, (m1, m2) -> 
                                        Long.compare(m2.getTimestamp(), m1.getTimestamp()));
                                
                                Log.d(TAG, "getMessages: Retrieved " + messages.size() + " messages");
                                if (listener != null) {
                                    listener.onMessagesLoaded(messages);
                                }
                            }
                        }
                        
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            loadedCount[0]++;
                            Log.e(TAG, "getMessages: Error loading message: " + databaseError.getMessage());
                            
                            // If we've tried to load all messages, return what we have so far
                            if (loadedCount[0] >= totalMessages) {
                                // Sort by timestamp in descending order (newest first)
                                Collections.sort(messages, (m1, m2) -> 
                                        Long.compare(m2.getTimestamp(), m1.getTimestamp()));
                                
                                Log.d(TAG, "getMessages: Retrieved " + messages.size() + " messages (with some errors)");
                                if (listener != null) {
                                    listener.onMessagesLoaded(messages);
                                }
                            }
                        }
                    });
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "getMessages: Failed to get messages", databaseError.toException());
                if (listener != null) {
                    listener.onError(databaseError.getMessage());
                }
            }
        });
    }
    
    /**
     * Mark a message as read
     * 
     * @param messageId ID of the message to mark as read
     * @param listener Listener to handle completion
     */
    public void markMessageAsRead(String messageId, OnTaskCompleteListener<Void> listener) {
        if (messageId == null) {
            Log.e(TAG, "markMessageAsRead: Message ID is null");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        Log.d(TAG, "markMessageAsRead: Marking message " + messageId + " as read");
        
        // Update the read status in the messages node
        DatabaseReference messageRef = database.child("messages").child(messageId).child("read");
        
        messageRef.setValue(true)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "markMessageAsRead: Message marked as read successfully");
                } else {
                    Log.e(TAG, "markMessageAsRead: Failed to mark message as read", task.getException());
                }
                
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }
    
    /**
     * Get all app users for selecting message recipients
     * 
     * @param listener Listener to handle loaded users
     */
    public void getAllUsers(OnUsersLoadedListener listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Log.e(TAG, "getAllUsers: User not logged in");
            if (listener != null) {
                listener.onError("User not logged in");
            }
            return;
        }
        
        Log.d(TAG, "getAllUsers: Getting all users");
        
        DatabaseReference usersRef = database.child("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Get the user ID (which is the key)
                    String userId = snapshot.getKey();
                    
                    // Skip the current user
                    if (userId.equals(currentUserId)) {
                        continue;
                    }
                    
                    // Get the user's profile
                    DataSnapshot profileSnapshot = snapshot.child("profile");
                    User user = profileSnapshot.getValue(User.class);
                    
                    if (user != null) {
                        // Make sure the ID is set
                        user.setId(userId);
                        users.add(user);
                    }
                }
                
                Log.d(TAG, "getAllUsers: Retrieved " + users.size() + " users");
                if (listener != null) {
                    listener.onUsersLoaded(users);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "getAllUsers: Failed to get users", databaseError.toException());
                if (listener != null) {
                    listener.onError(databaseError.getMessage());
                }
            }
        });
    }

    /**
     * Get a location name by its ID
     * @param locationId The ID of the location
     * @param listener Listener to handle the loaded name
     */
    public void getLocationName(String locationId, OnNameLoadedListener listener) {
        if (!isUserLoggedIn()) {
            Log.e(TAG, "Cannot get location name: User not logged in");
            if (listener != null) {
                listener.onError("User not logged in");
            }
            return;
        }
        
        String userId = getCurrentUserId();
        database.child("users").child(userId).child("locations").child(locationId).child("name")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = snapshot.getValue(String.class);
                    if (name != null) {
                        Log.d(TAG, "Retrieved location name: " + name);
                        if (listener != null) {
                            listener.onNameLoaded(name);
                        }
                    } else {
                        Log.e(TAG, "Location name not found for ID: " + locationId);
                        if (listener != null) {
                            listener.onError("Location name not found");
                        }
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to get location name: " + error.getMessage());
                    if (listener != null) {
                        listener.onError(error.getMessage());
                    }
                }
            });
    }

    /**
     * Get a sublocation name by its ID and parent location ID
     * @param locationId The ID of the parent location
     * @param sublocationId The ID of the sublocation
     * @param listener Listener to handle the loaded name
     */
    public void getSublocationName(String locationId, String sublocationId, OnNameLoadedListener listener) {
        if (!isUserLoggedIn()) {
            Log.e(TAG, "Cannot get sublocation name: User not logged in");
            if (listener != null) {
                listener.onError("User not logged in");
            }
            return;
        }
        
        String userId = getCurrentUserId();
        database.child("users").child(userId).child("locations").child(locationId)
            .child("sublocations").child(sublocationId).child("name")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = snapshot.getValue(String.class);
                    if (name != null) {
                        Log.d(TAG, "Retrieved sublocation name: " + name);
                        if (listener != null) {
                            listener.onNameLoaded(name);
                        }
                    } else {
                        Log.e(TAG, "Sublocation name not found for ID: " + sublocationId);
                        if (listener != null) {
                            listener.onError("Sublocation name not found");
                        }
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to get sublocation name: " + error.getMessage());
                    if (listener != null) {
                        listener.onError(error.getMessage());
                    }
                }
            });
    }

    // =========================
    // Interfaces
    // =========================
    
    public interface OnTaskCompleteListener<T> {
        void onComplete(Task<T> task);
    }

    public interface OnLocationsLoadedListener {
        void onLocationsLoaded(List<Location> locations);
        void onError(String error);
    }

    public interface OnSublocationsLoadedListener {
        void onSublocationsLoaded(List<Sublocation> sublocations);
        void onError(String error);
    }

    public interface OnSaamanLoadedListener {
        void onSaamanLoaded(List<Saaman> saamanList);
        void onError(String error);
    }

    public interface OnUserProfileLoadedListener {
        void onUserProfileLoaded(User user);
        void onError(String error);
    }

    public interface OnMessagesLoadedListener {
        void onMessagesLoaded(List<Message> messages);
        void onError(String error);
    }
    
    public interface OnUsersLoadedListener {
        void onUsersLoaded(List<User> users);
        void onError(String error);
    }

    public interface OnNameLoadedListener {
        void onNameLoaded(String name);
        void onError(String error);
    }

    public interface OnMessageLoadedListener {
        void onMessageLoaded(Message message);
        void onError(String error);
    }

    /**
     * Get a specific message by its ID
     * 
     * @param messageId ID of the message to retrieve
     * @param listener Listener to handle the loaded message
     */
    public void getMessageById(String messageId, OnMessageLoadedListener listener) {
        if (messageId == null) {
            Log.e(TAG, "getMessageById: message ID is null");
            if (listener != null) {
                listener.onError("Missing message ID");
            }
            return;
        }
        
        Log.d(TAG, "getMessageById: Getting message with ID: " + messageId);
        
        // In our new structure, messages are stored directly in the messages node
        DatabaseReference messageRef = database.child("messages").child(messageId);
        messageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Message message = snapshot.getValue(Message.class);
                
                if (message != null) {
                    Log.d(TAG, "getMessageById: Successfully retrieved message");
                    if (listener != null) {
                        listener.onMessageLoaded(message);
                    }
                } else {
                    Log.e(TAG, "getMessageById: Message not found");
                    if (listener != null) {
                        listener.onError("Message not found");
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "getMessageById: Failed to get message: " + error.getMessage());
                if (listener != null) {
                    listener.onError(error.getMessage());
                }
            }
        });
    }

    /**
     * Checks if Firebase Storage rules might be causing permission issues
     * This is a diagnostic method to help troubleshoot upload problems
     */
    public void checkStorageRules() {
        Log.d(TAG, "Checking Firebase Storage rules and permissions");
        
        // Check if user is authenticated
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User not logged in - Firebase Storage rules may deny access");
            return;
        }
        
        // Log Storage bucket info
        try {
            String bucket = storage.getReference().getBucket();
            Log.d(TAG, "Firebase Storage bucket: " + bucket);
        } catch (Exception e) {
            Log.e(TAG, "Error getting Storage bucket info", e);
        }
        
        // Try a test write to check permissions
        StorageReference testRef = storage.getReference().child("permission_test").child(userId + "_test.txt");
        byte[] testData = "test".getBytes();
        
        testRef.putBytes(testData)
            .addOnSuccessListener(taskSnapshot -> {
                Log.d(TAG, "Storage permission test passed - can write to Firebase Storage");
                
                // Clean up test file
                testRef.delete().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Test file deleted successfully");
                    } else {
                        Log.w(TAG, "Failed to delete test file", task.getException());
                    }
                });
            })
            .addOnFailureListener(e -> {
                if (e instanceof StorageException) {
                    StorageException storageException = (StorageException) e;
                    int errorCode = storageException.getErrorCode();
                    
                    if (errorCode == StorageException.ERROR_NOT_AUTHORIZED) {
                        Log.e(TAG, "STORAGE PERMISSION ERROR: Not authorized to access Firebase Storage");
                        Log.e(TAG, "This is likely due to Firebase Storage security rules that need to be updated");
                    } else {
                        Log.e(TAG, "Storage permission test failed with code: " + errorCode, e);
                    }
                } else {
                    Log.e(TAG, "Storage permission test failed", e);
                }
            });
    }
} 