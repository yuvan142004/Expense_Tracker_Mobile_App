package com.example.expensetracker.ui.add;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.expensetracker.R;
import com.example.expensetracker.utils.UpiAppDetector;

import java.util.List;

/**
 * Reusable dialog for selecting a UPI app from installed apps
 */
public class UpiAppSelectorDialog extends DialogFragment {

    public interface OnAppSelectedListener {
        void onAppSelected(String appPackage, boolean rememberChoice);
    }

    private OnAppSelectedListener listener;
    private String selectedAppPackage = null;

    public void setOnAppSelectedListener(OnAppSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_upi_app_selector, null);

        ListView listView = view.findViewById(R.id.listViewUpiApps);
        CheckBox cbRemember = view.findViewById(R.id.cbRememberChoice);

        // Get list of installed UPI apps
        List<UpiAppDetector.UpiApp> installedApps = UpiAppDetector.getInstalledUpiApps(requireContext());

        if (installedApps.isEmpty()) {
            // No UPI apps installed
            return new AlertDialog.Builder(requireContext())
                    .setTitle("No UPI Apps Found")
                    .setMessage("Please install a UPI app like Google Pay, PhonePe, or Paytm to make payments.")
                    .setPositiveButton("OK", null)
                    .create();
        }

        // Pre-select first app by default
        if (!installedApps.isEmpty()) {
            selectedAppPackage = installedApps.get(0).packageName;
        }

        // Create custom adapter for UPI apps
        ArrayAdapter<UpiAppDetector.UpiApp> adapter = new ArrayAdapter<UpiAppDetector.UpiApp>(
                requireContext(),
                R.layout.item_upi_app,
                installedApps
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull android.view.ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext())
                            .inflate(R.layout.item_upi_app, parent, false);
                }

                UpiAppDetector.UpiApp app = getItem(position);
                if (app != null) {
                    ImageView ivIcon = convertView.findViewById(R.id.ivAppIcon);
                    TextView tvName = convertView.findViewById(R.id.tvAppName);
                    TextView tvStatus = convertView.findViewById(R.id.tvInstallStatus);
                    RadioButton rbSelect = convertView.findViewById(R.id.rbSelect);

                    if (app.icon != null) {
                        ivIcon.setImageDrawable(app.icon);
                    }
                    tvName.setText(app.displayName != null ? app.displayName : app.name);
                    tvStatus.setVisibility(View.GONE); // All apps in list are installed

                    // Set radio button state
                    rbSelect.setChecked(app.packageName.equals(selectedAppPackage));
                }

                return convertView;
            }
        };

        listView.setAdapter(adapter);

        // Handle item selection
        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            selectedAppPackage = installedApps.get(position).packageName;
            adapter.notifyDataSetChanged(); // Refresh to show selection
        });

        return new AlertDialog.Builder(requireContext())
                .setTitle("Select UPI App")
                .setView(view)
                .setPositiveButton("Continue", (dialog, which) -> {
                    if (selectedAppPackage != null && listener != null) {
                        boolean rememberChoice = cbRemember.isChecked();
                        listener.onAppSelected(selectedAppPackage, rememberChoice);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
    }
}
