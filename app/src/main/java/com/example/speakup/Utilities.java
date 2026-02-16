package com.example.speakup;

import static com.example.speakup.FBRef.refAuth;

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
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

        View root = findViewById(R.id.main);

        // Safety check for the root view
        if (root == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // CHECK: Is this the MasterActivity?
            if (this instanceof MasterActivity) {
                // Logic for MasterActivity (Bottom nav bar bleeds to bottom)
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

                BottomNavigationView navBar = findViewById(R.id.bottom_navigation);
                if (navBar != null) {
                    // We pad the NavBar specifically so icons stay above the system line
                    navBar.setPadding(0, 0, 0, systemBars.bottom);
                }
            } else {
                // Logic for all other activities (Standard "Older" padding)
                // This applies padding to the whole root, preventing a "gap" in the nav bar
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            }

            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registering starts the automatic monitoring
        IntentFilter networkFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, networkFilter);

        if (this instanceof WelcomeScreenActivity ||
                this instanceof LogInActivity ||
                this instanceof SignUpActivity) {
            return;
        }
        // Check if user is logged in and redirect if not
        if (refAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, WelcomeScreenActivity.class);
            startActivity(intent);
            finish();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Crucial: Unregister to avoid leaking the Activity context
        unregisterReceiver(networkReceiver);
    }

    public static Bitmap getRotateBitmap(String photoPath, Bitmap bitmap) {
        try {
            ExifInterface ei = new ExifInterface(photoPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            //Clockwise rotation
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

    // Overloaded method for Gallery Uris
    public static Bitmap getRotateBitmap(Context context, Uri uri, Bitmap bitmap) {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) return bitmap;

            ExifInterface ei = new ExifInterface(input);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            //Clockwise rotation
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

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static void saveBitmapToFile(Bitmap bitmap, String filePath) {
        File file = new File(filePath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            // Compress the bitmap to the file
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