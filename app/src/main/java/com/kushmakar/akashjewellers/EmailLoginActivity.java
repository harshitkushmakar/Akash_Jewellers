package com.kushmakar.akashjewellers;

import static com.google.firebase.appcheck.internal.util.Logger.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EmailLoginActivity extends AppCompatActivity {

    // UI Elements
    TextView createNewAccount;
    AppCompatButton Login;
    EditText LoginEmail;
    EditText LoginPassword;
    TextView ForgotPassword;
    CheckBox showPasswordCheckBox;
    ProgressBar progressBar; // Add progress bar for better UX

    // SharedPreferences removed - no longer needed
    private SharedPreferences sharedPreferences;

    // Define constants for user types and admin UID
    private static final String ADMIN_UID = "o46KW7WkicXnhqGFAaUmLuqfqWf1"; // Replace with your actual admin UID
    private static final String USER_TYPE_ADMIN = "admin";
    private static final String USER_TYPE_REGULAR = "user";

    // Timeout and performance constants
    private static final long LOGIN_TIMEOUT = 15000; // 15 seconds timeout
    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable;

    // Declare FirebaseAuth
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, navigate to appropriate activity
            navigateToAppropriateActivity(currentUser.getUid());
            return;
        }

        // Set the layout
        setContentView(R.layout.activity_email_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find Views
        createNewAccount = findViewById(R.id.New_account);
        Login = findViewById(R.id.login);
        LoginEmail = findViewById(R.id.Login_email);
        ForgotPassword = findViewById(R.id.forgot_password);
        LoginPassword = findViewById(R.id.Login_password_text);
        showPasswordCheckBox = findViewById(R.id.checkbox_show_password);

        // Initialize progress bar (add this to your layout if not present)
        // progressBar = findViewById(R.id.progressBar);

        // Optimize Firebase Auth settings
        optimizeFirebaseAuth();

        // Listener for Show Password CheckBox
        showPasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    LoginPassword.setTransformationMethod(null); // Show password
                } else {
                    LoginPassword.setTransformationMethod(PasswordTransformationMethod.getInstance()); // Hide password
                }
                LoginPassword.setSelection(LoginPassword.getText().length()); // Move cursor to the end
            }
        });

        // Listener for Login Button
        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check network connection first
                if (!isNetworkAvailable()) {
                    showError("No internet connection. Please check your network.");
                    return;
                }

                // Validate both email and password before proceeding
                if (validateEmail() && validatePassword()) {
                    loginUser(); // Single login method for all users
                }
            }
        });

        ForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showForgotPasswordDialog();
            }
        });

        // Listener for Create New Account Button
        createNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EmailLoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    // Optimize Firebase Auth settings
    private void optimizeFirebaseAuth() {
        // Set language code for localized error messages
        firebaseAuth.setLanguageCode("en");

        // Enable Firebase Database persistence if using Realtime Database
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.d(TAG, "Firebase persistence already enabled or not available");
        }
    }

    // Check network connectivity
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // --- Validation Methods ---
    public boolean validateEmail() {
        String val = LoginEmail.getText().toString().trim();
        if (val.isEmpty()) {
            LoginEmail.setError("Email cannot be Empty");
            LoginEmail.requestFocus();
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(val).matches()) {
            LoginEmail.setError("Please enter a valid email address");
            LoginEmail.requestFocus();
            return false;
        } else {
            LoginEmail.setError(null);
            return true;
        }
    }

    public boolean validatePassword() {
        String val = LoginPassword.getText().toString().trim();
        if (val.isEmpty()) {
            LoginPassword.setError("Password cannot be Empty");
            LoginPassword.requestFocus();
            return false;
        } else if (val.length() < 6) {
            LoginPassword.setError("Password must be at least 6 characters");
            LoginPassword.requestFocus();
            return false;
        } else {
            LoginPassword.setError(null);
            return true;
        }
    }

    // --- Optimized Login Method ---
    public void loginUser() {
        String enteredEmail = LoginEmail.getText().toString().trim();
        String enteredPassword = LoginPassword.getText().toString().trim();

        // Show loading state
        setLoadingState(true);

        // Set up timeout handling
        timeoutRunnable = () -> {
            setLoadingState(false);
            showError("Login timeout. Please try again.");
        };
        timeoutHandler.postDelayed(timeoutRunnable, LOGIN_TIMEOUT);

        // Use Firebase Authentication for both admin and regular users
        firebaseAuth.signInWithEmailAndPassword(enteredEmail, enteredPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Cancel timeout
                        timeoutHandler.removeCallbacks(timeoutRunnable);

                        // Reset loading state
                        setLoadingState(false);

                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                String userUID = user.getUid();
                                navigateToAppropriateActivity(userUID);
                            } else {
                                showError("Login Failed: User data not available.");
                            }
                        } else {
                            // Handle login failure
                            handleLoginError(task.getException());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    setLoadingState(false);
                    handleLoginError(e);
                });
    }

    // Navigate to appropriate activity based on user type
    private void navigateToAppropriateActivity(String userUID) {
        Intent intent;

        if (ADMIN_UID.equals(userUID)) {
            // Admin Login - Navigate to AdminDashboardActivity
            intent = new Intent(EmailLoginActivity.this, AdminDashboardActivity.class);
        } else {
            // Regular User Login - Navigate to MainActivity
            intent = new Intent(EmailLoginActivity.this, MainActivity.class);
            intent.putExtra("USER_UID", userUID);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // Handle login errors with better error messages
    private void handleLoginError(Exception exception) {
        Log.w(TAG, "signInWithEmail:failure", exception);

        if (exception != null) {
            String errorMessage = exception.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("password") || errorMessage.contains("credential") ||
                        errorMessage.contains("INVALID_PASSWORD")) {
                    LoginPassword.setError("Incorrect password");
                    LoginPassword.requestFocus();
                } else if (errorMessage.contains("user") || errorMessage.contains("USER_NOT_FOUND") ||
                        errorMessage.contains("record")) {
                    LoginEmail.setError("Account not found");
                    LoginEmail.requestFocus();
                } else if (errorMessage.contains("network") || errorMessage.contains("NETWORK_ERROR")) {
                    showError("Network error. Please check your connection.");
                } else if (errorMessage.contains("too-many-requests") || errorMessage.contains("TOO_MANY_ATTEMPTS")) {
                    showError("Too many failed attempts. Please try again later.");
                } else if (errorMessage.contains("disabled") || errorMessage.contains("USER_DISABLED")) {
                    showError("This account has been disabled.");
                } else {
                    showError("Login failed. Please check your credentials.");
                }
            } else {
                showError("Login failed. Please try again.");
            }
        } else {
            showError("Login failed. Please try again.");
        }
    }

    // Set loading state for UI elements
    private void setLoadingState(boolean isLoading) {
        Login.setEnabled(!isLoading);
        Login.setText(isLoading ? "Logging in..." : "Login");

        // Show/hide progress bar if available
        // if (progressBar != null) {
        //     progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // }
    }

    // Show error message
    private void showError(String message) {
        Toast.makeText(EmailLoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    // Forgot password dialog
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(EmailLoginActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot, null);

        EditText emailBox = dialogView.findViewById(R.id.resetEmailEditText);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.resetButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String resetEmail = emailBox.getText().toString().trim();

                if (TextUtils.isEmpty(resetEmail) || !Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                    Toast.makeText(EmailLoginActivity.this, "Enter a valid registered email id", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Disable button during reset process
                view.setEnabled(false);

                firebaseAuth.sendPasswordResetEmail(resetEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        view.setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(EmailLoginActivity.this, "Password reset email sent", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            String errorMessage = "Unable to send password reset email. Please try again.";
                            if (task.getException() != null) {
                                String exceptionMessage = task.getException().getMessage();
                                if (exceptionMessage != null && exceptionMessage.contains("user")) {
                                    errorMessage = "No account found with this email address.";
                                }
                            }
                            Toast.makeText(EmailLoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        dialogView.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();
    }

    // This method would typically be called from a button in the Dashboard activities
    public static void performLogout(Context context) {
        // Sign out from Firebase Authentication
        FirebaseAuth.getInstance().signOut();

        // Redirect to Login screen
        Intent intent = new Intent(context, EmailLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up timeout handler
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Cancel any ongoing timeout when activity is paused
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }
}