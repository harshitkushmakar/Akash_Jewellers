package com.kushmakar.akashjewellers;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.InputStream;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Firebase Imports
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Add these imports for HTTP requests
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboardActivity";

    // Replace this with your actual admin UID
    private static final String ADMIN_UID = "o46KW7WkicXnhqGFAaUmLuqfqWf1";

    private static final String PRICE_UPDATE_TOPIC = "price_updates";
    private EditText goldPriceEditText, silverPriceEditText, goldRTGSPriceEditText, silverRTGSPriceEditText;

    private TextView timestampTextView;
    AppCompatButton currentTimeButton, saveButton;
    private DatabaseReference priceUpdateNodeReference;

    // Firebase Auth
    private FirebaseAuth mAuth;

    // Executor for background network operations
    private ExecutorService executorService;
    private GoogleCredentials googleCredentials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is authenticated and is admin
        if (!isUserAdmin()) {
            Toast.makeText(this, "Access denied. Admin privileges required.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize executor service
        executorService = Executors.newFixedThreadPool(2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initializeFirebaseCredentials();
        // Initialize Firebase
        priceUpdateNodeReference = FirebaseDatabase.getInstance().getReference("price_updates");
        Log.d(TAG, "Firebase reference points to: " + priceUpdateNodeReference.toString());

        // Initialize UI elements
        goldPriceEditText = findViewById(R.id.goldPriceEditText);
        silverPriceEditText = findViewById(R.id.silverPriceEditText);
        goldRTGSPriceEditText = findViewById(R.id.goldRTGSPriceEditText);
        silverRTGSPriceEditText = findViewById(R.id.silverRTGSPriceEditText);
        timestampTextView = findViewById(R.id.timestampEditText);
        currentTimeButton = findViewById(R.id.currentTimeButton);
        saveButton = findViewById(R.id.saveButton);

        // Button listeners
        currentTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date currentDate = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault());
                String formattedDateTime = sdf.format(currentDate);
                timestampTextView.setText(formattedDateTime);
                Toast.makeText(AdminDashboardActivity.this, "Display time updated", Toast.LENGTH_SHORT).show();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOrUpdatePriceDataToFirebase();
            }
        });

        // Set initial display time
        SimpleDateFormat initialSdf = new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault());
        timestampTextView.setText(initialSdf.format(new Date()));
    }

    private boolean isUserAdmin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUID = currentUser.getUid();
            Log.d(TAG, "Current user UID: " + currentUID);
            Log.d(TAG, "Admin UID: " + ADMIN_UID);

            if (ADMIN_UID.equals(currentUID)) {
                Log.d(TAG, "User is authenticated as admin");
                return true;
            } else {
                Log.w(TAG, "User is authenticated but not admin");
                return false;
            }
        } else {
            Log.w(TAG, "No user is currently authenticated");
            return false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check authentication state when activity starts
        if (!isUserAdmin()) {
            Toast.makeText(this, "Access denied. Admin privileges required.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeFirebaseCredentials() {
        try {
            // Try to load from assets first
            InputStream serviceAccount = null;
            try {
                serviceAccount = getAssets().open("service-account.json");
                googleCredentials = GoogleCredentials
                        .fromStream(serviceAccount)
                        .createScoped(List.of("https://www.googleapis.com/auth/firebase.messaging"));
                Log.d(TAG, "Firebase Admin SDK credentials initialized successfully");
            } catch (IOException e) {
                Log.w(TAG, "service-account.json not found in assets, notifications will be disabled", e);
                // Continue without credentials - other functions will still work
                googleCredentials = null;
            } finally {
                if (serviceAccount != null) {
                    serviceAccount.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase Admin SDK credentials", e);
            googleCredentials = null;
        }
    }

    private void saveOrUpdatePriceDataToFirebase() {
        // Read values from EditText fields
        String goldPriceStr = goldPriceEditText.getText().toString().trim();
        String silverPriceStr = silverPriceEditText.getText().toString().trim();
        String goldRTGSPriceStr = goldRTGSPriceEditText.getText().toString().trim();
        String silverRTGSPriceStr = silverRTGSPriceEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(goldPriceStr) || TextUtils.isEmpty(silverPriceStr) ||
                TextUtils.isEmpty(goldRTGSPriceStr) || TextUtils.isEmpty(silverRTGSPriceStr)) {
            Toast.makeText(this, "Please fill in all price fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable save button to prevent multiple clicks
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        double goldPrice, silverPrice, goldRTGSPrice, silverRTGSPrice;
        try {
            goldPrice = Double.parseDouble(goldPriceStr);
            silverPrice = Double.parseDouble(silverPriceStr);
            goldRTGSPrice = Double.parseDouble(goldRTGSPriceStr);
            silverRTGSPrice = Double.parseDouble(silverRTGSPriceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "NumberFormatException: ", e);
            resetSaveButton();
            return;
        }

        // Prepare data for Firebase
        Map<String, Object> priceData = new HashMap<>();
        priceData.put("gold_price", goldPrice);
        priceData.put("silver_price", silverPrice);
        priceData.put("gold_rtgs_price", goldRTGSPrice);
        priceData.put("silver_rtgs_price", silverRTGSPrice);
        priceData.put("timestamp", ServerValue.TIMESTAMP);
        priceData.put("display_time", timestampTextView.getText().toString());

        Log.d(TAG, "Attempting to save data: " + priceData.toString());

        // Save to Firebase with better error handling
        priceUpdateNodeReference.setValue(priceData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(AdminDashboardActivity.this, "Prices updated successfully!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Data updated successfully");

                        // Send notification only if credentials are available
                        if (googleCredentials != null) {
                            sendPriceUpdateNotification(goldPrice, silverPrice, goldRTGSPrice, silverRTGSPrice);
                        } else {
                            Log.w(TAG, "Skipping notification - no credentials");
                        }

                        // Clear fields after successful save
                        clearFields();
                        resetSaveButton();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AdminDashboardActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed to update data", e);
                        resetSaveButton();
                    }
                });
    }

    private void resetSaveButton() {
        saveButton.setEnabled(true);
        saveButton.setText("Save Prices");
    }

    private void clearFields() {
        goldPriceEditText.setText("");
        silverPriceEditText.setText("");
        goldRTGSPriceEditText.setText("");
        silverRTGSPriceEditText.setText("");
    }

    private void sendPriceUpdateNotification(double goldPrice, double silverPrice, double goldRTGSPrice, double silverRTGSPrice) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get access token
                    String accessToken = getAccessToken();
                    if (accessToken == null) {
                        runOnUiThread(() -> Toast.makeText(AdminDashboardActivity.this,
                                "Failed to get access token", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    // Create notification payload using FCM v1 API format
                    JSONObject messagePayload = new JSONObject();
                    JSONObject message = new JSONObject();
                    JSONObject notification = new JSONObject();
                    JSONObject data = new JSONObject();

                    // Notification content
                    notification.put("title", "✨ रेट बदला है... रिश्ते नहीं!\n" +
                            "आकाश ज्वेलर्स पर आज का सोना-चांदी रेट देखें");
                    notification.put("body", "सोना: ₹" + goldPrice + " | चाँदी: ₹" + silverPrice  );

                    // Additional data
                    data.put("type", "price_update");
                    data.put("gold_price", String.valueOf(goldPrice));
                    data.put("silver_price", String.valueOf(silverPrice));
                    data.put("gold_rtgs_price", String.valueOf(goldRTGSPrice));
                    data.put("silver_rtgs_price", String.valueOf(silverRTGSPrice));
                    data.put("timestamp", String.valueOf(System.currentTimeMillis()));

                    // Target all users subscribed to the topic
                    message.put("topic", PRICE_UPDATE_TOPIC);
                    message.put("notification", notification);
                    message.put("data", data);

                    // Android-specific configuration
                    JSONObject android = new JSONObject();
                    JSONObject androidNotification = new JSONObject();
                    androidNotification.put("icon", "ic_notifications");
                    androidNotification.put("sound", "default");
                    androidNotification.put("channel_id", "price_updates_channel");
                    android.put("notification", androidNotification);
                    message.put("android", android);

                    messagePayload.put("message", message);

                    // Send the notification
                    boolean success = sendFCMNotificationV1(messagePayload, accessToken);

                    // Update UI on main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                Toast.makeText(AdminDashboardActivity.this,
                                        "Notification sent to all users!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(AdminDashboardActivity.this,
                                        "Failed to send notification", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } catch (JSONException e) {
                    Log.e(TAG, "Error creating notification JSON", e);
                    runOnUiThread(() -> Toast.makeText(AdminDashboardActivity.this,
                            "Error creating notification", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private String getAccessToken() {
        try {
            if (googleCredentials != null) {
                googleCredentials.refreshIfExpired();
                AccessToken token = googleCredentials.getAccessToken();
                return token.getTokenValue();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting access token", e);
        }
        return null;
    }

    private boolean sendFCMNotificationV1(JSONObject messagePayload, String accessToken) {
        try {
            String projectId = "construction-based-application";
            URL url = new URL("https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set request method and headers
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Send the payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = messagePayload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Check response
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "FCM Response Code: " + responseCode);

            // Log response for debugging
            if (responseCode != HttpURLConnection.HTTP_OK) {
                try (InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        byte[] errorBytes = new byte[1024];
                        int bytesRead = errorStream.read(errorBytes);
                        String errorResponse = new String(errorBytes, 0, bytesRead, StandardCharsets.UTF_8);
                        Log.e(TAG, "FCM Error Response: " + errorResponse);
                    }
                }
            }

            return responseCode == HttpURLConnection.HTTP_OK;

        } catch (IOException e) {
            Log.e(TAG, "Error sending FCM notification", e);
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}