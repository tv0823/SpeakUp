package com.example.speakup.Activities;

import android.app.FragmentManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

import com.example.speakup.Fragments.TopicsFragment;
import com.example.speakup.Fragments.QuickAccessFragment;
import com.example.speakup.R;
import com.example.speakup.Utilities;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;

/**
 * The main activity that serves as the primary navigation hub for the application.
 * <p>
 * This activity manages a {@link BottomNavigationView} to switch between different
 * top-level fragments such as Home, Practice, and Past Recordings. It also handles
 * basic navigation state by clearing the fragment backstack when switching between tabs.
 * </p>
 */
public class MasterActivity extends Utilities {
    /**
     * Keeps track of the last time a navigation item was clicked to implement debouncing.
     */
    private long lastClickTime = 0;

    /**
     * Initializes the activity, sets the content view, and configures the bottom navigation.
     * <p>
     * If the activity is being created for the first time, it loads the default {@link QuickAccessFragment}.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}. <b>Note: Otherwise it is null.</b>
     */
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
            // Prevent rapid multiple clicks (debounce)
            if (SystemClock.elapsedRealtime() - lastClickTime < 300) {
                return false;
            }
            lastClickTime = SystemClock.elapsedRealtime();

            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_home) {
                selectedFragment = new QuickAccessFragment();
            } else if (itemId == R.id.nav_practice) {
                selectedFragment = new TopicsFragment("Practice Topics");
            } else if (itemId == R.id.nav_simulations) {
                Toast.makeText(this, "Simulations", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_recordings) {
                selectedFragment = new TopicsFragment("Past Recordings");
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
            }

            if (selectedFragment != null) {
                // Clear the backstack when switching tabs via bottom nav to maintain a clean navigation state
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
