package com.example.speakup.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.speakup.Utils.NotificationHelper;

/**
 * BroadcastReceiver responsible for handling scheduled reminder alarms.
 * <p>
 * When an alarm triggers, this receiver:
 * <ul>
 *     <li>Displays a system notification using {@link NotificationHelper}.</li>
 *     <li>Removes the reminder from permanent storage.</li>
 *     <li>Sends a local broadcast to update the UI if the application is active.</li>
 * </ul>
 * </p>
 */
public class AlarmReceiver extends BroadcastReceiver {

    /**
     * Called when the BroadcastReceiver is receiving an Intent broadcast.
     * Extracts the message and request code from the intent to trigger the notification and cleanup.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received, containing reminder details.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String msg = intent.getStringExtra("text");
        int requestCode = intent.getIntExtra("requestCode", -1);

        NotificationHelper.showNotification(context, msg);

        // Remove from Storage
        removeFromPermanentStorage(context, requestCode);

        // Send broadcast for the Screen (in case the user IS looking at the screen)
        Intent updateUIIntent = new Intent("REMOVE_REMINDER_ROW");
        updateUIIntent.putExtra("requestCode", requestCode);
        updateUIIntent.setPackage(context.getPackageName());
        context.sendBroadcast(updateUIIntent);
    }

    /**
     * Removes a specific reminder entry from the permanent SharedPreferences storage.
     *
     * @param context The Context used to access SharedPreferences.
     * @param code    The unique request code identifying the reminder to remove.
     */
    private void removeFromPermanentStorage(Context context, int code) {
        SharedPreferences sp = context.getSharedPreferences("RemindersLog", Context.MODE_PRIVATE);
        String data = sp.getString("list", "");
        String[] items = data.split("#");
        StringBuilder newData = new StringBuilder();

        for (String item : items) {
            // If the item doesn't start with the ID we want to delete, keep it
            if (!item.isEmpty() && !item.startsWith(code + "|")) {
                newData.append(item).append("#");
            }
        }
        sp.edit().putString("list", newData.toString()).apply();
    }
}
