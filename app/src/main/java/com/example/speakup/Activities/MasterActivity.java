package com.example.speakup.Activities;

import android.os.Bundle;
import android.widget.Toast;

import com.example.speakup.Fragments.PracticeTopicsFragment;
import com.example.speakup.Fragments.QuickAccessFragment;
import com.example.speakup.R;
import com.example.speakup.Utilities;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;

public class MasterActivity extends Utilities {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Set QuickAccessFragment as the default when the app opens
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new QuickAccessFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new QuickAccessFragment();
            } else if (itemId == R.id.nav_practice) {
                selectedFragment = new PracticeTopicsFragment();
            } else if (itemId == R.id.nav_simulations) {
                Toast.makeText(this, "Simulations", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_recordings) {
                Toast.makeText(this, "Recordings", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }
}
