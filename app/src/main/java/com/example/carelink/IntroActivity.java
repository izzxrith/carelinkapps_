package com.example.carelink;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

public class IntroActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // EMULATOR CALL REMOVED - NOW HANDLED BY CareLinkApp.java

        // --- SK PINJI: FORCED LOGIN FLOW ---
        if (isWatchDevice() && mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, WatchPairingActivity.class));
            finish();
            return;
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

    private boolean isWatchDevice() {
        Configuration config = getResources().getConfiguration();
        boolean hasWatchFeature = getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        boolean isWatchUi = (config.uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_WATCH;
        boolean isSmallScreen = (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL;
        return hasWatchFeature || isWatchUi || isSmallScreen;
    }
}