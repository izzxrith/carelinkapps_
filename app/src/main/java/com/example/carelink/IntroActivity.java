package com.example.carelink;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class IntroActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- SK PINJI: SMART REDIRECT ---
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        if (isWatchDevice()) {
            if (currentUser == null) {
                startActivity(new Intent(this, WatchPairingActivity.class));
            } else {
                startActivity(new Intent(this, WatchDashboardActivity.class));
            }
            finish();
            return;
        }

        // If logged in on a phone, check role and jump straight to the correct dashboard
        if (currentUser != null) {
            checkRoleAndRedirect(currentUser.getUid());
            // Don't finish yet, let the async check handle it or show intro if it fails
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_intro);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> {
            startActivity(new Intent(IntroActivity.this, OnboardingActivity1.class));
            finish();
        });
    }

    private void checkRoleAndRedirect(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String role = doc.getString("role");
                if ("Student".equalsIgnoreCase(role)) {
                    startActivity(new Intent(this, WatchDashboardActivity.class));
                } else {
                    startActivity(new Intent(this, DashboardActivity.class));
                }
                finish();
            }
        });
    }

    private boolean isWatchDevice() {
        Configuration config = getResources().getConfiguration();
        boolean hasWatchFeature = getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_WATCH);
        boolean isWatchUi = (config.uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_WATCH;
        boolean isSmallScreen = (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL;
        return hasWatchFeature || isWatchUi || isSmallScreen;
    }
}
