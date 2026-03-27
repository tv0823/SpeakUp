package com.example.speakup.Utils;

import static com.example.speakup.Utils.FBRef.refAuth;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;

import com.example.speakup.Activities.LogInActivity;
import com.example.speakup.Activities.MasterActivity;
import com.example.speakup.Activities.SignUpActivity;
import com.example.speakup.Activities.WelcomeScreenActivity;
import com.example.speakup.Receivers.NetworkChangeReceiver;
import com.example.speakup.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utilities class providing common app-wide helper methods and base activity logic.
 * <p>
 * This class extends {@link AppCompatActivity} and implements features such as:
 * <ul>
 *     <li>Network connectivity monitoring via {@link NetworkChangeReceiver}</li>
 *     <li>Edge-to-edge layout handling for different activities</li>
 *     <li>User authentication checks</li>
 *     <li>Bitmap rotation based on EXIF orientation</li>
 *     <li>Saving bitmaps to files</li>
 * </ul>
 */
public class Utilities extends AppCompatActivity {

    /** Receiver for monitoring network connectivity changes */
    private NetworkChangeReceiver networkReceiver;

    /**
     * Called when the activity is first created.
     * Initializes network receiver and sets default night mode to off.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *                           shut down then this Bundle contains the data it most recently supplied.
     *                           Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Initialize network connectivity receiver
        networkReceiver = new NetworkChangeReceiver(this);
    }

    /**
     * Overrides default setContentView to include edge-to-edge layout handling.
     *
     * @param layoutResID The layout resource ID to be set as the content view
     */
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyEdgeToEdgeLogic();
    }

    /**
     * Applies edge-to-edge layout behavior for the activity.
     * <p>
     * - In {@link MasterActivity}, the bottom navigation bar is padded to avoid overlap with system bars.
     * - In other activities, system bar insets are applied to the root view.
     */
    private void applyEdgeToEdgeLogic() {
        EdgeToEdge.enable(this);

        View root = findViewById(R.id.main);

        if (root == null) return; // Safety check

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (this instanceof MasterActivity) {
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

                BottomNavigationView navBar = findViewById(R.id.bottom_navigation);
                if (navBar != null) {
                    navBar.setPadding(0, 0, 0, systemBars.bottom);
                }
            } else {
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            }

            return insets;
        });
    }

    /**
     * Called when the activity becomes visible.
     * Registers the network connectivity receiver and checks authentication.
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Register receiver for connectivity changes
        IntentFilter networkFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, networkFilter);

        // Skip auth check for login/welcome/signup screens
        if (this instanceof WelcomeScreenActivity ||
                this instanceof LogInActivity ||
                this instanceof SignUpActivity) {
            return;
        }

        // Redirect to WelcomeScreenActivity if user is not logged in
        if (refAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, WelcomeScreenActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Called when the activity is no longer visible.
     * Unregisters the network receiver to prevent memory leaks.
     */
    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkReceiver);
    }

    /**
     * Rotates a {@link Bitmap} based on the EXIF orientation of an image file.
     *
     * @param photoPath Absolute file path of the image
     * @param bitmap    Bitmap to rotate
     * @return Rotated bitmap or original if no rotation is needed
     */
    public static Bitmap getRotateBitmap(String photoPath, Bitmap bitmap) {
        try {
            ExifInterface ei = new ExifInterface(photoPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(bitmap, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(bitmap, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(bitmap, 270);
                default:
                    return bitmap;
            }
        } catch (IOException e) {
            return bitmap;
        }
    }

    /**
     * Rotates a {@link Bitmap} based on the EXIF orientation from a {@link Uri}.
     *
     * @param context Context to access the content resolver
     * @param uri     URI of the image
     * @param bitmap  Bitmap to rotate
     * @return Rotated bitmap or original if no rotation is needed
     */
    public static Bitmap getRotateBitmap(Context context, Uri uri, Bitmap bitmap) {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) return bitmap;

            ExifInterface ei = new ExifInterface(input);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(bitmap, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(bitmap, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(bitmap, 270);
                default:
                    return bitmap;
            }
        } catch (IOException e) {
            return bitmap;
        }
    }

    /**
     * Rotates a bitmap by a specified angle.
     *
     * @param source Bitmap to rotate
     * @param angle  Rotation angle in degrees (clockwise)
     * @return New rotated bitmap
     */
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Saves a {@link Bitmap} to a file in JPEG format.
     *
     * @param bitmap   Bitmap to save
     * @param filePath Absolute file path where the bitmap will be saved
     */
    public static void saveBitmapToFile(Bitmap bitmap, String filePath) {
        File file = new File(filePath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}