package com.example.speakup.Utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/**
 * Utility class for managing and displaying system notifications.
 * <p>
 * This class handles the creation of notification channels (required for Android 8.0+)
 * and provides a static method to trigger simple text-based notifications.
 * </p>
 */
public class NotificationHelper {
    /**
     * The unique identifier for the notification channel.
     */
    private static final String CHANNEL_ID = "NotifCh1";

    /**
     * The user-visible name of the notification channel.
     */
    private static final String CHANNEL_NAME = "NotifCh1";

    /**
     * The unique identifier for notifications posted by this helper.
     */
    private static final int NOTIFICATION_ID = 1;

    /**
     * Displays a system notification with the provided message.
     * <p>
     * On Android O (API 26) and above, this method ensures the notification channel
     * is created before posting the notification.
     * </p>
     *
     * @param context The Context used to access system services and build the notification.
     * @param text    The message content to display in the notification.
     */
    public static void showNotification(Context context, String text) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notiBbuilder = new
                NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("SpeakUp Reminder")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationManager.notify(NOTIFICATION_ID, notiBbuilder.build());
    }
}
