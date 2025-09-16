package com.kushmakar.akashjewellers;


import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kushmakar.akashjewellers.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PRICE_UPDATE_TOPIC = "price_updates";

    ActivityMainBinding binding;

    // Firebase Authentication
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge should be enabled before setting the content view
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot()); // Set the root view of the inflated binding

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Set the initial fragment
        replaceFragment(new UserDashboardFragment());

        // Set background to null if needed (as per your original code)
        binding.bottomNavigationView.setBackground(null);

        // Set up the item selected listener for the bottom navigation
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.Home_menu) {
                replaceFragment(new UserDashboardFragment());
                return true;
            } else if (id == R.id.Payment_menu) {
                replaceFragment(new PaymentFragment());
                return true;
            } else if (id == R.id.Contact_menu) {
                replaceFragment(new ContactFragment());
                return true;
            } else if (id == R.id.Gallery_menu) {
                replaceFragment(new GalleryFragment());
                return true;
            } else if (id == R.id.Logout_menu) {
                // 🔥 SHOW LOGOUT CONFIRMATION DIALOG
                showLogoutConfirmationDialog();
                // Return false so the logout menu item doesn't get selected
                return false;
            }

            return false;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Removed systemBars.bottom
            return insets;
        });


    }

    // Method to replace fragments in the frame_layout
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // Ensure R.id.frame_layout is the ID of the FrameLayout in your activity_main.xml
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    // 🔥 LOGOUT CONFIRMATION DIALOG
    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout from your account?");
        builder.setIcon(android.R.drawable.ic_dialog_alert);

        // YES - LOGOUT BUTTON
        builder.setPositiveButton("Yes, Logout", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                performLogout();
            }
        });

        // CANCEL BUTTON
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Keep the current selected tab (don't change bottom navigation)
            }
        });

        // Make dialog non-cancelable by back button (optional)
        builder.setCancelable(true);

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 🔥 PERFORM ACTUAL LOGOUT
    private void performLogout() {
        try {
            // Show loading message (optional)
            Log.d(TAG, "Performing logout...");

            // Sign out from Firebase Authentication
            if (firebaseAuth != null) {
                firebaseAuth.signOut();
            }

            // Unsubscribe from Firebase messaging topics (optional but recommended)
            FirebaseMessaging.getInstance().unsubscribeFromTopic(PRICE_UPDATE_TOPIC)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Unsubscribed from price updates");
                        }
                    });



            // Show success message
            Toast.makeText(MainActivity.this, "Logged out successfully!", Toast.LENGTH_SHORT).show();

            // Navigate back to EmailLoginActivity
            Intent intent = new Intent(MainActivity.this, EmailLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Finish current activity
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error during logout: " + e.getMessage());
            Toast.makeText(MainActivity.this, "Logout failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }




    @Override
    protected void onResume() {
        super.onResume();

        // Check if user is still logged in when activity resumes
        if (firebaseAuth != null && firebaseAuth.getCurrentUser() == null) {
            // User is not logged in, redirect to login
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        binding = null;
    }
}