package com.example.carelink;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carelink.adapters.TopDoctorAdapter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView tvWelcome, tvSubtitle, tvSeeAllLocation, tvSeeAllDoctors;
    private EditText etSearch;
    private LinearLayout btnDoctor, btnMonitor, btnEmotion, btnAmbulance, btnLink, btnOpenMap;
    private CardView cardBanner;
    private RecyclerView rvTopDoctors;
    private MapView ivMapPreview;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TopDoctorAdapter doctorAdapter;
    private List<DoctorItem> doctorList;
    private EmergencyMonitor emergencyMonitor;

    private GoogleMap googleMap;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadUserData();
        setupClickListeners();
        loadTopDoctors();
        startEmergencyMonitoring();
        setupMap(savedInstanceState);
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        etSearch = findViewById(R.id.etSearch);
        btnDoctor = findViewById(R.id.btnDoctor);
        btnMonitor = findViewById(R.id.btnMonitor);
        btnEmotion = findViewById(R.id.btnEmotion);
        btnAmbulance = findViewById(R.id.btnAmbulance);
        btnLink = findViewById(R.id.btnLink);
        btnOpenMap = findViewById(R.id.btnOpenMap);

        cardBanner = findViewById(R.id.cardBanner);
        rvTopDoctors = findViewById(R.id.rvTopDoctors);
        ivMapPreview = findViewById(R.id.ivMapPreview);

        tvSeeAllLocation = findViewById(R.id.tvSeeAllLocation);
        tvSeeAllDoctors = findViewById(R.id.tvSeeAllDoctors);

        setupCustomBottomNavigation();

        rvTopDoctors.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        doctorList = new ArrayList<>();
        doctorAdapter = new TopDoctorAdapter(doctorList, this::onDoctorClick);
        rvTopDoctors.setAdapter(doctorAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        ivMapPreview.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });
    }

    private void setupCustomBottomNavigation() {
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navMessages = findViewById(R.id.navMessages);
        LinearLayout navSchedule = findViewById(R.id.navSchedule);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v ->
                Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show()
        );

        navMessages.setOnClickListener(v ->
                startActivity(new Intent(this, MessageActivity.class))
        );

        navSchedule.setOnClickListener(v ->
                startActivity(new Intent(this, ScheduleActivity.class))
        );

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                tvWelcome.setText("Welcome, " + name);
            } else {
                db.collection("users").document(user.getUid())
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String userName = doc.getString("name");
                                if (userName != null && !userName.isEmpty()) {
                                    tvWelcome.setText("Welcome, " + userName);
                                }
                            }
                        });
            }
        }
    }

    private void setupClickListeners() {
        etSearch.setOnClickListener(v ->
                startActivity(new Intent(this, BookingActivity.class))
        );

        btnDoctor.setOnClickListener(v ->
                startActivity(new Intent(this, BookingActivity.class))
        );

        btnMonitor.setOnClickListener(v ->
                startActivity(new Intent(this, MonitorActivity.class))
        );

        btnEmotion.setOnClickListener(v ->
                startActivity(new Intent(this, EmotionActivity.class))
        );

        btnAmbulance.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(android.net.Uri.parse("tel:999"));
            startActivity(intent);
        });

        cardBanner.setOnClickListener(v ->
                Toast.makeText(this, "Learn more about family health", Toast.LENGTH_SHORT).show()
        );

        ivMapPreview.setOnClickListener(v ->
                startActivity(new Intent(this, LocationMapActivity.class))
        );

        btnOpenMap.setOnClickListener(v ->
                startActivity(new Intent(this, LocationMapActivity.class))
        );

        btnLink.setOnClickListener(v ->
                startActivity(new Intent(this, QRCodeActivity.class))
        );

        tvSeeAllLocation.setOnClickListener(v ->
                startActivity(new Intent(this, LocationMapActivity.class))
        );

        tvSeeAllDoctors.setOnClickListener(v ->
                startActivity(new Intent(this, BookingActivity.class))
        );
    }

    private void loadTopDoctors() {
        doctorList.add(new DoctorItem("Dr. Sarah", "Nephrology (Kidney diseases & Care)", "4.7", "2km away", R.drawable.ic_doctor_male));
        doctorList.add(new DoctorItem("Dr. Rajesh Kumar", "Radiology (X-ray & CT Scan)", "4.9", "1.5km away", R.drawable.ic_doctor_female));
        doctorList.add(new DoctorItem("Dr. Lim Mei Hua", "General Surgery", "4.8", "3km away", R.drawable.ic_doctor_male));
        doctorList.add(new DoctorItem("Dr. Ahmad Abdullah", "Dermatology (Skin & Hair)", "4.6", "2.5km away", R.drawable.ic_doctor_female));

        doctorAdapter.notifyDataSetChanged();
    }

    private void onDoctorClick(DoctorItem doctor) {
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("DOCTOR_NAME", doctor.name);
        intent.putExtra("DOCTOR_SPECIALTY", doctor.specialty);
        startActivity(intent);
    }

    private void startEmergencyMonitoring() {
        Intent fallService = new Intent(this, FallDetectionService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(fallService);
        } else {
            startService(fallService);
        }

        emergencyMonitor = new EmergencyMonitor(this);
        emergencyMonitor.startMonitoring();
    }

    private void setupMap(Bundle savedInstanceState) {
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        ivMapPreview.onCreate(mapViewBundle);
        ivMapPreview.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        LatLng kl = new LatLng(3.1390, 101.6869);

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions().position(kl).title("Top Doctor Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kl, 12f));

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        googleMap.setOnMapClickListener(latLng ->
                startActivity(new Intent(DashboardActivity.this, LocationMapActivity.class))
        );

        googleMap.setOnMarkerClickListener(marker -> {
            startActivity(new Intent(DashboardActivity.this, LocationMapActivity.class));
            return true;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ivMapPreview.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ivMapPreview.onResume();
    }

    @Override
    protected void onPause() {
        ivMapPreview.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        ivMapPreview.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        ivMapPreview.onDestroy();
        super.onDestroy();

        if (emergencyMonitor != null) {
            emergencyMonitor.stopMonitoring();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ivMapPreview.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        ivMapPreview.onSaveInstanceState(mapViewBundle);
    }

    public static class DoctorItem {
        public String name, specialty, rating, distance;
        public int imageRes;

        public DoctorItem(String name, String specialty, String rating, String distance, int imageRes) {
            this.name = name;
            this.specialty = specialty;
            this.rating = rating;
            this.distance = distance;
            this.imageRes = imageRes;
        }
    }
}