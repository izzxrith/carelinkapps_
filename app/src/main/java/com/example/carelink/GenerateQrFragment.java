package com.example.carelink;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class GenerateQrFragment extends Fragment {

    private ImageView ivQrCode;
    private Button btnShare, btnSave;
    private TextView tvLinkStatus;
    private Bitmap qrBitmap;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private String deviceLinkId;

    public GenerateQrFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generate_qr, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivQrCode = view.findViewById(R.id.ivQrCode);
        btnShare = view.findViewById(R.id.btnShare);
        btnSave = view.findViewById(R.id.btnSave);
        tvLinkStatus = view.findViewById(R.id.tvLinkStatus);

        generateQRCode();

        btnShare.setOnClickListener(v -> shareQrCode());
        btnSave.setOnClickListener(v -> saveQrToGallery());

        listenForWatchConnection();

        return view;
    }

    private void generateQRCode() {
        try {
            // Add null check for current user
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }
            currentUserId = currentUser.getUid();

            deviceLinkId = currentUserId + "_" + System.currentTimeMillis();

            String qrData = "CARELINK://" + deviceLinkId + ":" + currentUserId;

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 400, 400);
            BarcodeEncoder encoder = new BarcodeEncoder();
            qrBitmap = encoder.createBitmap(matrix);

            ivQrCode.setImageBitmap(qrBitmap);

            Map<String, Object> linkData = new HashMap<>();
            linkData.put("userId", currentUserId);
            linkData.put("status", "waiting"); // waiting, linked, expired
            linkData.put("timestamp", System.currentTimeMillis());
            linkData.put("deviceType", "watch");

            db.collection("device_links").document(deviceLinkId)
                    .set(linkData);

            Toast.makeText(getContext(), "Scan this QR with your watch", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error generating QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQrCode() {
        if (qrBitmap == null) {
            Toast.makeText(getContext(), "QR Code not ready yet. Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File cachePath = new File(requireContext().getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }

            File file = new File(cachePath, "carelink_qr.png");
            FileOutputStream stream = new FileOutputStream(file);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();

            if (!file.exists()) {
                Toast.makeText(getContext(), "Failed to create file", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri contentUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Scan this QR code with CareLink Watch App to link your device!");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
            Toast.makeText(getContext(), "Opening share...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Share failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveQrToGallery() {
        if (qrBitmap == null) return;

        String filename = "CareLink_QR_" + System.currentTimeMillis() + ".png";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CareLink");

                Uri uri = requireContext().getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    OutputStream os = requireContext().getContentResolver().openOutputStream(uri);
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    if (os != null) os.close();
                }
            } else {
                File directory = new File(Environment.getExternalStorageDirectory(), "CareLink");
                if (!directory.exists()) directory.mkdirs();

                File file = new File(directory, filename);
                FileOutputStream fos = new FileOutputStream(file);
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(file);
                mediaScanIntent.setData(contentUri);
                requireContext().sendBroadcast(mediaScanIntent);
            }

            Toast.makeText(getContext(), "QR Code saved to gallery!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to save", Toast.LENGTH_SHORT).show();
        }
    }

    private void listenForWatchConnection() {
        if (deviceLinkId == null) return;

        db.collection("device_links").document(deviceLinkId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;

                    String status = snapshot.getString("status");
                    if ("linked".equals(status)) {
                        String deviceName = snapshot.getString("deviceName");
                        tvLinkStatus.setText("✓ Connected to: " + (deviceName != null ? deviceName : "Watch"));
                        tvLinkStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                        startHealthDataSync();
                    }
                });
    }

    private void startHealthDataSync() {
        Toast.makeText(getContext(), "Health data syncing from watch...", Toast.LENGTH_SHORT).show();
    }
}