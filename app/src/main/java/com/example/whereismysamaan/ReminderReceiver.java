package com.example.whereismysamaan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    private static final String CHANNEL_ID = "reminder_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d(TAG, "Reminder received");
            
            // Create an intent for when the notification is tapped
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 
                    0, 
                    notificationIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Build the notification with a fallback icon if needed
            int iconResource = R.drawable.ic_notification;
            try {
                // Check if the ic_notification drawable exists
                context.getResources().getDrawable(R.drawable.ic_notification, null);
            } catch (Exception e) {
                Log.e(TAG, "Notification icon not found, using application icon", e);
                iconResource = R.mipmap.ic_launcher; // Fallback to app icon
            }
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(iconResource)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.reminder_notification_message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            // Get the notification manager
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null");
                return;
            }

            // Create notification channel for Android 8.0 and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            context.getString(R.string.reminder_channel_name),
                            NotificationManager.IMPORTANCE_DEFAULT
                    );
                    channel.setDescription(context.getString(R.string.reminder_channel_description));
                    notificationManager.createNotificationChannel(channel);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating notification channel: " + e.getMessage(), e);
                }
            }

            // Show the notification
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Notification sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage(), e);
        }
    }
} 