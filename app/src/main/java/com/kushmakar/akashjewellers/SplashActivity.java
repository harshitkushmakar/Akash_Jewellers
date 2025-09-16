package com.kushmakar.akashjewellers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox; // Import CheckBox
import android.widget.TextView;
import android.net.Uri;
import android.widget.Toast; // Import Toast

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import android.view.WindowManager;
import android.os.Build;
import android.os.Handler;

// Firebase Auth imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {


    private static final String PREFS_NAME = "LoginPrefs";
    private static final String PREF_STAY_SIGNED_IN = "staySignedIn";
    private static final String PREF_USER_TYPE = "userType";
    private static final String PREF_USER_IDENTIFIER = "userIdentifier";
    private static final String USER_TYPE_ADMIN = "admin";
    private static final String USER_TYPE_REGULAR = "user";
    // --------------------------------------------------------------------

    private SharedPreferences sharedPreferences;
    // Firebase Auth instance
    private FirebaseAuth mAuth;

    // UI elements
    private Button btnLogin;
    private Button btnRegister;
    private TextView privacyPolicyLink;
    private TextView termsLink; // Reference to Terms of Use link
    private CheckBox checkboxAgreePolicy; // Declare the CheckBox


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupStatusBar();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize SharedPreferences and Firebase Auth
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();


        btnLogin = findViewById(R.id.btn_login);

        privacyPolicyLink = findViewById(R.id.privacy_policy_link);
        termsLink = findViewById(R.id.terms_link); // Find terms link
        // Assuming the CheckBox ID is checkbox_agree_policy from previous XML modifications
        checkboxAgreePolicy = findViewById(R.id.privacy_checkbox); // Get reference to the checkbox


        // Hide buttons, links, and checkbox initially
        if (btnLogin != null) btnLogin.setVisibility(View.GONE);
        if (btnRegister != null) btnRegister.setVisibility(View.GONE);
        if (privacyPolicyLink != null) privacyPolicyLink.setVisibility(View.GONE);
        if (termsLink != null) termsLink.setVisibility(View.GONE);
        if (checkboxAgreePolicy != null) checkboxAgreePolicy.setVisibility(View.GONE);


        // Call navigateToAppropriateScreen() directly (no delay)
        navigateToAppropriateScreen();

    }


    private void setupStatusBar() {
        Window window = getWindow();
        window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(true);
        }
    }


    /**
     * Setup click listeners for all interactive elements (called after views are made visible)
     */
    private void setupClickListeners() {
        // Login button click listener
        if (btnLogin != null && checkboxAgreePolicy != null) { // Ensure both are found
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // --- Check CheckBox state ---
                    if (checkboxAgreePolicy.isChecked()) {
                        // Checkbox is checked, proceed to login
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Checkbox is NOT checked, show Toast
                        Toast.makeText(SplashActivity.this, "click on privacy policy", Toast.LENGTH_SHORT).show();
                    }
                    // ----------------------------
                }
            });
        }

        // Register button click listener
        if (btnRegister != null && checkboxAgreePolicy != null) { // Ensure both are found
            btnRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // --- Check CheckBox state ---
                    if (checkboxAgreePolicy.isChecked()) {
                        // Checkbox is checked, proceed to register
                        Intent intent = new Intent(SplashActivity.this, RegisterActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Checkbox is NOT checked, show Toast
                        Toast.makeText(SplashActivity.this, "click on privacy policy", Toast.LENGTH_SHORT).show();
                    }
                    // ----------------------------
                }
            });
        }

        // Privacy Policy link click listener
        if (privacyPolicyLink != null) {
            privacyPolicyLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = "https://sites.google.com/d/1pIVS_eKPGZTLau2I3qiEO5PC56o_iyPY/p/1YIGiU_9TRAJ9SsYO6-0amvrZyacUp_kk/edit"; // Replace with your actual URL
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            });
        }

        // Terms of Use link click listener
        if (termsLink != null) {
            termsLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = "YOUR_TERMS_OF_USE_URL"; // Replace with your actual Terms URL
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            });
        }
    }


    private void navigateToAppropriateScreen() {
        // --- Step 1: Check Firebase Auth state first (for regular users) ---
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is logged in via Firebase Auth - bypass policy check
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.putExtra("USER_UID", currentUser.getUid());
            startActivity(intent);
            finish();

        } else {
            // --- Step 2: If no Firebase user, check SharedPreferences for Admin "Stay Signed In" ---
            boolean staySignedInPref = sharedPreferences.getBoolean(PREF_STAY_SIGNED_IN, false);
            String userType = sharedPreferences.getString(PREF_USER_TYPE, null);
            String userIdentifier = sharedPreferences.getString(PREF_USER_IDENTIFIER, null);

            if (staySignedInPref && USER_TYPE_ADMIN.equals(userType)) {
                // Admin user auto-login - bypass policy check
                Intent intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
                finish();

            } else {
                // --- Step 3: No auto-login - Show UI immediately ---
                // Make the buttons, links, and checkbox visible
                if (btnLogin != null) btnLogin.setVisibility(View.VISIBLE);
                if (btnRegister != null) btnRegister.setVisibility(View.VISIBLE);
                if (privacyPolicyLink != null) privacyPolicyLink.setVisibility(View.VISIBLE);
                if (termsLink != null) termsLink.setVisibility(View.VISIBLE);
                if (checkboxAgreePolicy != null) checkboxAgreePolicy.setVisibility(View.VISIBLE);


                // Setup click listeners for visible elements
                setupClickListeners();

                // Buttons are enabled by default, the click listener will check the checkbox state
            }
        }
    }


    private void clearStaySignedInPreference() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PREF_STAY_SIGNED_IN);
        editor.remove(PREF_USER_TYPE);
        editor.remove(PREF_USER_IDENTIFIER);
        editor.apply();
    }
}