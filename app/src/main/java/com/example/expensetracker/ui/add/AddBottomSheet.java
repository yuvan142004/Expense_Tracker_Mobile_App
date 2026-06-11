package com.example.expensetracker.ui.add;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.expensetracker.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AddBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_add, container, false);

        // Manual Entry
        view.findViewById(R.id.btnManualEntry).setOnClickListener(v -> {
            dismiss();
            new ManualEntryDialog().show(
                requireActivity().getSupportFragmentManager(), "ManualEntry");
        });

        return view;
    }
}
