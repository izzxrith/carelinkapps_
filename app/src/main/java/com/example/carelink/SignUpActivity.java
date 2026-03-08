package com.example.carelink;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SignUpActivity";
    private EditText inputName, inputEmail, inputPassword;
    private CheckBox checkTerms;
    private RadioGroup rgRole;
    private Button btnSignUp;
    private TextView tvLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating your account...");
        progressDialog.setCancelable(false);

        // --- EMULATOR SETUP ---
        try {
            mAuth.useEmulator("10.0.2.2", 9099);
            db.useEmulator("10.0.2.2", 8080);
            FirebaseDatabase.getInstance().useEmulator("10.0.2.2", 9000);
        } catch (Exception e) {
            Log.d(TAG, "Emulator already connected or skipped");
        }

        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        rgRole = findViewById(R.id.rgRole);
        checkTerms = findViewById(R.id.checkTerms);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLogin = findViewById(R.id.tvLogin);

        btnSignUp.setOnClickListener(v -> validateAndRegister());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void validateAndRegister() {
        if (!checkTerms.isChecked()) {
            Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String pass = inputPassword.getText().toString().trim();
        
        String role = "Guardian";
        int selectedId = rgRole.getCheckedRadioButtonId();
        if (selectedId == R.id.rbStudent) {
            role = "Student";
        }

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.contains("@gmail.com")) {
            Toast.makeText(this, "Please use a valid @gmail.com address", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog.show();
        final String finalRole = role;
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserDataToFirestore(user.getUid(), name, email, finalRole);
                        }
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserDataToFirestore(String userId, String name, String email, String role) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", userId);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("role", role);
        userMap.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignUpActivity.this, "Success! Please login.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignUpActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}