package com.kushmakar.akashjewellers;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.kushmakar.akashjewellers.databinding.FragmentUserDashboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserDashboardFragment extends Fragment {

    private static final String TAG = "UserDashboardFragment";
    private static final String PREF_LAST_LOGIN = "last_login";

    // Cache keys for SharedPreferences
    private static final String PREF_GOLD_PRICE = "cached_gold_price";
    private static final String PREF_SILVER_PRICE = "cached_silver_price";
    private static final String PREF_GOLD_RTGS_PRICE = "cached_gold_rtgs_price";
    private static final String PREF_SILVER_RTGS_PRICE = "cached_silver_rtgs_price";
    private static final String PREF_LAST_UPDATE_TIME = "cached_update_time";

    private FragmentUserDashboardBinding binding;
    private TextView tvGoldPrice, tvGoldUpdateTime;
    private TextView tvSilverPrice, tvSilverUpdateTime;
    private TextView tvGoldUpiPrice, tvGoldUpiUpdateTime;
    private TextView tvSilverUpiPrice, tvSilverUpiUpdateTime;
    private TextView scrollingText;
    private ImageButton ShareScreenshot;

    private ActivityResultLauncher<IntentSenderRequest> activityResultLauncher;
    ImageSlider imageSlider;

    private DatabaseReference priceUpdateNodeReference;
    private ValueEventListener priceValueEventListener;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private boolean isDataLoaded = false;

    public UserDashboardFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUserDashboardBinding.inflate(inflater, container, false);

        // INITIALIZE activityResultLauncher FIRST (before using it)
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() != RESULT_OK) {
                            Log.d(TAG, "Update flow failed! Result code: " + result.getResultCode());
                            // If the update is canceled or fails,
                            // you can request to start the update again.
                        } else {
                            Log.d(TAG, "Update flow succeeded!");
                        }
                    }
                });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("user_prefs", requireContext().MODE_PRIVATE);

        // Update the last login time
        updateLastLogin();

        // Initialize views
        initializeViews();

        // Show cached prices immediately while loading new data
        loadCachedPrices();

        // Initialize Firebase Database reference
        priceUpdateNodeReference = FirebaseDatabase.getInstance().getReference("price_updates");
        priceUpdateNodeReference.keepSynced(true);

        // Setup the real-time listener for price updates
        setupFirebaseListener();

        // Check for in-app update (activityResultLauncher is now initialized)
        checkForInAppUpdate();
    }

    private void initializeViews() {
        tvGoldPrice = binding.goldPrice;
        tvGoldUpdateTime = binding.goldUpdateTime;
        tvSilverPrice = binding.silverPrice;
        tvSilverUpdateTime = binding.silverUpdateTime;
        tvGoldUpiPrice = binding.goldUpiPrice;
        tvGoldUpiUpdateTime = binding.goldUpiUpdateTime;
        tvSilverUpiPrice = binding.silverUpiPrice;
        tvSilverUpiUpdateTime = binding.silverUpiUpdateTime;
        scrollingText = binding.ScrollingText;
        ShareScreenshot = binding.shareIcon;
        imageSlider = binding.imageSlider;
        scrollingText.setSelected(true);

        // Set click listener for screenshot sharing
        ShareScreenshot.setOnClickListener(v -> shareScreenshot());

        List<SlideModel> imageList = new ArrayList<>();
        imageList.add(new SlideModel(R.drawable.slider_holi, ScaleTypes.CENTER_CROP));
        imageList.add(new SlideModel(R.drawable.slider_4, ScaleTypes.CENTER_CROP));


        // Set images to slider
        imageSlider.setImageList(imageList);
    }

    private void shareScreenshot() {
        try {
            // Find the scrollable view (adjust this based on your layout structure)
            View scrollableView = findScrollableView(binding.getRoot());

            if (scrollableView != null) {
                Log.d(TAG, "Found scrollable view: " + scrollableView.getClass().getSimpleName());

                // Create bitmap from the scrollable content
                Bitmap screenshot = captureFullScrollableContent(scrollableView);

                if (screenshot != null) {
                    // Save screenshot to file
                    File imageFile = saveScreenshotToFile(screenshot);

                    if (imageFile != null) {
                        // Share the screenshot
                        shareImageFile(imageFile);
                    } else {
                        Toast.makeText(requireActivity(), "Failed to save screenshot", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireActivity(), "Failed to capture screenshot", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Fallback to regular screenshot
                Log.d(TAG, "No scrollable view found, using regular screenshot");
                View rootView = binding.getRoot();
                Bitmap screenshot = captureView(rootView);

                if (screenshot != null) {
                    File imageFile = saveScreenshotToFile(screenshot);
                    if (imageFile != null) {
                        shareImageFile(imageFile);
                    } else {
                        Toast.makeText(requireActivity(), "Failed to save screenshot", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireActivity(), "Failed to capture screenshot", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sharing screenshot", e);
            Toast.makeText(requireActivity(), "Error sharing screenshot: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Method to find scrollable view in the layout
    private View findScrollableView(View view) {
        if (view instanceof androidx.core.widget.NestedScrollView ||
                view instanceof android.widget.ScrollView ||
                view instanceof androidx.recyclerview.widget.RecyclerView) {
            return view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View scrollableChild = findScrollableView(viewGroup.getChildAt(i));
                if (scrollableChild != null) {
                    return scrollableChild;
                }
            }
        }
        return null;
    }

    // Method to capture full scrollable content
    private Bitmap captureFullScrollableContent(View scrollableView) {
        try {
            if (scrollableView instanceof androidx.core.widget.NestedScrollView) {
                return captureNestedScrollView((androidx.core.widget.NestedScrollView) scrollableView);
            } else if (scrollableView instanceof android.widget.ScrollView) {
                return captureScrollView((android.widget.ScrollView) scrollableView);
            } else {
                // Fallback to regular capture
                return captureView(scrollableView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error capturing scrollable content", e);
            return null;
        }
    }

    // Capture NestedScrollView content
    private Bitmap captureNestedScrollView(androidx.core.widget.NestedScrollView scrollView) {
        try {
            View child = scrollView.getChildAt(0);
            if (child == null) return null;

            int totalHeight = child.getHeight();
            int totalWidth = child.getWidth();

            Log.d(TAG, "NestedScrollView - Total Height: " + totalHeight + ", Width: " + totalWidth);

            // Create bitmap with full content size
            Bitmap bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // Fill with white background first
            canvas.drawColor(android.graphics.Color.argb(245, 255, 255, 255));

            // Save current scroll position
            int originalScrollY = scrollView.getScrollY();

            // Scroll to top and draw
            scrollView.scrollTo(0, 0);

            // Draw the child content
            canvas.save();
            child.draw(canvas);
            canvas.restore();

            // Restore scroll position
            scrollView.scrollTo(0, originalScrollY);

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error capturing NestedScrollView", e);
            return null;
        }
    }

    // Capture ScrollView content
    private Bitmap captureScrollView(android.widget.ScrollView scrollView) {
        try {
            View child = scrollView.getChildAt(0);
            if (child == null) return null;

            int totalHeight = child.getHeight();
            int totalWidth = child.getWidth();

            Log.d(TAG, "ScrollView - Total Height: " + totalHeight + ", Width: " + totalWidth);

            // Create bitmap with full content size
            Bitmap bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // Fill with white background first
            canvas.drawColor(android.graphics.Color.WHITE);

            // Save current scroll position
            int originalScrollY = scrollView.getScrollY();

            // Scroll to top and draw
            scrollView.scrollTo(0, 0);

            // Draw the child content
            canvas.save();
            child.draw(canvas);
            canvas.restore();

            // Restore scroll position
            scrollView.scrollTo(0, originalScrollY);

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error capturing ScrollView", e);
            return null;
        }
    }

    // Method to capture view as bitmap (full scrollable content)
    private Bitmap captureView(View view) {
        try {
            int totalHeight = 0;
            int totalWidth = view.getWidth();

            // Check if the view is scrollable
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;

                // Calculate total height including all children
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    if (child.getVisibility() != View.GONE) {
                        child.measure(
                                View.MeasureSpec.makeMeasureSpec(totalWidth, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        );
                        totalHeight += child.getMeasuredHeight();
                    }
                }

                // If calculated height is less than current height, use current height
                totalHeight = Math.max(totalHeight, view.getHeight());
            } else {
                totalHeight = view.getHeight();
            }

            Log.d(TAG, "Capturing screenshot - Width: " + totalWidth + ", Height: " + totalHeight);

            // Create bitmap with full content dimensions
            Bitmap bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // Save current scroll position
            int originalScrollX = 0;
            int originalScrollY = 0;

            if (view.canScrollVertically(1) || view.canScrollVertically(-1)) {
                if (view instanceof androidx.core.widget.NestedScrollView) {
                    androidx.core.widget.NestedScrollView scrollView = (androidx.core.widget.NestedScrollView) view;
                    originalScrollY = scrollView.getScrollY();
                    scrollView.scrollTo(0, 0); // Scroll to top
                } else if (view instanceof android.widget.ScrollView) {
                    android.widget.ScrollView scrollView = (android.widget.ScrollView) view;
                    originalScrollY = scrollView.getScrollY();
                    scrollView.scrollTo(0, 0); // Scroll to top
                }
            }

            // Draw the full content
            view.draw(canvas);

            // Restore original scroll position
            if (view instanceof androidx.core.widget.NestedScrollView) {
                androidx.core.widget.NestedScrollView scrollView = (androidx.core.widget.NestedScrollView) view;
                scrollView.scrollTo(originalScrollX, originalScrollY);
            } else if (view instanceof android.widget.ScrollView) {
                android.widget.ScrollView scrollView = (android.widget.ScrollView) view;
                scrollView.scrollTo(originalScrollX, originalScrollY);
            }

            Log.d(TAG, "Full content screenshot captured successfully");
            return bitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error capturing full content view", e);
            return null;
        }
    }

    // Method to save bitmap to file
    private File saveScreenshotToFile(Bitmap bitmap) {
        try {
            // Create filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = "akash_jewellers_prices_" + timestamp + ".png";

            // Create file in external cache directory (doesn't require WRITE_EXTERNAL_STORAGE permission)
            File imagesFolder = new File(requireContext().getExternalCacheDir(), "screenshots");
            if (!imagesFolder.exists()) {
                imagesFolder.mkdirs();
            }

            File imageFile = new File(imagesFolder, filename);

            // Save bitmap to file
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            Log.d(TAG, "Screenshot saved: " + imageFile.getAbsolutePath());
            return imageFile;

        } catch (IOException e) {
            Log.e(TAG, "Error saving screenshot", e);
            return null;
        }
    }

    // Method to share image file
    private void shareImageFile(File imageFile) {
        try {
            // Get URI using FileProvider
            Uri imageUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    imageFile
            );

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "आकाश ज्वैलर्स से नवीनतम सोने और चांदी की कीमतें https://play.google.com/store/apps/details?id=com.kushmakar.akashjewellers");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Akash Jewellers - Price Update");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Create chooser to let user select app
            Intent chooser = Intent.createChooser(shareIntent, "Share Price Screenshot");

            // Start the share activity
            if (shareIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(chooser);
                Log.d(TAG, "Share intent started successfully");
            } else {
                Toast.makeText(requireActivity(), "No apps available to share screenshot", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error sharing image file", e);
            Toast.makeText(requireActivity(), "Error sharing screenshot", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCachedPrices() {
        String cachedGoldPrice = sharedPreferences.getString(PREF_GOLD_PRICE, null);
        String cachedSilverPrice = sharedPreferences.getString(PREF_SILVER_PRICE, null);
        String cachedGoldRtgsPrice = sharedPreferences.getString(PREF_GOLD_RTGS_PRICE, null);
        String cachedSilverRtgsPrice = sharedPreferences.getString(PREF_SILVER_RTGS_PRICE, null);
        String cachedUpdateTime = sharedPreferences.getString(PREF_LAST_UPDATE_TIME, null);

        if (cachedGoldPrice != null && cachedSilverPrice != null) {
            // Show cached prices immediately
            tvGoldPrice.setText(cachedGoldPrice);
            tvSilverPrice.setText(cachedSilverPrice);
            tvGoldUpiPrice.setText(cachedGoldRtgsPrice != null ? cachedGoldRtgsPrice : "N/A");
            tvSilverUpiPrice.setText(cachedSilverRtgsPrice != null ? cachedSilverRtgsPrice : "N/A");

            // Set consistent update time format
            String displayTime = cachedUpdateTime != null ? "Updated: " + cachedUpdateTime : "Updated: --";
            tvGoldUpdateTime.setText(displayTime);
            tvSilverUpdateTime.setText(displayTime);
            tvGoldUpiUpdateTime.setText(displayTime);
            tvSilverUpiUpdateTime.setText(displayTime);

            Log.d(TAG, "Loaded cached prices");
        } else {
            showSkeletonState();
        }
    }

    private void showSkeletonState() {
        tvGoldPrice.setText("Loading...");
        tvSilverPrice.setText("Loading...");
        tvGoldUpiPrice.setText("Loading...");
        tvSilverUpiPrice.setText("Loading...");

        tvGoldUpdateTime.setText("Updated: --");
        tvSilverUpdateTime.setText("Updated: --");
        tvGoldUpiUpdateTime.setText("Updated: --");
        tvSilverUpiUpdateTime.setText("Updated: --");

        Log.d(TAG, "Showing skeleton loading state");
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) return "--";

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, hh:mm a", Locale.getDefault());
            Date resultDate = new Date(timestamp);
            return sdf.format(resultDate);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting timestamp", e);
            return "Invalid Date";
        }
    }

    private void cachePrices(PriceData prices) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setMinimumFractionDigits(2);

        String formattedTime = formatTimestamp(prices.getTimestamp());

        // Cache the formatted prices
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (prices.getGold_price() != null) {
            editor.putString(PREF_GOLD_PRICE, currencyFormat.format(prices.getGold_price()));
        }
        if (prices.getSilver_price() != null) {
            editor.putString(PREF_SILVER_PRICE, currencyFormat.format(prices.getSilver_price()));
        }
        if (prices.getGold_rtgs_price() != null) {
            editor.putString(PREF_GOLD_RTGS_PRICE, currencyFormat.format(prices.getGold_rtgs_price()));
        }
        if (prices.getSilver_rtgs_price() != null) {
            editor.putString(PREF_SILVER_RTGS_PRICE, currencyFormat.format(prices.getSilver_rtgs_price()));
        }

        editor.putString(PREF_LAST_UPDATE_TIME, formattedTime);
        editor.apply();

        Log.d(TAG, "Prices cached successfully");
    }

    private void checkForInAppUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(requireContext());

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                Log.d(TAG, "Update available! Starting update flow...");

                try {
                    // Request the update (activityResultLauncher is now properly initialized)
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activityResultLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build());
                } catch (Exception e) {
                    Log.e(TAG, "Error starting update flow", e);
                }
            } else {
                Log.d(TAG, "No update available or update not allowed");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking for updates", e);
        });
    }

    private void updateLastLogin() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_LAST_LOGIN, System.currentTimeMillis());
        editor.apply();
        Log.d(TAG, "Last login time updated.");
    }

    private void navigateToLoginActivity() {
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setupFirebaseListener() {
        priceValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    PriceData currentPrices = dataSnapshot.getValue(PriceData.class);
                    if (currentPrices != null) {
                        Log.d(TAG, "Data received: Gold=" + currentPrices.getGold_price() + ", Timestamp=" + currentPrices.getTimestamp());

                        // Cache the new prices
                        cachePrices(currentPrices);

                        // Update UI
                        updatePriceUI(currentPrices);

                        // Mark data as loaded
                        isDataLoaded = true;
                    } else {
                        Log.w(TAG, "PriceData object is null.");
                        if (!isDataLoaded) {
                            clearPriceUI("Data Error");
                        }
                    }
                } else {
                    Log.w(TAG, "price_updates node does not exist.");
                    if (!isDataLoaded) {
                        clearPriceUI("No Data");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read price data.", databaseError.toException());
                Toast.makeText(requireActivity(), "Failed to load price data.", Toast.LENGTH_SHORT).show();
                if (!isDataLoaded) {
                    clearPriceUI("Load Error");
                }
            }
        };

        priceUpdateNodeReference.addValueEventListener(priceValueEventListener);
        Log.d(TAG, "Firebase ValueEventListener attached.");
    }

    private void updatePriceUI(PriceData prices) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setMinimumFractionDigits(2);

        String formattedTime = formatTimestamp(prices.getTimestamp());
        String displayTime = "Updated: " + formattedTime;

        // Update all price fields
        tvGoldPrice.setText(prices.getGold_price() != null ? currencyFormat.format(prices.getGold_price()) : "N/A");
        tvGoldUpdateTime.setText(displayTime);

        tvSilverPrice.setText(prices.getSilver_price() != null ? currencyFormat.format(prices.getSilver_price()) : "N/A");
        tvSilverUpdateTime.setText(displayTime);

        tvGoldUpiPrice.setText(prices.getGold_rtgs_price() != null ? currencyFormat.format(prices.getGold_rtgs_price()) : "N/A");
        tvGoldUpiUpdateTime.setText(displayTime);

        tvSilverUpiPrice.setText(prices.getSilver_rtgs_price() != null ? currencyFormat.format(prices.getSilver_rtgs_price()) : "N/A");
        tvSilverUpiUpdateTime.setText(displayTime);

        Log.d(TAG, "UI updated with fresh data");
    }

    private void clearPriceUI(String message) {
        tvGoldPrice.setText(message);
        tvSilverPrice.setText(message);
        tvGoldUpiPrice.setText(message);
        tvSilverUpiPrice.setText(message);

        String placeholderTime = "Updated: --";
        tvGoldUpdateTime.setText(placeholderTime);
        tvSilverUpdateTime.setText(placeholderTime);
        tvGoldUpiUpdateTime.setText(placeholderTime);
        tvSilverUpiUpdateTime.setText(placeholderTime);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        Log.d(TAG, "onDestroyView: Binding nulled.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (priceUpdateNodeReference != null && priceValueEventListener != null) {
            priceUpdateNodeReference.removeEventListener(priceValueEventListener);
            Log.d(TAG, "Firebase ValueEventListener removed in onDestroy.");
        }
        Log.d(TAG, "onDestroy: Fragment destroyed.");
    }
}