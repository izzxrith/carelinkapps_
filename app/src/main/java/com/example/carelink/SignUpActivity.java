package com.example.carelink;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {
    private EditText inputName, inputEmail, inputPassword;
    private CheckBox checkTerms;
    private Button btnSignUp;
    private TextView tvLogin;
    private FirebaseAuth mAuth;

    private static final String PREFS_NAME = "CareLinkUserPrefs";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PASSWORD = "user_password";
    private static final String KEY_REGISTERED = "is_registered";

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

        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        checkTerms = findViewById(R.id.checkTerms);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLogin = findViewById(R.id.tvLogin);

        btnSignUp.setOnClickListener(v -> {
            validateAndRegister();
        });

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

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Invalid email address! Please use @gmail.com", Toast.LENGTH_LONG).show();
            inputEmail.setError("Email must be a valid @gmail.com address");
            inputEmail.requestFocus();
            return;
        }

        String passwordError = isValidPassword(pass);
        if (passwordError != null) {
            Toast.makeText(this, passwordError, Toast.LENGTH_LONG).show();
            inputPassword.setError(passwordError);
            inputPassword.requestFocus();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserData(name, email, pass);

                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Account created: " + user.getEmail(), Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$");
    }

    private String isValidPassword(String password) {
        if (password.length() < 8) {
            return "Password must be at least 8 characters";
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasNumber = false;
        boolean hasSymbol = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasNumber = true;
            else if ("!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(c) != -1) hasSymbol = true;
        }

        StringBuilder error = new StringBuilder("Password must contain:");
        boolean hasError = false;

        if (!hasUpper) {
            error.append("\n• At least 1 uppercase letter");
            hasError = true;
        }
        if (!hasLower) {
            error.append("\n• At least 1 lowercase letter");
            hasError = true;
        }
        if (!hasNumber) {
            error.append("\n• At least 1 number");
            hasError = true;
        }
        if (!hasSymbol) {
            error.append("\n• At least 1 special symbol (!@#$%^&*)");
            hasError = true;
        }

        return hasError ? error.toString() : null;
    }

    private void saveUserData(String name, String email, String password) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_REGISTERED, true);

        editor.apply();
    }
}