package com.example.carelink;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;

import java.util.Calendar;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUsername, tvHeartRate, tvCalories, tvWeight, tvBirthday, tvPhone, tvAddress;
    private LinearLayout rowInformation, rowFaq, rowLogout;
    private LinearLayout navHome, navMessages, navSchedule, navProfile;
    private ImageView profileImage;
    private NestedScrollView scrollProfile;
    private CardView cardProfileInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        setupBottomNavigation();
        setupClickListeners();
        loadUserData();
        loadHealthStats();
    }

    private void initViews() {
        profileImage = findViewById(R.id.profileImage);

        tvUsername = findViewById(R.id.tvUsername);
        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvCalories = findViewById(R.id.tvCalories);
        tvWeight = findViewById(R.id.tvWeight);

        tvBirthday = findViewById(R.id.tvBirthday);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);

        rowInformation = findViewById(R.id.rowInformation);
        rowFaq = findViewById(R.id.rowFaq);
        rowLogout = findViewById(R.id.rowLogout);

        navHome = findViewById(R.id.navHome);
        navMessages = findViewById(R.id.navMessages);
        navSchedule = findViewById(R.id.navSchedule);
        navProfile = findViewById(R.id.navProfile);

        scrollProfile = findViewById(R.id.scrollProfile);
        cardProfileInfo = findViewById(R.id.cardProfileInfo);
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));

        navMessages.setOnClickListener(v ->
                startActivity(new Intent(this, MessageActivity.class)));

        navSchedule.setOnClickListener(v ->
                startActivity(new Intent(this, ScheduleActivity.class)));

        navProfile.setOnClickListener(v ->
                Toast.makeText(this, "Already on Profile", Toast.LENGTH_SHORT).show());
    }

    private void setupClickListeners() {
        rowInformation.setOnClickListener(v -> showEditDialog());
        rowFaq.setOnClickListener(v -> showFaqDialog());
        rowLogout.setOnClickListener(v -> logout());
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("CareLinkProfile", MODE_PRIVATE);

        String name = prefs.getString("name", "User Name");
        String birthday = prefs.getString("birthday", "Not set");
        String phone = prefs.getString("phone", "Not set");
        String address = prefs.getString("address", "Not set");

        tvUsername.setText(name);
        tvBirthday.setText(birthday);
        tvPhone.setText(phone);
        tvAddress.setText(address);
    }

    private void loadHealthStats() {
        tvHeartRate.setText("72 bpm");
        tvCalories.setText("2000 cal");
        tvWeight.setText("150 lbs");
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

        etName.setText(tvUsername.getText().toString());
        etBirthday.setText(tvBirthday.getText().toString());
        etPhone.setText(tvPhone.getText().toString());
        etAddress.setText(tvAddress.getText().toString());

        etBirthday.setOnClickListener(v -> openDatePicker(etBirthday));

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String birthday = etBirthday.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Required");
                return;
            }

            if (!phone.isEmpty() && !phone.matches("^01[0-9]{8,9}$")) {
                etPhone.setError("Invalid phone");
                return;
            }

            saveProfile(name, birthday, phone, address);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openDatePicker(EditText etBirthday) {
        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this,
                (view, year, month, day) ->
                        etBirthday.setText(day + "/" + (month + 1) + "/" + year),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void saveProfile(String name, String birthday, String phone, String address) {
        SharedPreferences prefs = getSharedPreferences("CareLinkProfile", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("name", name);
        editor.putString("birthday", birthday);
        editor.putString("phone", phone);
        editor.putString("address", address);
        editor.apply();

        tvUsername.setText(name);
        tvBirthday.setText(birthday.isEmpty() ? "Not set" : birthday);
        tvPhone.setText(phone.isEmpty() ? "Not set" : phone);
        tvAddress.setText(address.isEmpty() ? "Not set" : address);

        scrollProfile.post(() -> scrollProfile.smoothScrollTo(0, 0));

        Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
    }

    private void showFaqDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("FAQ");
        builder.setMessage("Contact support");

        builder.setPositiveButton("Call", (d, w) -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+60165570027"));
            startActivity(intent);
        });

        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("CareLinkProfile", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, IntroActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_edit) {
            showEditDialog();
            return true;
        }

        if (id == R.id.action_switch_account) {
            startActivity(new Intent(this, LoginActivity.class));
            return true;
        }

        if (id == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}