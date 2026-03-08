package com.example.carelink;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;

public class ScanQrFragment extends Fragment {

    private DecoratedBarcodeView barcodeView;
    private FirebaseFirestore db;
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    public ScanQrFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_qr, container, false);

        db = FirebaseFirestore.getInstance();
        barcodeView = view.findViewById(R.id.barcodeScanner);

        if (checkCameraPermission()) {
            startScanning();
        } else {
            requestCameraPermission();
        }

        return view;
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
    }

    private void startScanning() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    processScannedCode(result.getText());
                    pauseScanning();
                }
            }

            @Override
            public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {}
        });
    }

    private void processScannedCode(String qrData) {
        try {
            if (qrData.startsWith("CARELINK://")) {
                String[] parts = qrData.replace("CARELINK://", "").split(":");
                if (parts.length == 2) {
                    String linkId = parts[0];
                    String userId = parts[1];

                    linkDevice(linkId, userId);
                }
            } else {
                Toast.makeText(getContext(), "Invalid CareLink QR Code", Toast.LENGTH_SHORT).show();
                resumeScanning();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error processing QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void linkDevice(String linkId, String targetUserId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("device_links").document(linkId)
                .update(
                        "status", "linked",
                        "linkedUserId", currentUserId,
                        "linkedAt", System.currentTimeMillis(),
                        "deviceName", "CareLink Watch"
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Watch linked successfully!", Toast.LENGTH_LONG).show();
                    addDeviceToUser(linkId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Link failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resumeScanning();
                });
    }

    private void addDeviceToUser(String linkId) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        java.util.Map<String, Object> device = new java.util.HashMap<>();
        device.put("linkId", linkId);
        device.put("type", "watch");
        device.put("linkedAt", System.currentTimeMillis());
        device.put("status", "active");

        db.collection("users").document(userId)
                .collection("devices")
                .add(device);
    }

    private void pauseScanning() {
        barcodeView.pause();
    }

    private void resumeScanning() {
        barcodeView.resume();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (barcodeView != null) barcodeView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (barcodeView != null) barcodeView.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(getContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}