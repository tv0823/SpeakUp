package com.example.speakup.Activities;

import android.app.FragmentManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

import com.example.speakup.Fragments.PracticeTopicsFragment;
import com.example.speakup.Fragments.QuickAccessFragment;
import com.example.speakup.R;
import com.example.speakup.Utilities;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;

public class MasterActivity extends Utilities {
    private long lastClickTime = 0;

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
            if (SystemClock.elapsedRealtime() - lastClickTime < 300) {
                return false;
            }
            lastClickTime = SystemClock.elapsedRealtime();

            int itemId = item.getItemId();
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            Fragment selectedFragment = null;
            if (itemId == R.id.nav_home) {
                // Check if we are already on Home
                if (!(currentFragment instanceof QuickAccessFragment)) {
                    selectedFragment = new QuickAccessFragment();
                }
            } else if (itemId == R.id.nav_practice) {
                // Check if we are already on Practice
                if (!(currentFragment instanceof PracticeTopicsFragment)) {
                    selectedFragment = new PracticeTopicsFragment();
                }
            } else if (itemId == R.id.nav_simulations) {
                Toast.makeText(this, "Simulations", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_recordings) {
                Toast.makeText(this, "Recordings", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
            }

            if (selectedFragment != null) {
                // Clear the backstack when switching tabs via bottom nav
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }
}
