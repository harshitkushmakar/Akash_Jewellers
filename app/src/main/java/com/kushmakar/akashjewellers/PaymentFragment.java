package com.kushmakar.akashjewellers;



import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kushmakar.akashjewellers.databinding.FragmentPaymentBinding; // Generated binding class

public class PaymentFragment extends Fragment {

    private FragmentPaymentBinding binding; // View binding object

    public PaymentFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment using view binding
        binding = FragmentPaymentBinding.inflate(inflater, container, false);
        return binding.getRoot(); // Return the root view
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Setup Copy Functionality ---
        setupAllBanksCopyFunctionality();
        setupMobileNumberCopy();

        // Any other view-related setup for the payment fragment can go here
    }

    private void setupAllBanksCopyFunctionality() {
        // Setup copy functionality for Union Bank
        setupBankCopyFunctionality(
                binding.unionAcNameValue, binding.unionAcNumberValue, binding.unionIfscValue, binding.unionBranchValue,
                binding.copyAcName, binding.copyAcNumber, binding.copyIfsc, binding.copyBranch,
                "Union Bank" // Bank name prefix for clipboard label
        );

        // Setup copy functionality for SBI Bank
        setupBankCopyFunctionality(
                binding.sbiAcNameValue, binding.sbiAcNumberValue, binding.sbiIfscValue, binding.sbiBranchValue,
                binding.copySbiAcName, binding.copySbiAcNumber, binding.copySbiIfsc, binding.copySbiBranch,
                "SBI" // Bank name prefix for clipboard label
        );
    }

    // Method to set up copy functionality for a specific bank's details
    private void setupBankCopyFunctionality(
            TextView acNameValue, TextView acNumberValue, TextView ifscValue, TextView branchValue,
            ImageView copyAcNameIcon, ImageView copyAcNumberIcon, ImageView copyIfscIcon, ImageView copyBranchIcon,
            String bankName) {

        // Set click listeners only if views are found (using binding implies they should exist if IDs are correct)
        if (copyAcNameIcon != null && acNameValue != null) {
            copyAcNameIcon.setOnClickListener(v -> copyToClipboard(bankName + " Account Name", acNameValue.getText().toString()));
        }
        if (copyAcNumberIcon != null && acNumberValue != null) {
            copyAcNumberIcon.setOnClickListener(v -> copyToClipboard(bankName + " Account Number", acNumberValue.getText().toString()));
        }
        if (copyIfscIcon != null && ifscValue != null) {
            copyIfscIcon.setOnClickListener(v -> copyToClipboard(bankName + " IFSC Code", ifscValue.getText().toString()));
        }
        if (copyBranchIcon != null && branchValue != null) {
            copyBranchIcon.setOnClickListener(v -> copyToClipboard(bankName + " Branch", branchValue.getText().toString()));
        }
    }

    // Method to set up copy functionality for the mobile number in the QR section
    private void setupMobileNumberCopy() {
        TextView mobileNumberTextView = binding.mobileNumber;
        ImageView copyMobileIcon = binding.copyMobileNumber;

        if (mobileNumberTextView != null && copyMobileIcon != null) {
            copyMobileIcon.setOnClickListener(v -> copyToClipboard("Mobile Number", mobileNumberTextView.getText().toString()));
        }
    }


    private void copyToClipboard(String label, String text) {
        // Use requireActivity() or requireContext() to get system service context
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(requireActivity(), "Clipboard service not available.", Toast.LENGTH_SHORT).show();
            return; // Exit if service is not available
        }
        // Create ClipData object
        ClipData clip = ClipData.newPlainText(label, text);
        // Set the primary clip on the clipboard
        clipboard.setPrimaryClip(clip);

        // Show a confirmation toast message
        Toast.makeText(requireActivity(), label + " copied!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up the binding object
        binding = null;
    }
}