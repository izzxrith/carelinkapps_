package com.example.carelink;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.example.carelink.models.Appointment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ReceiptActivity extends AppCompatActivity {

    private TextView tvReceiptNo, tvDate, tvPatient, tvDoctor, tvClinic,
            tvTime, tvPrice, tvPayment, tvStatus;
    private LinearLayout receiptContainer;
    private Button btnDownload, btnViewSchedule;
    private Appointment appointment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        initViews();

        if (savedInstanceState != null) {
            appointment = (Appointment) savedInstanceState.getSerializable("SAVED_APPOINTMENT");
        }

        if (appointment == null) {
            appointment = (Appointment) getIntent().getSerializableExtra("APPOINTMENT");
        }

        if (appointment == null) {
            Toast.makeText(this, "Error: Receipt data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        displayReceipt(appointment);

        btnDownload.setOnClickListener(v -> downloadReceipt());
        btnViewSchedule.setOnClickListener(v -> {
            startActivity(new Intent(this, ScheduleActivity.class));
            finish();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (appointment != null) {
            outState.putSerializable("SAVED_APPOINTMENT", appointment);
        }
    }

    private void initViews() {
        receiptContainer = findViewById(R.id.receiptContainer);
        tvReceiptNo = findViewById(R.id.tvReceiptNo);
        tvDate = findViewById(R.id.tvDate);
        tvPatient = findViewById(R.id.tvPatient);
        tvDoctor = findViewById(R.id.tvDoctor);
        tvClinic = findViewById(R.id.tvClinic);
        tvTime = findViewById(R.id.tvTime);
        tvPrice = findViewById(R.id.tvPrice);
        tvPayment = findViewById(R.id.tvPayment);
        tvStatus = findViewById(R.id.tvStatus);
        btnDownload = findViewById(R.id.btnDownload);
        btnViewSchedule = findViewById(R.id.btnViewSchedule);
    }

    private void displayReceipt(Appointment apt) {
        if (apt == null) return;

        tvReceiptNo.setText("Receipt #: " + apt.getId());
        tvDate.setText("Booking Date: " + apt.getBookingTime());
        tvPatient.setText("Patient: " + apt.getPatientName() + "\nPhone: " + apt.getPhoneNumber());
        tvDoctor.setText("Doctor: " + apt.getDoctorName() + "\nSpecialty: " + apt.getSpecialty());
        tvClinic.setText("Clinic: " + apt.getClinicName() + "\nAddress: " + apt.getClinicAddress());
        tvTime.setText("Appointment: " + apt.getDate() + " at " + apt.getTime());
        tvPrice.setText("Amount Paid: RM " + String.format("%.2f", apt.getPrice()));
        tvPayment.setText("Payment Method: " + apt.getPaymentMethod());
        tvStatus.setText("Status: " + apt.getStatus().toUpperCase());
    }

    private void downloadReceipt() {
        if (receiptContainer.getWidth() == 0 || receiptContainer.getHeight() == 0) {
            Toast.makeText(this, "Cannot save receipt yet", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(
                receiptContainer.getWidth(),
                receiptContainer.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        receiptContainer.draw(canvas);

        String filename = "CareLink_Receipt_" + System.currentTimeMillis() + ".png";
        File filepath = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename);

        try {
            FileOutputStream outputStream = new FileOutputStream(filepath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            Uri uri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider", filepath);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "My CareLink Appointment Receipt");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Receipt"));

        } catch (IOException e) {
            Toast.makeText(this, "Error saving receipt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}