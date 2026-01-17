package com.example.speakup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * BroadcastReceiver that monitors changes in network connectivity.
 * <p>
 * This receiver detects when the device connects to or disconnects from the internet.
 * If the connection is lost, it displays a non-cancelable alert dialog to the user,
 * prompting them to reconnect.
 * </p>
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    /**
     * Static flag indicating the current connection status.
     */
    private static boolean isConnected = false;

    /**
     * Static reference to the network connection warning dialog.
     */
    private static AlertDialog networkDialog;

    /**
     * The activity context used to display the dialog.
     */
    private Activity activity;

    /**
     * Constructs a new NetworkChangeReceiver.
     *
     * @param activity The activity context where the dialog should be shown.
     */
    public NetworkChangeReceiver(Activity activity) {
        this.activity = activity;
    }

    /**
     * Called when the network state changes.
     * <p>
     * Checks the active network info and updates the {@link #isConnected} flag.
     * Displays or dismisses the warning dialog based on the connectivity status.
     * </p>
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = (activeNetwork != null && activeNetwork.isConnected());

        if (!isConnected) {
            showDialog();
        } else {
            dismissDialog();
        }
    }

    /**
     * Displays a dialog informing the user that the internet connection is lost.
     * <p>
     * The dialog provides a button to open the device's Wi-Fi settings.
     * It only shows if the activity is not finishing and a dialog is not already visible.
     * </p>
     */
    private void showDialog() {
        // Ensure we don't create multiple dialogs and the activity is still valid
        if ((networkDialog == null || !networkDialog.isShowing()) && !activity.isFinishing()) {
            networkDialog = new AlertDialog.Builder(activity)
                    .setTitle("Connection Lost")
                    .setMessage("SpeakUp requires internet connection. Please reconnect.")
                    .setCancelable(false)
                    .setPositiveButton("Settings", (dialog, which) -> {
                        activity.startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                    })
                    .create();
            networkDialog.show();
        }
    }

    /**
     * Dismisses the connectivity warning dialog if it is currently showing.
     */
    private void dismissDialog() {
        if (networkDialog != null && networkDialog.isShowing()) {
            networkDialog.dismiss();
            networkDialog = null;
        }
    }
}
