package com.example.carelink;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;

import java.util.HashMap;
import java.util.Map;

public class ScanQrFragment extends Fragment {

    private static final String TAG = "ScanQrFragment";
    private DecoratedBarcodeView barcodeView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    public ScanQrFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_qr, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
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
        // Expected format: CARELINK_UID:[student_uid]
        if (qrData.startsWith("CARELINK_UID:")) {
            String studentUid = qrData.replace("CARELINK_UID:", "").trim();
            linkStudentToGuardian(studentUid);
        } else {
            Toast.makeText(getContext(), "Invalid CareLink ID", Toast.LENGTH_SHORT).show();
            resumeScanning();
        }
    }

    private void linkStudentToGuardian(String studentUid) {
        String guardianUid = mAuth.getCurrentUser().getUid();
        
        // Use a global 'links' collection to connect Students and Guardians
        Map<String, Object> linkData = new HashMap<>();
        linkData.put("guardian_uid", guardianUid);
        linkData.put("student_uid", studentUid);
        linkData.put("status", "linked");
        linkData.put("linkedAt", FieldValue.serverTimestamp());

        String linkId = guardianUid + "_" + studentUid;

        db.collection("student_guardian_links").document(linkId)
                .set(linkData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Student linked to your dashboard!", Toast.LENGTH_LONG).show();
                    // Also update the student's record to know who their guardian is
                    db.collection("users").document(studentUid)
                            .update("guardian_id", guardianUid);
                    
                    resumeScanning();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Link Error: " + e.getMessage());
                    Toast.makeText(getContext(), "Linking failed. Try again.", Toast.LENGTH_SHORT).show();
                    resumeScanning();
                });
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
}