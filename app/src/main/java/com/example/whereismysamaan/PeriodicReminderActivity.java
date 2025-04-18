package com.example.whereismysamaan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PeriodicReminderActivity extends AppCompatActivity {
    private static final String TAG = "PeriodicReminderActvty";
    
    private RadioGroup reminderFrequencyGroup;
    private RadioButton dailyOption, weeklyOption, monthlyOption;
    private Button timePickerButton, saveReminderSettings;
    private TextView selectedTimeText;
    private SwitchCompat enableReminderSwitch;
    
    private Calendar selectedTime;
    private SharedPreferences sharedPreferences;
    private static final String REMINDER_PREFS = "ReminderPrefs";
    private static final String PREF_REMINDER_ENABLED = "reminderEnabled";
    private static final String PREF_REMINDER_FREQUENCY = "reminderFrequency";
    private static final String PREF_REMINDER_HOUR = "reminderHour";
    private static final String PREF_REMINDER_MINUTE = "reminderMinute";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_periodic_reminder);
            
            // Initialize views
            initViews();
            
            // Initialize calendar
            selectedTime = Calendar.getInstance();
            
            // Initialize shared preferences
            sharedPreferences = getSharedPreferences(REMINDER_PREFS, MODE_PRIVATE);
            
            // Load saved preferences
            loadSavedPreferences();
            
            // Set up time picker button
            if (timePickerButton != null) {
                timePickerButton.setOnClickListener(v -> showTimePickerDialog());
            } else {
                Log.e(TAG, "timePickerButton is null");
            }
            
            // Set up save button
            if (saveReminderSettings != null) {
                saveReminderSettings.setOnClickListener(v -> saveReminderSettings());
            } else {
                Log.e(TAG, "saveReminderSettings is null");
            }
            
            Log.d(TAG, "PeriodicReminderActivity initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing reminder settings", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void initViews() {
        try {
            reminderFrequencyGroup = findViewById(R.id.reminderFrequencyGroup);
            dailyOption = findViewById(R.id.dailyOption);
            weeklyOption = findViewById(R.id.weeklyOption);
            monthlyOption = findViewById(R.id.monthlyOption);
            timePickerButton = findViewById(R.id.timePickerButton);
            selectedTimeText = findViewById(R.id.selectedTimeText);
            enableReminderSwitch = findViewById(R.id.enableReminderSwitch);
            saveReminderSettings = findViewById(R.id.saveReminderSettings);
            
            // Set up back button
            View backButton = findViewById(R.id.backButton);
            if (backButton != null) {
                backButton.setOnClickListener(v -> finish());
            } else {
                Log.e(TAG, "backButton is null");
            }
            
            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            throw e;
        }
    }
    
    private void loadSavedPreferences() {
        try {
            boolean isEnabled = sharedPreferences.getBoolean(PREF_REMINDER_ENABLED, false);
            int frequency = sharedPreferences.getInt(PREF_REMINDER_FREQUENCY, 0);
            int hour = sharedPreferences.getInt(PREF_REMINDER_HOUR, 9);
            int minute = sharedPreferences.getInt(PREF_REMINDER_MINUTE, 0);
            
            // Set switch state
            if (enableReminderSwitch != null) {
                enableReminderSwitch.setChecked(isEnabled);
            } else {
                Log.e(TAG, "enableReminderSwitch is null");
            }
            
            // Set frequency radio button
            if (dailyOption != null && weeklyOption != null && monthlyOption != null) {
                switch (frequency) {
                    case 0:
                        dailyOption.setChecked(true);
                        break;
                    case 1:
                        weeklyOption.setChecked(true);
                        break;
                    case 2:
                        monthlyOption.setChecked(true);
                        break;
                    default:
                        dailyOption.setChecked(true);
                }
            } else {
                Log.e(TAG, "One or more radio buttons are null");
            }
            
            // Set time
            selectedTime.set(Calendar.HOUR_OF_DAY, hour);
            selectedTime.set(Calendar.MINUTE, minute);
            updateSelectedTimeText();
            
            Log.d(TAG, "Preferences loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error loading saved preferences: " + e.getMessage(), e);
        }
    }
    
    private void showTimePickerDialog() {
        try {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedTime.set(Calendar.MINUTE, minute);
                        updateSelectedTimeText();
                    },
                    selectedTime.get(Calendar.HOUR_OF_DAY),
                    selectedTime.get(Calendar.MINUTE),
                    false
            );
            timePickerDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing time picker dialog: " + e.getMessage(), e);
            Toast.makeText(this, "Could not open time picker", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateSelectedTimeText() {
        try {
            if (selectedTimeText != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                selectedTimeText.setText(sdf.format(selectedTime.getTime()));
            } else {
                Log.e(TAG, "selectedTimeText is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating selected time text: " + e.getMessage(), e);
        }
    }
    
    private void saveReminderSettings() {
        try {
            // Get selected frequency
            int selectedFrequency = 0;
            if (reminderFrequencyGroup != null) {
                int checkedRadioButtonId = reminderFrequencyGroup.getCheckedRadioButtonId();
                if (checkedRadioButtonId == R.id.dailyOption) {
                    selectedFrequency = 0;
                } else if (checkedRadioButtonId == R.id.weeklyOption) {
                    selectedFrequency = 1;
                } else if (checkedRadioButtonId == R.id.monthlyOption) {
                    selectedFrequency = 2;
                }
            } else {
                Log.e(TAG, "reminderFrequencyGroup is null");
            }
            
            // Get switch state
            boolean isEnabled = enableReminderSwitch != null && enableReminderSwitch.isChecked();
            
            // Save preferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(PREF_REMINDER_ENABLED, isEnabled);
            editor.putInt(PREF_REMINDER_FREQUENCY, selectedFrequency);
            editor.putInt(PREF_REMINDER_HOUR, selectedTime.get(Calendar.HOUR_OF_DAY));
            editor.putInt(PREF_REMINDER_MINUTE, selectedTime.get(Calendar.MINUTE));
            editor.apply();
            
            // Schedule or cancel alarm based on switch state
            if (isEnabled) {
                try {
                    scheduleReminder(selectedFrequency);
                    Toast.makeText(this, R.string.reminder_enabled, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error scheduling reminder: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to schedule reminder", Toast.LENGTH_SHORT).show();
                }
            } else {
                try {
                    cancelReminder();
                    Toast.makeText(this, R.string.reminder_disabled, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error canceling reminder: " + e.getMessage(), e);
                }
            }
            
            // Show a confirmation toast and finish the activity
            Toast.makeText(this, R.string.reminder_saved, Toast.LENGTH_SHORT).show();
            
            // Return to the settings screen
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error saving reminder settings: " + e.getMessage(), e);
            Toast.makeText(this, "Error saving settings", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void scheduleReminder(int frequency) {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager service is null");
                return;
            }
            
            Intent intent = new Intent(this, ReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Cancel any existing alarms
            alarmManager.cancel(pendingIntent);
            
            // Set up calendar for reminder time
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            // If time is already passed for today, schedule for tomorrow
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            long intervalMillis;
            switch (frequency) {
                case 0: // Daily
                    intervalMillis = AlarmManager.INTERVAL_DAY;
                    break;
                case 1: // Weekly
                    intervalMillis = AlarmManager.INTERVAL_DAY * 7;
                    break;
                case 2: // Monthly (approximate)
                    intervalMillis = AlarmManager.INTERVAL_DAY * 30;
                    break;
                default:
                    intervalMillis = AlarmManager.INTERVAL_DAY;
            }
            
            // Schedule the repeating alarm
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    intervalMillis,
                    pendingIntent
            );
            
            Log.d(TAG, "Reminder scheduled successfully for " + calendar.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminder: " + e.getMessage(), e);
            throw e;
        }
    }
    
    private void cancelReminder() {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager service is null");
                return;
            }
            
            Intent intent = new Intent(this, ReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Cancel any existing alarms
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Reminder canceled successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error canceling reminder: " + e.getMessage(), e);
            throw e;
        }
    }
} 