package com.example.carelink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class OnboardingActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button btnStart = findViewById(R.id.btnNext);
        btnStart.setOnClickListener(v -> {
            startActivity(new Intent(OnboardingActivity2.this, OnboardingActivity3.class));
            finish();
        });
        Button btnSkip = findViewById(R.id.btnSkip);
        btnSkip.setOnClickListener(v -> {
            startActivity(new Intent(OnboardingActivity2.this, LoginGatewayActivity.class));
            finish();
        });
    }
}
