package com.example.carelink;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WatchPairingActivity extends AppCompatActivity {

    private ImageView ivPairingQr;
    private TextView tvPairingStatus;
    private FirebaseFirestore db;
    private String pairingId;
    private ListenerRegistration pairingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_pairing);

        db = FirebaseFirestore.getInstance();
        ivPairingQr = findViewById(R.id.ivPairingQr);
        tvPairingStatus = findViewById(R.id.tvPairingStatus);

        generatePairingCode();
    }

    private void generatePairingCode() {
        pairingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrData = "CARELINK_PAIR:" + pairingId;

        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.createBitmap(matrix);
            ivPairingQr.setImageBitmap(bitmap);

            // Create a pending pairing record in Firestore
            Map<String, Object> pairingData = new HashMap<>();
            pairingData.put("status", "pending");
            pairingData.put("createdAt", System.currentTimeMillis());

            db.collection("watch_pairings").document(pairingId)
                    .set(pairingData)
                    .addOnSuccessListener(aVoid -> listenForPairing());

        } catch (Exception e) {
            Log.e("WatchPairing", "QR Error", e);
        }
    }

    private void listenForPairing() {
        pairingListener = db.collection("watch_pairings").document(pairingId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;

                    if (snapshot != null && snapshot.exists()) {
                        String status = snapshot.getString("status");
                        if ("paired".equals(status)) {
                            String userUid = snapshot.getString("userUid");
                            completePairing(userUid);
                        }
                    }
                });
    }

    private void completePairing(String uid) {
        if (pairingListener != null) pairingListener.remove();
        
        Toast.makeText(this, "Pairing Successful!", Toast.LENGTH_SHORT).show();
        
        // In a real app, we'd use a secure token. 
        // For the SK Pinji demo, we'll redirect to the Dashboard.
        Intent intent = new Intent(this, WatchDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pairingListener != null) pairingListener.remove();
    }
}