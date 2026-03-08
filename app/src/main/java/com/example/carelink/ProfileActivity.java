package com.example.carelink;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private TextView tvUsername, tvBirthday, tvPhone, tvAddress, tvRoleInfo;
    private LinearLayout rowInformation, rowFaq, rowLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupBottomNavigation();
        setupClickListeners();
        loadRealUserData();
    }

    private void initViews() {
        tvUsername = findViewById(R.id.tvUsername);
        tvBirthday = findViewById(R.id.tvBirthday);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        tvRoleInfo = findViewById(R.id.tvUserRole); 

        rowInformation = findViewById(R.id.rowInformation);
        rowLogout = findViewById(R.id.rowLogout);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading profile...");
    }

    private void loadRealUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("name");
                    String role = doc.getString("role");
                    String phone = doc.getString("phone");
                    String address = doc.getString("address");
                    String birthday = doc.getString("birthday");

                    tvUsername.setText(name != null ? name : "User");
                    if (tvRoleInfo != null) tvRoleInfo.setText((role != null ? role : "Guardian") + " Profile");
                    
                    tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "Not set");
                    tvAddress.setText(address != null && !address.isEmpty() ? address : "Not set");
                    tvBirthday.setText(birthday != null && !birthday.isEmpty() ? birthday : "Not set");
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load cloud profile", Toast.LENGTH_SHORT).show();
            });
    }

    private void setupClickListeners() {
        if (rowInformation != null) rowInformation.setOnClickListener(v -> showEditDialog());
        if (rowLogout != null) rowLogout.setOnClickListener(v -> logout());
    }

    private void showEditDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_profile);

        EditText etName = dialog.findViewById(R.id.etName);
        EditText etBirthday = dialog.findViewById(R.id.etBirthday);
        EditText etPhone = dialog.findViewById(R.id.etPhone);
        EditText etAddress = dialog.findViewById(R.id.etAddress);
        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        // Pre-fill
        etName.setText(tvUsername.getText().toString());
        etBirthday.setText(tvBirthday.getText().toString().equals("Not set") ? "" : tvBirthday.getText().toString());
        etPhone.setText(tvPhone.getText().toString().equals("Not set") ? "" : tvPhone.getText().toString());
        etAddress.setText(tvAddress.getText().toString().equals("Not set") ? "" : tvAddress.getText().toString());

        etBirthday.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                etBirthday.setText(day + "/" + (month + 1) + "/" + year);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newBirthday = etBirthday.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();
            String newAddress = etAddress.getText().toString().trim();

            if (newName.isEmpty()) {
                etName.setError("Name required");
                return;
            }

            saveProfileToCloud(newName, newBirthday, newPhone, newAddress, dialog);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void saveProfileToCloud(String name, String birthday, String phone, String address, Dialog dialog) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put("name", name);
        update.put("birthday", birthday);
        update.put("phone", phone);
        update.put("address", address);

        progressDialog.show();

        db.collection("users").document(uid).update(update)
            .addOnSuccessListener(aVoid -> {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                dialog.dismiss();
                loadRealUserData();
                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show();
            });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, IntroActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> finish());
        findViewById(R.id.navSchedule).setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        findViewById(R.id.navMessages).setOnClickListener(v -> startActivity(new Intent(this, MessageActivity.class)));
    }
}