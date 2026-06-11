package com.example.expensetracker.ui.profile;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName;
    private TextView tvProfileInitial;
    private ImageView ivProfilePhoto;

    private User currentUser;

    // URI for the photo taken with the camera (needed before it's saved)
    private Uri cameraTempUri;

    // ── Activity result launchers ─────────────────────────────────────────────

    /** Pick image from gallery */
    private final ActivityResultLauncher<String> pickImageLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) saveProfilePhoto(uri);
        });

    /** Capture photo with camera */
    private final ActivityResultLauncher<Uri> takePhotoLauncher =
        registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraTempUri != null) saveProfilePhoto(cameraTempUri);
        });

    // ─────────────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvProfileName    = view.findViewById(R.id.tvProfileName);
        tvProfileInitial = view.findViewById(R.id.tvProfileInitial);
        ivProfilePhoto   = view.findViewById(R.id.ivProfilePhoto);

        // Tap avatar or camera badge → show photo-source chooser
        view.findViewById(R.id.flAvatarContainer).setOnClickListener(v -> showPhotoSourceDialog());

        // Tap pencil icon → show edit-name dialog
        view.findViewById(R.id.ivEditName).setOnClickListener(v -> showEditNameDialog());

        loadUserData();

        return view;
    }

    // ── Load user from DB ─────────────────────────────────────────────────────

    private void loadUserData() {
        AppDatabase.dbExecutor.execute(() -> {
            User user = AppDatabase.getInstance(requireContext()).userDao().getUser();
            if (user == null) return;
            currentUser = user;
            requireActivity().runOnUiThread(() -> applyUserToUi(user));
        });
    }

    private void applyUserToUi(User user) {
        tvProfileName.setText(user.name);

        if (user.profilePicturePath != null) {
            File imgFile = new File(user.profilePicturePath);
            if (imgFile.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                ivProfilePhoto.setImageBitmap(toCircleBitmap(bmp));
                tvProfileInitial.setVisibility(View.GONE);
                return;
            }
        }
        // Fallback: show initials
        tvProfileInitial.setVisibility(View.VISIBLE);
        tvProfileInitial.setText(getInitial(user.name));
        ivProfilePhoto.setImageDrawable(null);
    }

    // ── Photo source dialog ───────────────────────────────────────────────────

    private void showPhotoSourceDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Profile Picture")
            .setItems(new String[]{"Choose from Gallery", "Take Photo", "Remove Photo"},
                (dialog, which) -> {
                    switch (which) {
                        case 0: pickImageLauncher.launch("image/*"); break;
                        case 1: launchCamera(); break;
                        case 2: removePhoto(); break;
                    }
                })
            .show();
    }

    private void launchCamera() {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.DISPLAY_NAME, "profile_temp_" + System.currentTimeMillis() + ".jpg");
        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        cameraTempUri = requireContext().getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
        if (cameraTempUri != null) takePhotoLauncher.launch(cameraTempUri);
    }

    // ── Save / remove photo ───────────────────────────────────────────────────

    private void saveProfilePhoto(Uri sourceUri) {
        AppDatabase.dbExecutor.execute(() -> {
            try {
                // Copy to app-private storage so it survives gallery deletions
                File dir = new File(requireContext().getFilesDir(), "profile");
                if (!dir.exists()) dir.mkdirs();
                File dest = new File(dir, "avatar_" + System.currentTimeMillis() + ".jpg");

                try (InputStream in = requireContext().getContentResolver().openInputStream(sourceUri);
                     OutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                }

                // Delete old photo file if it exists
                if (currentUser != null && currentUser.profilePicturePath != null) {
                    new File(currentUser.profilePicturePath).delete();
                }

                // Update DB
                if (currentUser != null) {
                    AppDatabase.getInstance(requireContext())
                        .userDao().updateProfilePicture(currentUser.id, dest.getAbsolutePath());
                    currentUser.profilePicturePath = dest.getAbsolutePath();
                }

                requireActivity().runOnUiThread(() -> {
                    Bitmap bmp = BitmapFactory.decodeFile(dest.getAbsolutePath());
                    ivProfilePhoto.setImageBitmap(toCircleBitmap(bmp));
                    tvProfileInitial.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Failed to save photo", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void removePhoto() {
        if (currentUser == null) return;
        AppDatabase.dbExecutor.execute(() -> {
            if (currentUser.profilePicturePath != null) {
                new File(currentUser.profilePicturePath).delete();
            }
            AppDatabase.getInstance(requireContext())
                .userDao().updateProfilePicture(currentUser.id, null);
            currentUser.profilePicturePath = null;

            requireActivity().runOnUiThread(() -> {
                ivProfilePhoto.setImageDrawable(null);
                tvProfileInitial.setVisibility(View.VISIBLE);
                tvProfileInitial.setText(getInitial(currentUser.name));
            });
        });
    }

    // ── Edit name dialog ──────────────────────────────────────────────────────

    private void showEditNameDialog() {
        if (currentUser == null) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_name, null);

        TextInputEditText etName  = dialogView.findViewById(R.id.etName);
        TextInputLayout   tilName = dialogView.findViewById(R.id.tilName);
        etName.setText(currentUser.name);
        etName.setSelection(etName.getText() != null ? etName.getText().length() : 0);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String newName = etName.getText() != null
                    ? etName.getText().toString().trim() : "";
            if (TextUtils.isEmpty(newName)) {
                tilName.setError("Name cannot be empty");
                return;
            }
            tilName.setError(null);
            saveName(newName);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveName(String newName) {
        AppDatabase.dbExecutor.execute(() -> {
            AppDatabase.getInstance(requireContext())
                .userDao().updateName(currentUser.id, newName);
            currentUser.name = newName;
            requireActivity().runOnUiThread(() -> {
                tvProfileName.setText(newName);
                if (tvProfileInitial.getVisibility() == View.VISIBLE) {
                    tvProfileInitial.setText(getInitial(newName));
                }
            });
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Crop a rectangular bitmap into a circle. */
    private Bitmap toCircleBitmap(Bitmap src) {
        int size = Math.min(src.getWidth(), src.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader shader = new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        return output;
    }

    private String getInitial(String name) {
        if (name == null || name.isEmpty()) return "?";
        return String.valueOf(name.charAt(0)).toUpperCase();
    }
}
