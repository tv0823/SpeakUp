package com.example.speakup.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

import com.example.speakup.Receivers.AlarmReceiver;
import com.example.speakup.R;

import java.util.Calendar;

/**
 * Activity for managing practice and simulation reminders.
 * <p>
 * This activity allows users to create, view, and delete reminders for their practice sessions.
 * Reminders are scheduled using {@link AlarmManager} and persisted using {@link SharedPreferences}.
 * It also handles runtime permissions for notifications on Android 13+.
 * </p>
 */
public class RemindersActivity extends AppCompatActivity {
    /**
     * Request code for notification permission.
     */
    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    /**
     * Container layout where reminder rows are dynamically added.
     */
    private LinearLayout reminderContainer;

    /**
     * Receiver to update the UI when a reminder is triggered or needs removal.
     */
    private BroadcastReceiver uiUpdater = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int codeToRemove = intent.getIntExtra("requestCode", -1);
            removeRowByCode(codeToRemove);
        }
    };

    /**
     * Initializes the activity, sets the content view, and checks for notification permissions.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied.
     */
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

    /**
     * Checks and requests notification permissions for Android 13 (API 33) and above.
     */
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

    /**
     * Finishes the activity and returns to the previous screen.
     *
     * @param view The clicked view.
     */
    public void goBack(View view) {
        finish();
    }

    /**
     * Displays a dialog to create a new reminder, allowing selection of type, time, and date.
     */
    private void showNewReminderDialog() {
        AlertDialog.Builder adb;
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_new_reminder, null);

        Spinner spinnerType = layout.findViewById(R.id.spinnerType);
        TimePicker timePicker = layout.findViewById(R.id.timePicker);
        EditText etDate = layout.findViewById(R.id.etDate);
        Button btnSave = layout.findViewById(R.id.btnSave);

        timePicker.setIs24HourView(true);

        String[] categories = {"Recordings", "Simulations"};
        ArrayAdapter<String> adp = new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, categories);
        spinnerType.setAdapter(adp);

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
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                String timeStr = String.format("%02d:%02d", hour, minute);
                String dateStr = etDate.getText().toString();

                if (dateStr.isEmpty()) {
                    Toast.makeText(RemindersActivity.this, "Please select a date", Toast.LENGTH_SHORT).show();
                    return;
                }

                Calendar calSet = getCalendar(dateStr, timeStr);
                Calendar now = Calendar.getInstance();

                if (calSet.before(now)) {
                    Toast.makeText(RemindersActivity.this, "Cannot set a reminder in the past!", Toast.LENGTH_SHORT).show();
                    return;
                }

                int requestCode = (int) (System.currentTimeMillis() / 1000);
                
                String[] dParts = dateStr.split("/");
                String displayDate = dParts[0] + "/" + dParts[1] + " at " + timeStr;
                
                addNewReminderToScreen(type, type + " reminder on " + displayDate, requestCode);

                saveReminderToStorage(requestCode, type, dateStr, timeStr);
                setAlarm(calSet, "Time for your " + type + "!", requestCode);

                dialog.dismiss();
            }
        });
    }

    /**
     * Adds a new reminder row to the screen, including a category header if necessary.
     *
     * @param category    The category of the reminder (e.g., "Recordings").
     * @param displayInfo Text description of the reminder.
     * @param requestCode Unique identifier for the reminder.
     * @return The request code of the added reminder.
     */
    private int addNewReminderToScreen(String category, String displayInfo, final int requestCode) {
        if (reminderContainer.findViewWithTag(category) == null) {
            View headerView = getLayoutInflater().inflate(R.layout.item_category_header, reminderContainer, false);
            TextView txtCategoryTitle = headerView.findViewById(R.id.txtCategoryTitle);
            txtCategoryTitle.setText(category);
            headerView.setTag(category);
            reminderContainer.addView(headerView);
        }

        View rowView = getLayoutInflater().inflate(R.layout.item_reminder_row, reminderContainer, false);
        rowView.setTag(requestCode);

        TextView rowTitle = rowView.findViewById(R.id.rowTitle);
        TextView rowSubtitle = rowView.findViewById(R.id.rowSubtitle);
        ImageView rowIcon = rowView.findViewById(R.id.rowIcon);
        ImageView btnDelete = rowView.findViewById(R.id.deleteReminder);

        rowTitle.setText(category);
        rowSubtitle.setText(displayInfo);

        rowIcon.setImageResource(category.equals("Simulations") ? R.drawable.ic_simulation_outline : R.drawable.ic_recording_outline);

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAlarm(requestCode);
                removeFromStorage(requestCode);
                reminderContainer.removeView(rowView);
                cleanUpHeader(category);
                Toast.makeText(RemindersActivity.this, "Reminder Deleted", Toast.LENGTH_SHORT).show();
            }
        });

        reminderContainer.addView(rowView);
        return requestCode;
    }

    /**
     * Schedules a system alarm for the specified time.
     *
     * @param calSet      The time to trigger the alarm.
     * @param text        The notification text.
     * @param requestCode Unique identifier for the alarm.
     */
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

    /**
     * Cancels an existing scheduled alarm.
     *
     * @param requestCode Unique identifier of the alarm to cancel.
     */
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

    /**
     * Removes a category header from the UI if it no longer contains any reminder rows.
     *
     * @param category The category header to check.
     */
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

    /**
     * Shows a {@link DatePickerDialog} to select a date for the reminder.
     *
     * @param dateField The EditText to populate with the selected date.
     */
    private void showDatePicker(final EditText dateField) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                dateField.setText(selectedDate);
            }
        }, year, month, day);
        
        // Disable selecting past dates in the picker
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    /**
     * Converts date and time strings into a {@link Calendar} object.
     *
     * @param dStr Date string in "dd/MM/yyyy" format.
     * @param tStr Time string in "HH:mm" format.
     * @return A configured Calendar instance.
     */
    private Calendar getCalendar(String dStr, String tStr) {
        String[] d = dStr.split("/");
        String[] t = tStr.split(":");
        Calendar cal = Calendar.getInstance();
        cal.set(Integer.parseInt(d[2]), Integer.parseInt(d[1]) - 1, Integer.parseInt(d[0]), Integer.parseInt(t[0]), Integer.parseInt(t[1]), 0);
        return cal;
    }

    /**
     * Removes a reminder row from the UI container based on its request code.
     *
     * @param code The request code of the reminder to remove.
     */
    private void removeRowByCode(int code) {
        for (int i = 0; i < reminderContainer.getChildCount(); i++) {
            View child = reminderContainer.getChildAt(i);
            if (child.getTag() != null && child.getTag().equals(code)) {
                TextView titleTv = child.findViewById(R.id.rowTitle);
                String category = (titleTv != null) ? titleTv.getText().toString() : "";

                reminderContainer.removeView(child);
                removeFromStorage(code);
                cleanUpHeader(category);
                break;
            }
        }
    }

    /**
     * Persists reminder data into local {@link SharedPreferences}.
     *
     * @param code     Unique ID of the reminder.
     * @param category Type of reminder.
     * @param date     Selected date.
     * @param time     Selected time.
     */
    private void saveReminderToStorage(int code, String category, String date, String time) {
        SharedPreferences sp = getSharedPreferences("RemindersLog", MODE_PRIVATE);
        String currentData = sp.getString("list", "");
        // Format: code|category|date|time#
        String newData = currentData + code + "|" + category + "|" + date + "|" + time + "#";
        sp.edit().putString("list", newData).apply();
    }

    /**
     * Loads saved reminders from storage and displays them if they are still in the future.
     * Removes expired reminders from storage.
     */
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

            if (calItem.after(now)) {
                String[] dParts = p[2].split("/");
                String displayDate = dParts[0] + "/" + dParts[1] + " at " + p[3];
                addNewReminderToScreen(p[1], p[1] + " reminder on " + displayDate, Integer.parseInt(p[0]));
                remainingData.append(item).append("#");
            }
        }
        sp.edit().putString("list", remainingData.toString()).apply();
    }

    /**
     * Removes a specific reminder's data from {@link SharedPreferences}.
     *
     * @param code The unique identifier of the reminder to remove.
     */
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

    /**
     * Registers the UI update receiver when the activity starts.
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("REMOVE_REMINDER_ROW");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(uiUpdater, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(uiUpdater, filter);
        }
    }

    /**
     * Unregisters the UI update receiver when the activity stops.
     */
    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(uiUpdater);
        } catch (IllegalArgumentException ignored) {}
    }

    /**
     * Reloads the reminders list when the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();
        reminderContainer.removeAllViews();
        loadReminders();
    }
}
