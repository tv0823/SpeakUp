package com.example.speakup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkChangeReceiver extends BroadcastReceiver {

    public static boolean isConnected = false;
    private static AlertDialog networkDialog;
    private Activity activity;

    public NetworkChangeReceiver(Activity activity) {
        this.activity = activity;
    }

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

    private void dismissDialog() {
        if (networkDialog != null && networkDialog.isShowing()) {
            networkDialog.dismiss();
            networkDialog = null;
        }
    }
}