package com.kushmakar.akashjewellers;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    EditText SignupName, SignupPhoneNumber, SignupEmail, SignupPassword;
    AppCompatButton SignUpButton;
    ProgressBar progressBar;
    FirebaseDatabase database;
    DatabaseReference reference;
    FirebaseAuth firebaseAuth; // Declare FirebaseAuth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        SignupName = findViewById(R.id.name_register_text);
        SignupPhoneNumber = findViewById(R.id.phone_number_register);
        SignupEmail = findViewById(R.id.email_text);
        SignupPassword = findViewById(R.id.password_text);
        SignUpButton = findViewById(R.id.SignUp_button);
        progressBar = findViewById(R.id.progressbar_register);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        // Initialize Firebase Database and Auth references
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("users"); // Reference to store user details
        firebaseAuth = FirebaseAuth.getInstance(); // Get Firebase Auth instance

        // --- Sign Up Button Click Listener with Validation ---
        SignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get text from fields
                String name = SignupName.getText().toString().trim();
                String phoneNumber = SignupPhoneNumber.getText().toString().trim();
                String email = SignupEmail.getText().toString().trim();
                String password = SignupPassword.getText().toString().trim();

                // Validate input
                if (!validateInput(name, phoneNumber, email, password)) {
                    return;
                }

                // Show progress indicator
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }

                // Use Firebase Authentication to create the user
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                // Hide progress indicator
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }

                                if (task.isSuccessful()) {
                                    // User creation with Firebase Auth successful
                                    Log.d(TAG, "createUserWithEmail:success");
                                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                                    if (firebaseUser != null) {
                                        // Save user details (excluding password) to Realtime Database
                                        // Use the Firebase Auth UID as the key for the database entry
                                        UserClass userClass = new UserClass(name, phoneNumber, email); // Password removed from constructor
                                        reference.child(firebaseUser.getUid()).setValue(userClass)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d(TAG, "User details saved to database for UID: " + firebaseUser.getUid());
                                                    Toast.makeText(RegisterActivity.this, "Signup Successful", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(RegisterActivity.this, EmailLoginActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Failed to write user details to database", e);
                                                    Toast.makeText(RegisterActivity.this, "Signup successful, but failed to save details.", Toast.LENGTH_LONG).show();
                                                    // Decide how to handle this - maybe still proceed or show an error
                                                    Intent intent = new Intent(RegisterActivity.this, EmailLoginActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                });
                                    } else {
                                        // This case should ideally not happen if task.isSuccessful() is true
                                        Toast.makeText(RegisterActivity.this, "Signup failed: User not found after creation.", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Firebase user is null after successful creation task.");
                                    }

                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                    // Handle specific Firebase Auth exceptions (e.g., email already in use, weak password)
                                    Toast.makeText(RegisterActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    // You can add more specific error handling based on task.getException()
                                }
                            }
                        });
            }
        });
    }

    /**
     * Validates all input fields.
     * Sets errors on EditText fields if validation fails.
     *
     * @param name        Name input
     * @param phoneNumber Phone number input
     * @param email       Email input
     * @param password    Password input
     * @return true if all fields are valid, false otherwise.
     */
    private boolean validateInput(String name, String phoneNumber, String email, String password) {
        // 1. Validate Name
        if (TextUtils.isEmpty(name)) {
            SignupName.setError("Name cannot be empty");
            SignupName.requestFocus();
            return false;
        } else {
            SignupName.setError(null); // Clear error
        }

        // 2. Validate Phone Number
        if (TextUtils.isEmpty(phoneNumber)) {
            SignupPhoneNumber.setError("Phone number cannot be empty");
            SignupPhoneNumber.requestFocus();
            return false;
        } else if (phoneNumber.length() != 10) {
            SignupPhoneNumber.setError("Phone number must be 10 digits");
            SignupPhoneNumber.requestFocus();
            return false;
        } else if (!TextUtils.isDigitsOnly(phoneNumber)) { // Check if it contains only numbers
            SignupPhoneNumber.setError("Phone number must contain only digits");
            SignupPhoneNumber.requestFocus();
            return false;
        } else {
            SignupPhoneNumber.setError(null); // Clear error
        }

        // 3. Validate Email
        if (TextUtils.isEmpty(email)) {
            SignupEmail.setError("Email cannot be empty");
            SignupEmail.requestFocus();
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // Use Android's built-in email pattern matcher
            SignupEmail.setError("Enter a valid email address");
            SignupEmail.requestFocus();
            return false;
        } else {
            SignupEmail.setError(null); // Clear error
        }

        // 4. Validate Password (Firebase Auth has its own requirements, but basic check is good)
        if (TextUtils.isEmpty(password)) {
            SignupPassword.setError("Password cannot be empty");
            SignupPassword.requestFocus();
            return false;
        } else if (password.length() < 6) { // Firebase Auth minimum is 6 characters
            SignupPassword.setError("Password must be at least 6 characters");
            SignupPassword.requestFocus();
            return false;
        } else {
            SignupPassword.setError(null); // Clear error
        }

        // All validations passed
        return true;
    }

    // Removed checkEmailExists method as Firebase Auth handles email uniqueness check
}