package com.kushmakar.akashjewellers;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import com.google.android.play.core.integrity.IntegrityTokenResponse;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    EditText EnterName, EnterNumber;
    AppCompatButton GetOtp;
    ProgressBar progressBar;

    LinearLayout emailBtn;
    FirebaseAuth mAuth;
    PhoneAuthProvider.ForceResendingToken resendToken;
    String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Initialize Firebase App Check ---
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        mAuth = FirebaseAuth.getInstance();

        EnterName = findViewById(R.id.enter_name);
        EnterNumber = findViewById(R.id.phoneNumber);
        GetOtp = findViewById(R.id.getOtpButton);
        progressBar = findViewById(R.id.progressbarGetOtp);
        emailBtn = findViewById(R.id.email_btn);

        emailBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, EmailLoginActivity.class);
            startActivity(intent);
        });

        GetOtp.setOnClickListener(view -> {
            String name = EnterName.getText().toString().trim();
            String mobileNumber = EnterNumber.getText().toString().trim();

            if (name.isEmpty()) {
                EnterName.setError("Name cannot be empty");
                EnterName.requestFocus();
                return;
            }

            if (mobileNumber.isEmpty()) {
                EnterNumber.setError("Mobile number cannot be empty");
                EnterNumber.requestFocus();
                return;
            } else if (mobileNumber.length() != 10) {
                EnterNumber.setError("Enter a valid 10-digit number");
                EnterNumber.requestFocus();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            GetOtp.setVisibility(View.INVISIBLE);

            prepareAndSendOTP(name, mobileNumber);
        });
    }

    private void prepareAndSendOTP(String name, String mobileNumber) {
        Log.d(TAG, "Starting Play Integrity check before sending OTP for: " + mobileNumber);

        IntegrityManager integrityManager = IntegrityManagerFactory.create(this);
        String nonce = UUID.randomUUID().toString();

        IntegrityTokenRequest request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .setCloudProjectNumber(335824851604L)
                .build();

        integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener(new OnSuccessListener<IntegrityTokenResponse>() {
                    @Override
                    public void onSuccess(IntegrityTokenResponse integrityTokenResponse) {
                        Log.d(TAG, "Play Integrity Token retrieved successfully.");
                        String integrityToken = integrityTokenResponse.token();

                        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
                                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                    @Override
                                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                        Log.d(TAG, "Phone verification completed automatically.");
                                        progressBar.setVisibility(View.GONE);
                                        GetOtp.setVisibility(View.VISIBLE);
                                        signInWithPhoneAuthCredential(credential);
                                    }

                                    @Override
                                    public void onVerificationFailed(@NonNull FirebaseException e) {
                                        Log.e(TAG, "Phone verification failed", e);
                                        progressBar.setVisibility(View.GONE);
                                        GetOtp.setVisibility(View.VISIBLE);
                                        Toast.makeText(LoginActivity.this, "Phone verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                        Log.d(TAG, "OTP code sent successfully.");
                                        progressBar.setVisibility(View.GONE);
                                        GetOtp.setVisibility(View.VISIBLE);
                                        LoginActivity.this.verificationId = verificationId;
                                        LoginActivity.this.resendToken = token;

                                        Intent intent = new Intent(getApplicationContext(), OtpActivity.class);
                                        intent.putExtra("mobile", mobileNumber);
                                        intent.putExtra("verificationId", verificationId);
                                        intent.putExtra("resendToken", token);
                                        startActivity(intent);
                                    }
                                };

                        String phoneNumberE164 = "+91" + mobileNumber;
                        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                                .setPhoneNumber(phoneNumberE164)
                                .setTimeout(60L, TimeUnit.SECONDS)
                                .setActivity(LoginActivity.this)
                                .setCallbacks(mCallbacks)
                                .build();

                        Log.d(TAG, "Calling verifyPhoneNumber.");
                        PhoneAuthProvider.verifyPhoneNumber(options);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Play Integrity API call failed: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    GetOtp.setVisibility(View.VISIBLE);
                    Toast.makeText(LoginActivity.this, "Security check failed. Cannot send OTP.", Toast.LENGTH_LONG).show();
                });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        Log.d(TAG, "Signing in with phone auth credential.");
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential successful.");
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "signInWithCredential failed: " + Objects.toString(task.getException(), "Unknown error"));
                        Toast.makeText(this, "Authentication failed: " + Objects.toString(task.getException() != null ? task.getException().getMessage() : "Unknown error", "Unknown error"), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
