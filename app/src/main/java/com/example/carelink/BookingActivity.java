package com.example.carelink;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private TextInputEditText etPatientName, etPhone, etDate;
    private AutoCompleteTextView actvClinic;
    private TextInputLayout tilClinic;
    private Button btnSearch;
    private Calendar selectedDate;

    private final String[][] clinics = {
            {"City Medical Center", "Jalan Universiti, Petaling Jaya", "3.1209", "101.6538"},
            {"Sunway Specialist Centre", "Jalan Lagoon Selatan, Bandar Sunway", "3.0731", "101.6077"},
            {"KPJ Damansara Hospital", "119 Jalan SS20/10, Damansara Utama", "3.1368", "101.6289"},
            {"Gleneagles KL", "282 Jalan Ampang, Kuala Lumpur", "3.1608", "101.7386"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        etPatientName = findViewById(R.id.etPatientName);
        etPhone = findViewById(R.id.etPhone);
        etDate = findViewById(R.id.etDate);
        actvClinic = findViewById(R.id.actvClinic);
        tilClinic = findViewById(R.id.tilClinic);
        btnSearch = findViewById(R.id.btnSearch);

        selectedDate = Calendar.getInstance();

        String[] clinicNames = new String[clinics.length];
        for (int i = 0; i < clinics.length; i++) {
            clinicNames[i] = clinics[i][0];
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, clinicNames);
        actvClinic.setAdapter(adapter);

        tilClinic.setEndIconOnClickListener(v -> {
            String selected = actvClinic.getText().toString();
            if (!selected.isEmpty()) {
                openGoogleMaps(selected);
            } else {
                Toast.makeText(this, "Select a clinic first", Toast.LENGTH_SHORT).show();
            }
        });

        etDate.setOnClickListener(v -> showDatePicker());

        btnSearch.setOnClickListener(v -> {
            Toast.makeText(this, "Button clicked!", Toast.LENGTH_SHORT).show();
            validateAndProceed();
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    String format = "dd/MM/yyyy";
                    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                    etDate.setText(sdf.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePicker.show();
    }

    private void openGoogleMaps(String clinicName) {
        String address = "", lat = "", lng = "";
        for (String[] c : clinics) {
            if (c[0].equals(clinicName)) {
                address = c[1];
                lat = c[2];
                lng = c[3];
                break;
            }
        }

        Uri gmmIntentUri = Uri.parse("geo:" + lat + "," + lng + "?q=" +
                Uri.encode(clinicName + ", " + address));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Uri webUri = Uri.parse("https://maps.google.com/?q=" +
                    Uri.encode(clinicName + ", " + address));
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    private void validateAndProceed() {
        String name = etPatientName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String clinic = actvClinic.getText().toString();
        String date = etDate.getText().toString();

        if (name.isEmpty()) {
            etPatientName.setError("Enter patient name");
            etPatientName.requestFocus();
            return;
        }
        if (phone.isEmpty() || phone.length() < 10) {
            etPhone.setError("Enter valid phone number");
            etPhone.requestFocus();
            return;
        }
        if (clinic.isEmpty()) {
            actvClinic.setError("Select clinic");
            return;
        }
        if (date.isEmpty()) {
            Toast.makeText(this, "Please select date", Toast.LENGTH_SHORT).show();
            return;
        }

        String address = "";
        double lat = 0.0, lng = 0.0;
        for (String[] c : clinics) {
            if (c[0].equals(clinic)) {
                address = c[1];
                lat = Double.parseDouble(c[2]);
                lng = Double.parseDouble(c[3]);
                break;
            }
        }

        Intent intent = new Intent(BookingActivity.this, AvailableSlotsActivity.class);
        intent.putExtra("PATIENT_NAME", name);
        intent.putExtra("PHONE", phone);
        intent.putExtra("CLINIC_NAME", clinic);
        intent.putExtra("CLINIC_ADDRESS", address);
        intent.putExtra("CLINIC_LAT", lat);
        intent.putExtra("CLINIC_LNG", lng);
        intent.putExtra("DATE", date);

        Toast.makeText(this, "Going to Available Slots...", Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }
}