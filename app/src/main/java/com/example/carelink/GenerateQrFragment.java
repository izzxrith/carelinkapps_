package com.example.carelink;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class GenerateQrFragment extends Fragment {

    private ImageView ivQrCode;
    private TextView tvInstruction;
    private FirebaseAuth mAuth;

    public GenerateQrFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generate_qr, container, false);

        mAuth = FirebaseAuth.getInstance();
        ivQrCode = view.findViewById(R.id.ivQrCode);
        tvInstruction = view.findViewById(R.id.tvInstruction);

        generateStudentQRCode();

        return view;
    }

    private void generateStudentQRCode() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Simplified QR format for SK Pinji student linking
            String qrData = "CARELINK_UID:" + user.getUid();

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap qrBitmap = encoder.createBitmap(matrix);

            ivQrCode.setImageBitmap(qrBitmap);
            
            if (tvInstruction != null) {
                tvInstruction.setText("Ask your Teacher/Parent to scan this code to link your account.");
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error generating QR", Toast.LENGTH_SHORT).show();
        }
    }
}