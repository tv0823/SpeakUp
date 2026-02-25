package com.example.speakup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class AlarmReceiver extends BroadcastReceiver {

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
