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

    private void startScanning() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    processScannedCode(result.getText());
                    pauseScanning();
                }
            }
            @Override public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {}
        });
    }

    private void processScannedCode(String qrData) {
        if (qrData.startsWith("CARELINK_UID:")) {
            // Existing Linking Logic
            String studentUid = qrData.replace("CARELINK_UID:", "").trim();
            linkStudentToGuardian(studentUid);
        } 
        else if (qrData.startsWith("CARELINK_PAIR:")) {
            // NEW PAIRING LOGIC: Authorizing a Watch
            String pairingId = qrData.replace("CARELINK_PAIR:", "").trim();
            pairWatchToUser(pairingId);
        }
        else {
            Toast.makeText(getContext(), "Invalid CareLink Code", Toast.LENGTH_SHORT).show();
            resumeScanning();
        }
    }

    private void pairWatchToUser(String pairingId) {
        if (mAuth.getCurrentUser() == null) return;
        String currentUserUid = mAuth.getCurrentUser().getUid();
        
        // This tells the watch: "You are now logged in as me"
        Map<String, Object> pairingUpdate = new HashMap<>();
        pairingUpdate.put("status", "paired");
        pairingUpdate.put("userUid", currentUserUid);
        pairingUpdate.put("pairedAt", FieldValue.serverTimestamp());

        db.collection("watch_pairings").document(pairingId)
                .update(pairingUpdate)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Watch Authorized!", Toast.LENGTH_LONG).show();
                    resumeScanning();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Pairing Failed", Toast.LENGTH_SHORT).show();
                    resumeScanning();
                });
    }

    private void linkStudentToGuardian(String studentUid) {
        if (mAuth.getCurrentUser() == null) return;
        String guardianUid = mAuth.getCurrentUser().getUid();
        Map<String, Object> linkData = new HashMap<>();
        linkData.put("guardian_uid", guardianUid);
        linkData.put("student_uid", studentUid);
        linkData.put("status", "linked");
        linkData.put("linkedAt", FieldValue.serverTimestamp());

        db.collection("student_guardian_links").document(guardianUid + "_" + studentUid)
                .set(linkData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Student Linked!", Toast.LENGTH_LONG).show();
                    db.collection("users").document(studentUid).update("guardian_id", guardianUid);
                    resumeScanning();
                });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
    }

    private void pauseScanning() {
        if (barcodeView != null) barcodeView.pause();
    }

    private void resumeScanning() {
        if (barcodeView != null) barcodeView.resume();
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeScanning();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseScanning();
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