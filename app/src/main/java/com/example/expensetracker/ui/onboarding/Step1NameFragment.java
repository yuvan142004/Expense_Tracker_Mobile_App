package com.example.expensetracker.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.R;

public class Step1NameFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboard_step1, container, false);

        EditText etName = view.findViewById(R.id.etName);
        Button   btnNext = view.findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }
            OnboardingActivity activity = (OnboardingActivity) requireActivity();
            activity.userName = name;
            activity.showStep(new Step2BankFragment());
        });

        return view;
    }
}
