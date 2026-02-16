package com.example.speakup.Activities;

import androidx.fragment.app.FragmentManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

import com.example.speakup.Fragments.ProfileFragment;
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

        if (savedInstanceState == null) {
            loadFragment(new QuickAccessFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            // 1. Debouncing is good, but let's keep it tight (250-300ms)
            if (SystemClock.elapsedRealtime() - lastClickTime < 300) {
                return false;
            }
            lastClickTime = SystemClock.elapsedRealtime();

            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new QuickAccessFragment();
            } else if (itemId == R.id.nav_practice) {
                selectedFragment = TopicsFragment.newInstance("Practice Topics");
            } else if (itemId == R.id.nav_simulations) {
                Toast.makeText(this, "Simulations", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_recordings) {
                selectedFragment = TopicsFragment.newInstance("Past Recordings");
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            return loadFragment(selectedFragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment == null) return false;

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            // If it's a TopicsFragment, check if the internal "type" is the same
            if (currentFragment instanceof TopicsFragment && fragment instanceof TopicsFragment) {
                String currentType = ((TopicsFragment) currentFragment).getType();
                String newType = ((TopicsFragment) fragment).getType();

                if (currentType != null && currentType.equals(newType)) {
                    return false; // Exactly the same screen and data, do nothing
                }
            } else {
                return false; // Same fragment class, no need to reload
            }
        }

        if (getSupportFragmentManager().isStateSaved()) return false;

        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .commit();

        return true;
    }
}
