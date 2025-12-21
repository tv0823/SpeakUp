package com.example.speakup;

import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Utilities extends AppCompatActivity {

    private NetworkChangeReceiver networkReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Initialize receiver with 'this' activity context
        networkReceiver = new NetworkChangeReceiver(this);
    }

    @Override

    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyEdgeToEdgeLogic();
    }

    private void applyEdgeToEdgeLogic() {
        EdgeToEdge.enable(this);
        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registering starts the automatic monitoring
        IntentFilter networkFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, networkFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Crucial: Unregister to avoid leaking the Activity context
        unregisterReceiver(networkReceiver);
    }
}