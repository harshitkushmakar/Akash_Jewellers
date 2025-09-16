package com.kushmakar.akashjewellers;



import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log; // <-- Import Log
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OtpActivity extends AppCompatActivity {

    private static final String TAG = "OtpActivity"; // Tag for logging

    AppCompatButton verifyButtonOnclick;
    EditText inputNumber1, inputNumber2, inputNumber3, inputNumber4, inputNumber5, inputNumber6;
    String getOtpBackend; // This is the Verification ID
    String mobileNumber;
    ProgressBar progressBarVerify;
    TextView resendOtpText;
    TextView change_number;
    private TextView waitingOtpTextView;
    private CountDownTimer countDownTimer;
    private final long startTimeInMillis = 60000;
    private final long intervalInMillis = 1000;
    private PhoneAuthProvider.ForceResendingToken forceResendingToken; // <-- Variable for the token

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        inputNumber1 = findViewById(R.id.otp_number_1);
        inputNumber2 = findViewById(R.id.otp_number_2);
        inputNumber3 = findViewById(R.id.otp_number_3);
        inputNumber4 = findViewById(R.id.otp_number_4);
        inputNumber5 = findViewById(R.id.otp_number_5);
        inputNumber6 = findViewById(R.id.otp_number_6);
        verifyButtonOnclick = findViewById(R.id.btnVerify);
        progressBarVerify = findViewById(R.id.progressbar_verify);
        resendOtpText = findViewById(R.id.resend_otp);
        change_number = findViewById(R.id.change_number);
        waitingOtpTextView = findViewById(R.id.waiting_otp);

        // Get data from intent
        getOtpBackend = getIntent().getStringExtra("verificationId"); // Verification ID
        mobileNumber = getIntent().getStringExtra("mobile");
        forceResendingToken = getIntent().getParcelableExtra("resendToken"); // <-- GET THE TOKEN

        // Log received data for debugging
        Log.d(TAG, "Received Verification ID: " + getOtpBackend);
        Log.d(TAG, "Received Mobile Number: " + mobileNumber);
        Log.d(TAG, "Received Resend Token: " + (forceResendingToken != null));


        // Set mobile number text
        TextView textView = findViewById(R.id.text_mobile_number);
        if (mobileNumber != null) {
            textView.setText(String.format("+91 | %s", mobileNumber));
        } else {
            textView.setText("+91 | Error");
            Toast.makeText(this, "Error: Mobile number not received.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Mobile number is null in Intent extras.");
        }
        if (getOtpBackend == null) {
            Log.e(TAG, "Verification ID (backendOtp) is null in Intent extras.");
            // Maybe show an error and prevent verification/resend
        }
        if (forceResendingToken == null) {
            Log.w(TAG, "Resend token is null in Intent extras. Resend might fail.");
            // Show a warning or handle appropriately. Resend might still work the first time sometimes,
            // but it's unreliable without the token.
        }

        // Verify button click listener (No changes needed here for resend)
        verifyButtonOnclick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ... (your existing verification logic - looks okay)
                // ... ensure you handle null getOtpBackend inside this listener too
                if (getOtpBackend == null) {
                    Toast.makeText(OtpActivity.this, "Verification process error. Please go back.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // ... rest of verification code ...
                if (!inputNumber1.getText().toString().trim().isEmpty() &&
                        !inputNumber2.getText().toString().trim().isEmpty() &&
                        !inputNumber3.getText().toString().trim().isEmpty() &&
                        !inputNumber4.getText().toString().trim().isEmpty() &&
                        !inputNumber5.getText().toString().trim().isEmpty() &&
                        !inputNumber6.getText().toString().trim().isEmpty()) {

                    String enterCodeOtp = inputNumber1.getText().toString() +
                            inputNumber2.getText().toString() +
                            inputNumber3.getText().toString() +
                            inputNumber4.getText().toString() +
                            inputNumber5.getText().toString() +
                            inputNumber6.getText().toString();


                    progressBarVerify.setVisibility(View.VISIBLE);
                    verifyButtonOnclick.setVisibility(View.INVISIBLE);

                    PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(getOtpBackend, enterCodeOtp);

                    FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            progressBarVerify.setVisibility(View.GONE);
                            verifyButtonOnclick.setVisibility(View.VISIBLE);

                            if (task.isSuccessful()) {
                                if (countDownTimer != null) {
                                    countDownTimer.cancel();
                                }
                                Toast.makeText(OtpActivity.this, "OTP Verified", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(OtpActivity.this, "Enter correct OTP", Toast.LENGTH_SHORT).show();
                                Log.w(TAG, "signInWithCredential failed", task.getException());
                            }
                        }
                    });

                } else {
                    Toast.makeText(OtpActivity.this, "Please Enter valid OTP", Toast.LENGTH_SHORT).show();
                }

            }
        });

        // Change number listener (No changes needed here for resend)
        change_number.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                // Go back to LoginActivity - Ensure LoginActivity handles being reopened correctly
                Intent intent = new Intent(OtpActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Prevent multiple instances
                startActivity(intent);
                finish();
            }
        });

        // Setup OTP input fields (No changes needed here for resend)
        numberOtpMoveForward();
        numberOtpMoveBack();

        // --- Resend OTP Click Listener ---
        resendOtpText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Resend OTP clicked."); // Log click

                if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
                    Toast.makeText(OtpActivity.this, "Cannot resend OTP: Mobile number missing.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Resend failed: mobileNumber is null or empty.");
                    return;
                }

                // Check if the token is available (it's crucial for resend)
                if (forceResendingToken == null) {
                    Toast.makeText(OtpActivity.this, "Cannot resend OTP: Resend token missing.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Resend failed: forceResendingToken is null.");
                    // You might want to direct the user back to LoginActivity in this case
                    // or attempt resend without token (which might fail silently or with an error)
                    return; // <-- IMPORTANT: Don't proceed without token
                }


                progressBarVerify.setVisibility(View.VISIBLE);
                resendOtpText.setEnabled(false);

                PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
                        new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                Log.d(TAG, "Resend: onVerificationCompleted.");
                                progressBarVerify.setVisibility(View.GONE);
                                resendOtpText.setEnabled(true);
                                // This might happen if auto-retrieval works
                                // You might want to sign in directly here
                                // signInWithPhoneAuthCredential(credential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Log.e(TAG, "Resend: onVerificationFailed", e);
                                progressBarVerify.setVisibility(View.GONE);
                                resendOtpText.setEnabled(true);
                                Toast.makeText(OtpActivity.this, "Resend failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                // Handle specific errors like quota exceeded, invalid request etc.
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId,
                                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                Log.d(TAG, "Resend: onCodeSent. New Verification ID: " + verificationId);
                                progressBarVerify.setVisibility(View.GONE);
                                resendOtpText.setEnabled(true);

                                Toast.makeText(OtpActivity.this, "OTP sent again", Toast.LENGTH_SHORT).show();

                                // *** IMPORTANT: UPDATE Verification ID and Resend Token ***
                                getOtpBackend = verificationId;
                                forceResendingToken = token; // <-- UPDATE THE STORED TOKEN

                                startTimer(); // Restart the timer
                            }
                        };

                // --- Build PhoneAuthOptions WITH the ForceResendingToken ---
                PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                        .setPhoneNumber("+91" + mobileNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(OtpActivity.this)
                        .setCallbacks(callbacks)
                        .setForceResendingToken(forceResendingToken) // <-- SET THE TOKEN HERE
                        .build();

                PhoneAuthProvider.verifyPhoneNumber(options); // Initiate resend request
            }
        });

        // Start the initial countdown timer only if required data is present
        if (mobileNumber != null && getOtpBackend != null) {
            startTimer();
        } else {
            Log.e(TAG, "Timer not started due to missing mobileNumber or verificationId.");
            // Optionally disable verify/resend buttons or show an error state
            waitingOtpTextView.setText("Error loading OTP screen.");
            resendOtpText.setVisibility(View.INVISIBLE);
        }

    } // --- End of onCreate ---

    // Timer method (No changes needed here for resend)
    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        resendOtpText.setVisibility(View.INVISIBLE);
        resendOtpText.setEnabled(false);
        waitingOtpTextView.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(startTimeInMillis, intervalInMillis) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished);
                waitingOtpTextView.setText(getString(R.string.waiting_for_otp_dynamic, secondsRemaining));
            }

            @Override
            public void onFinish() {
                waitingOtpTextView.setVisibility(View.INVISIBLE);
                resendOtpText.setText(getString(R.string.otp_timer_finished)); // Use correct string
                resendOtpText.setVisibility(View.VISIBLE);
                resendOtpText.setEnabled(true);
            }
        };
        countDownTimer.start();
        waitingOtpTextView.setText(getString(R.string.waiting_for_otp_dynamic, TimeUnit.MILLISECONDS.toSeconds(startTimeInMillis)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    // --- OTP Input Field Helper Methods (No changes needed here for resend) ---
    private void numberOtpMoveForward() {
        setupOtpInputListener(inputNumber1, inputNumber2);
        setupOtpInputListener(inputNumber2, inputNumber3);
        setupOtpInputListener(inputNumber3, inputNumber4);
        setupOtpInputListener(inputNumber4, inputNumber5);
        setupOtpInputListener(inputNumber5, inputNumber6);
    }

    private void numberOtpMoveBack() {
        setupOtpBackspaceListener(inputNumber6, inputNumber5);
        setupOtpBackspaceListener(inputNumber5, inputNumber4);
        setupOtpBackspaceListener(inputNumber4, inputNumber3);
        setupOtpBackspaceListener(inputNumber3, inputNumber2);
        setupOtpBackspaceListener(inputNumber2, inputNumber1);
    }

    private void setupOtpInputListener(final EditText currentEditText, final EditText nextEditText) {
        currentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (!charSequence.toString().trim().isEmpty() && nextEditText != null) {
                    nextEditText.requestFocus();
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void setupOtpBackspaceListener(final EditText currentEditText, final EditText previousEditText) {
        currentEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && currentEditText.getText().toString().isEmpty()) {
                    if (previousEditText != null) {
                        previousEditText.requestFocus();
                        return true;
                    }
                }
                return false;
            }
        });
    }
}