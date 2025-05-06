package com.example.whereismysamaan.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A utility class to compress images before converting to base64
 */
public class ImageCompressor {
    private static final String TAG = "ImageCompressor";
    
    /**
     * Compress a bitmap image to a smaller size
     * 
     * @param bitmap The original bitmap to compress
     * @param quality Compression quality (0-100)
     * @param maxDimension Maximum width or height in pixels
     * @return The compressed bitmap
     */
    public static Bitmap compressBitmap(Bitmap bitmap, int quality, int maxDimension) {
        if (bitmap == null) {
            Log.e(TAG, "Cannot compress null bitmap");
            return null;
        }
        
        try {
            // Calculate new dimensions while maintaining aspect ratio
            int originalWidth = bitmap.getWidth();
            int originalHeight = bitmap.getHeight();
            
            // Check if we need to resize
            if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
                Log.d(TAG, "Image already smaller than max dimension, skipping resize");
                return bitmap;
            }
            
            float scaleFactor = Math.min(
                (float) maxDimension / originalWidth,
                (float) maxDimension / originalHeight
            );
            
            int targetWidth = Math.round(originalWidth * scaleFactor);
            int targetHeight = Math.round(originalHeight * scaleFactor);
            
            Log.d(TAG, "Resizing image from " + originalWidth + "x" + originalHeight + 
                  " to " + targetWidth + "x" + targetHeight);
            
            // Create the resized bitmap
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                bitmap, targetWidth, targetHeight, true);
            
            return resizedBitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error resizing bitmap: " + e.getMessage(), e);
            return bitmap;
        }
    }
    
    /**
     * Convert a bitmap to a base64 encoded string with compression
     * 
     * @param bitmap The bitmap to convert
     * @param quality JPEG compression quality (0-100)
     * @return The base64 encoded string
     */
    public static String bitmapToBase64(Bitmap bitmap, int quality) {
        if (bitmap == null) {
            Log.e(TAG, "Cannot convert null bitmap to base64");
            return null;
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        
        String base64String = Base64.encodeToString(byteArray, Base64.DEFAULT);
        Log.d(TAG, "Converted bitmap to base64 string, size: " + base64String.length() + " characters");
        
        return base64String;
    }
    
    /**
     * Load an image from a URI and compress it
     * 
     * @param context The context
     * @param uri The URI of the image
     * @param maxDimension Maximum width or height in pixels
     * @param quality JPEG compression quality (0-100)
     * @return The base64 encoded string of the compressed image
     */
    public static String compressAndEncodeImage(Context context, Uri uri, int maxDimension, int quality) {
        if (context == null || uri == null) {
            Log.e(TAG, "Context or URI is null");
            return null;
        }
        
        try {
            // Load the bitmap from URI
            Bitmap originalBitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
                originalBitmap = ImageDecoder.decodeBitmap(source);
            } else {
                originalBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            }
            
            // Compress the bitmap
            Bitmap compressedBitmap = compressBitmap(originalBitmap, quality, maxDimension);
            
            // Convert to base64
            String base64Image = bitmapToBase64(compressedBitmap, quality);
            
            // Clean up bitmaps
            if (originalBitmap != compressedBitmap) {
                originalBitmap.recycle();
            }
            compressedBitmap.recycle();
            
            return base64Image;
        } catch (IOException e) {
            Log.e(TAG, "Error loading image from URI: " + e.getMessage(), e);
            return null;
        }
    }
} 