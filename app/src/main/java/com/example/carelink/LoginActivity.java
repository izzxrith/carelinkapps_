package com.example.carelink;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView tvForgot, tvSignUp;
    private ImageButton btnGoogle, btnFacebook;
    private ProgressDialog progressDialog;

    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- SK PINJI FIX: FORCE PAIRING SCREEN ON WATCH ---
        if (isWatchDevice()) {
            Log.d(TAG, "Watch detected. Current User: " + mAuth.getCurrentUser());
            // If already logged in, go to dashboard. Otherwise, show pairing QR.
            if (mAuth.getCurrentUser() == null) {
                startActivity(new Intent(this, WatchPairingActivity.class));
                finish();
                return;
            } else {
                startActivity(new Intent(this, WatchDashboardActivity.class));
                finish();
                return;
            }
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Syncing...");
        progressDialog.setCancelable(false);

        try {
            mAuth.useEmulator("10.0.2.2", 9099);
            db.useEmulator("10.0.2.2", 8080);
            FirebaseDatabase.getInstance().useEmulator("10.0.2.2", 9000);
        } catch (Exception e) {
            Log.d(TAG, "Emulator skipped");
        }

        initializeViews();
        setupSocialLogin();
        setupClickListeners();
    }

    private boolean isWatchDevice() {
        Configuration config = getResources().getConfiguration();
        // Check 1: Screen size (watches are tiny)
        boolean isSmallScreen = (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL;
        // Check 2: System feature
        boolean hasWatchFeature = getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        // Check 3: UI Mode
        boolean isWatchUi = (config.uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_WATCH;
        
        return hasWatchFeature || isWatchUi || isSmallScreen;
    }

    private void initializeViews() {
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgot = findViewById(R.id.tvForgot);
        tvSignUp = findViewById(R.id.tvSignUp);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
    }

    private void setupSocialLogin() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String pass = inputPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog.show();
            mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkUserRoleAndRedirect();
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        });

        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            finish();
        });
    }

    private void checkUserRoleAndRedirect() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            progressDialog.dismiss();
            return;
        }

        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(doc -> {
                progressDialog.dismiss();
                if (doc.exists()) {
                    String role = doc.contains("role") ? doc.getString("role") : "Guardian";
                    Intent intent;
                    if ("Student".equalsIgnoreCase(role)) {
                        intent = new Intent(LoginActivity.this, WatchDashboardActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                finish();
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "Google failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        progressDialog.show();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) checkUserRoleAndRedirect();
            else progressDialog.dismiss();
        });
    }
}