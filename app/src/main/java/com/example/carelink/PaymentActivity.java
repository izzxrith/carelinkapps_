package com.example.carelink;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.carelink.models.Appointment;
import com.example.carelink.utils.DataManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {

    private TextView tvSummary, tvTotal;
    private RadioGroup rgPayment;
    private RadioButton rbTnG, rbBanking;
    private Button btnPay;
    private ProgressBar progressBar;
    private LinearLayout paymentForm;

    private String patientName, phone, clinicName, clinicAddress, date, time, doctorName, specialty;
    private double clinicLat, clinicLng, price;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        patientName = getIntent().getStringExtra("PATIENT_NAME");
        phone = getIntent().getStringExtra("PHONE");
        clinicName = getIntent().getStringExtra("CLINIC_NAME");
        clinicAddress = getIntent().getStringExtra("CLINIC_ADDRESS");
        clinicLat = getIntent().getDoubleExtra("CLINIC_LAT", 0);
        clinicLng = getIntent().getDoubleExtra("CLINIC_LNG", 0);
        date = getIntent().getStringExtra("DATE");
        time = getIntent().getStringExtra("TIME");
        doctorName = getIntent().getStringExtra("DOCTOR_NAME");
        specialty = getIntent().getStringExtra("SPECIALTY");
        price = getIntent().getDoubleExtra("PRICE", 0);

        initViews();
        displaySummary();

        btnPay.setOnClickListener(v -> processPayment());
    }

    private void initViews() {
        tvSummary = findViewById(R.id.tvSummary);
        tvTotal = findViewById(R.id.tvTotal);
        rgPayment = findViewById(R.id.rgPayment);
        rbTnG = findViewById(R.id.rbTnG);
        rbBanking = findViewById(R.id.rbBanking);
        btnPay = findViewById(R.id.btnPay);
        progressBar = findViewById(R.id.progressBar);
        paymentForm = findViewById(R.id.paymentForm);
    }

    private void displaySummary() {
        String summary = "Consultation with " + doctorName + "\n" +
                specialty + "\n" +
                clinicName + "\n" +
                date + " at " + time;
        tvSummary.setText(summary);
        tvTotal.setText("Total: RM " + String.format("%.2f", price));
    }

    private void processPayment() {
        int selectedId = rgPayment.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select payment method", Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentMethod = (selectedId == R.id.rbTnG) ? "Touch 'n Go" : "Online Banking";

        paymentForm.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        // Simulate payment processing (API call)
        new Handler().postDelayed(() -> {
            String aptId = "APT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            String currentTime = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date());

            Appointment appointment = new Appointment(
                    aptId, patientName, phone, clinicName, clinicAddress,
                    clinicLat, clinicLng, date, time, doctorName, specialty,
                    price, "upcoming", paymentMethod, currentTime
            );

            DataManager.getInstance().addAppointment(appointment);

            Intent intent = new Intent(PaymentActivity.this, ReceiptActivity.class);
            intent.putExtra("APPOINTMENT", appointment);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();

        }, 2000); // 2 second simulation
    }
}