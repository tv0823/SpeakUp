package com.example.speakup.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.speakup.AlarmReceiver;
import com.example.speakup.R;

import java.util.Calendar;

public class RemindersActivity extends AppCompatActivity {
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private LinearLayout reminderContainer;

    private BroadcastReceiver uiUpdater = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int codeToRemove = intent.getIntExtra("requestCode", -1);
            removeRowByCode(codeToRemove);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        reminderContainer = findViewById(R.id.reminderContainer);
        Button btnNewReminder = findViewById(R.id.btnNewReminder);

        checkNotificationPermission();
        btnNewReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewReminderDialog();
            }
        });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    public void goBack(View view) {
        finish();
    }

    private void showNewReminderDialog() {
        AlertDialog.Builder adb;
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_new_reminder, null);

        Spinner spinnerType = layout.findViewById(R.id.spinnerType);
        EditText etTime = layout.findViewById(R.id.etTime);
        EditText etDate = layout.findViewById(R.id.etDate);
        Button btnSave = layout.findViewById(R.id.btnSave);

        String[] categories = {"Recordings", "Simulations"};
        ArrayAdapter<String> adp = new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, categories);
        spinnerType.setAdapter(adp);

        etTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePicker(etTime);
            }
        });

        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker(etDate);
            }
        });

        adb = new AlertDialog.Builder(this);
        adb.setView(layout);
        AlertDialog dialog = adb.create();
        dialog.show();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String type = spinnerType.getSelectedItem().toString();
                String timeStr = etTime.getText().toString();
                String dateStr = etDate.getText().toString();

                if (!timeStr.isEmpty() && !dateStr.isEmpty()) {
                    int requestCode = (int) (System.currentTimeMillis() / 1000);
                    addNewReminderToScreen(type,  type + " reminder at " + timeStr, requestCode);

                    saveReminderToStorage(requestCode, type, dateStr, timeStr);
                    Calendar calSet = getCalendar(dateStr, timeStr);
                    setAlarm(calSet, "Time for your " + type + "!", requestCode);

                    dialog.dismiss();
                }
            }
        });
    }

    private int addNewReminderToScreen(String category, String time, final int requestCode) {
        // Check if the Category Header already exists in the layout
        if (reminderContainer.findViewWithTag(category) == null) {
            // Category not found, so we create and add the header
            View headerView = getLayoutInflater().inflate(R.layout.item_category_header, reminderContainer, false);
            TextView txtCategoryTitle = headerView.findViewById(R.id.txtCategoryTitle);
            txtCategoryTitle.setText(category);
            headerView.setTag(category); // Set the tag so we can find it next time

            reminderContainer.addView(headerView);
        }

        // Add the individual Reminder Row
        View rowView = getLayoutInflater().inflate(R.layout.item_reminder_row, reminderContainer, false);
        rowView.setTag(requestCode);

        TextView rowTitle = rowView.findViewById(R.id.rowTitle);
        TextView rowSubtitle = rowView.findViewById(R.id.rowSubtitle);
        ImageView rowIcon = rowView.findViewById(R.id.rowIcon);
        ImageView btnDelete = rowView.findViewById(R.id.deleteReminder);

        rowTitle.setText(category);
        rowSubtitle.setText(time);

        // Change icon based on selection
        rowIcon.setImageResource(category.equals("Simulations") ? R.drawable.ic_simulation_outline : R.drawable.ic_recording_outline);

        // DELETE BUTTON LOGIC
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAlarm(requestCode); // Cancel the alarm in the system
                removeFromStorage(requestCode); // Remove from storage
                reminderContainer.removeView(rowView); // Remove the row from the screen
                cleanUpHeader(category);
                Toast.makeText(RemindersActivity.this, "Reminder Deleted", Toast.LENGTH_SHORT).show();
            }
        });

        reminderContainer.addView(rowView);
        return requestCode;
    }

    private void setAlarm(Calendar calSet, String text, int requestCode) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("text", text);
        intent.putExtra("requestCode", requestCode);

        PendingIntent pI = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);
        AlarmManager aM = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (aM != null) {
            aM.set(AlarmManager.RTC, calSet.getTimeInMillis(), pI);
        }
    }

    private void cancelAlarm(int requestCode) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pI = PendingIntent.getBroadcast(this, requestCode,
                intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        if (pI != null) {
            AlarmManager aM = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            aM.cancel(pI);
            pI.cancel();
        }
    }

    private void cleanUpHeader(String category) {
        boolean hasItems = false;
        for (int i = 0; i < reminderContainer.getChildCount(); i++) {
            View v = reminderContainer.getChildAt(i);
            TextView title = v.findViewById(R.id.rowTitle);
            if (title != null && title.getText().toString().equals(category)) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) {
            View header = reminderContainer.findViewWithTag(category);
            if (header != null) reminderContainer.removeView(header);
        }
    }

    private void showDatePicker(final EditText dateField) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // Month 0 is January, so add 1
                String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                dateField.setText(selectedDate);
            }
        }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimePicker(final EditText timeField) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfHour) {
                String time = String.format("%02d:%02d", hourOfDay, minuteOfHour);
                timeField.setText(time);
            }
        }, hour, minute, true); // true for the 24-hour format
        timePickerDialog.show();
    }

    private Calendar getCalendar(String dStr, String tStr) {
        String[] d = dStr.split("/");
        String[] t = tStr.split(":");
        Calendar cal = Calendar.getInstance();
        cal.set(Integer.parseInt(d[2]), Integer.parseInt(d[1]) - 1, Integer.parseInt(d[0]), Integer.parseInt(t[0]), Integer.parseInt(t[1]), 0);
        if (cal.compareTo(Calendar.getInstance()) <= 0) cal.add(Calendar.DATE, 1);
        return cal;
    }

    private void removeRowByCode(int code) {
        Log.d("SpeakUp", "Attempting to remove row with code: " + code);
        for (int i = 0; i < reminderContainer.getChildCount(); i++) {
            View child = reminderContainer.getChildAt(i);
            if (child.getTag() != null && child.getTag().equals(code)) {
                TextView titleTv = child.findViewById(R.id.rowTitle);
                String category = (titleTv != null) ? titleTv.getText().toString() : "";

                reminderContainer.removeView(child);
                removeFromStorage(code);
                cleanUpHeader(category);
                Log.d("SpeakUp", "Row removed successfully!");
                break;
            }
        }
    }

    // Save a reminder to SharedPreferences
    private void saveReminderToStorage(int code, String category, String date, String time) {
        SharedPreferences sp = getSharedPreferences("RemindersLog", MODE_PRIVATE);
        String currentData = sp.getString("list", "");
        // Format: code|category|date|time#
        String newData = currentData + code + "|" + category + "|" + date + "|" + time + "#";
        sp.edit().putString("list", newData).apply();
    }

    // Load and clean up past reminders
    private void loadReminders() {
        SharedPreferences sp = getSharedPreferences("RemindersLog", MODE_PRIVATE);
        String data = sp.getString("list", "");
        if (data.isEmpty()) return;

        String[] items = data.split("#");
        StringBuilder remainingData = new StringBuilder();
        Calendar now = Calendar.getInstance();

        for (String item : items) {
            if (item.isEmpty()) continue;
            String[] p = item.split("\\|"); // p[0]=code, p[1]=category, p[2]=date, p[3]=time

            Calendar calItem = getCalendar(p[2], p[3]);

            // If time is in the future, show it. If past, ignore it (it gets deleted)
            if (calItem.after(now)) {
                addNewReminderToScreen(p[1], p[1] + " reminder at " + p[3], Integer.parseInt(p[0]));
                remainingData.append(item).append("#");
            }
        }
        // Save the cleaned list (without the past reminders)
        sp.edit().putString("list", remainingData.toString()).apply();
    }

    // Remove a specific reminder from storage when deleted manually
    private void removeFromStorage(int code) {
        SharedPreferences sp = getSharedPreferences("RemindersLog", MODE_PRIVATE);
        String data = sp.getString("list", "");
        String[] items = data.split("#");
        StringBuilder newData = new StringBuilder();

        for (String item : items) {
            if (!item.startsWith(code + "|")) {
                newData.append(item).append("#");
            }
        }
        sp.edit().putString("list", newData.toString()).apply();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("REMOVE_REMINDER_ROW");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(uiUpdater, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            // Older versions do not support or require the flag
            registerReceiver(uiUpdater, filter);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(uiUpdater);
        } catch (IllegalArgumentException e) {
            Log.e("unregisterReceiver", e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear the container first so we don't double the rows
        reminderContainer.removeAllViews();
        // Refresh the list from the updated SharedPreferences
        loadReminders();
    }
}