package com.example.expensetracker.ui.onboarding;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.R;

import java.util.ArrayList;
import java.util.List;

public class Step3PermissionsFragment extends Fragment {

    private ImageView ivSmsStatus, ivNotifStatus;
    private Button btnGrantSms, btnGrantNotif, btnNext;

    private final ActivityResultLauncher<String[]> permissionLauncher =
        registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> updatePermissionStatus()
        );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboard_step3, container, false);

        ivSmsStatus   = view.findViewById(R.id.ivSmsStatus);
        ivNotifStatus = view.findViewById(R.id.ivNotifStatus);
        btnGrantSms   = view.findViewById(R.id.btnGrantSms);
        btnGrantNotif = view.findViewById(R.id.btnGrantNotif);
        btnNext       = view.findViewById(R.id.btnNext);

        btnGrantSms.setOnClickListener(v -> requestSmsPermission());
        btnGrantNotif.setOnClickListener(v -> requestNotifPermission());
        btnNext.setOnClickListener(v -> {
            if (!hasSmsPermission()) {
                Toast.makeText(requireContext(),
                    "SMS permission is required to auto-detect transactions.",
                    Toast.LENGTH_LONG).show();
                return;
            }
            OnboardingActivity host = (OnboardingActivity) requireActivity();
            host.showStep(new Step4LockSetupFragment());
        });

        updatePermissionStatus();
        return view;
    }

    private void requestSmsPermission() {
        permissionLauncher.launch(new String[]{
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        });
    }

    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(new String[]{
                Manifest.permission.POST_NOTIFICATIONS
            });
        } else {
            Toast.makeText(requireContext(), "Notifications enabled by default on this device.",
                Toast.LENGTH_SHORT).show();
            updatePermissionStatus();
        }
    }

    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void updatePermissionStatus() {
        ivSmsStatus.setImageResource(hasSmsPermission()
            ? R.drawable.ic_check_green : R.drawable.ic_close_red);
        ivNotifStatus.setImageResource(hasNotifPermission()
            ? R.drawable.ic_check_green : R.drawable.ic_close_red);
    }
}
