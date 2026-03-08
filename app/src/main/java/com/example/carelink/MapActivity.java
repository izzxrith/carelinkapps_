package com.example.carelink;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (checkLocationPermission()) {
            enableMyLocation();
        } else {
            requestLocationPermission();
        }

        loadClinicsFromFirebase();

        mMap.setOnMarkerClickListener(marker -> {
            showClinicDetails(marker);
            return true;
        });
    }

    private void loadClinicsFromFirebase() {
        FirebaseFirestore.getInstance()
                .collection("clinics")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        String address = doc.getString("address");
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        String specialty = doc.getString("specialty");

                        if (lat != null && lng != null) {
                            LatLng position = new LatLng(lat, lng);
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(position)
                                    .title(name != null ? name : "Clinic")
                                    .snippet(specialty != null ? specialty : "General")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                            Marker marker = mMap.addMarker(markerOptions);
                            if (marker != null) {
                                marker.setTag(new Clinic(name, address, lat, lng, specialty));
                            }
                        }
                    }
                    if (querySnapshot.isEmpty()) {
                        addSampleClinics();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load clinics", Toast.LENGTH_SHORT).show();
                    addSampleClinics();
                });
    }

    private void addSampleClinics() {
        Clinic[] clinics = {
                new Clinic("City Medical Center", "Jalan Universiti", 3.1209, 101.6538, "Cardiology"),
                new Clinic("Sunway Specialist", "Bandar Sunway", 3.0731, 101.6077, "Pediatrics"),
                new Clinic("KPJ Damansara", "Damansara Utama", 3.1368, 101.6289, "Orthopedics"),
                new Clinic("Gleneagles KL", "Jalan Ampang", 3.1608, 101.7386, "General")
        };

        for (Clinic clinic : clinics) {
            LatLng position = new LatLng(clinic.lat, clinic.lng);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(clinic.name)
                    .snippet(clinic.specialty)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            if (marker != null) marker.setTag(clinic);
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12));
                    }
                });
    }

    private void showClinicDetails(Marker marker) {
        Clinic clinic = (Clinic) marker.getTag();
        if (clinic == null) return;

        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_clinic, null);

        TextView tvName = view.findViewById(R.id.tvClinicName);
        TextView tvAddress = view.findViewById(R.id.tvClinicAddress);
        TextView tvSpecialty = view.findViewById(R.id.tvSpecialty);
        Button btnDirections = view.findViewById(R.id.btnDirections);
        Button btnBook = view.findViewById(R.id.btnBookAppointment);

        tvName.setText(clinic.name);
        tvAddress.setText(clinic.address);
        tvSpecialty.setText(clinic.specialty);

        btnDirections.setOnClickListener(v -> {
            String uri = String.format("google.navigation:q=%f,%f", clinic.lat, clinic.lng);
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
            bottomSheet.dismiss();
        });

        btnBook.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookingActivity.class);
            intent.putExtra("CLINIC_NAME", clinic.name);
            intent.putExtra("CLINIC_LAT", clinic.lat);
            intent.putExtra("CLINIC_LNG", clinic.lng);
            intent.putExtra("CLINIC_ADDRESS", clinic.address);
            startActivity(intent);
            bottomSheet.dismiss();
        });

        bottomSheet.setContentView(view);
        bottomSheet.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    static class Clinic {
        String name, address, specialty;
        double lat, lng;

        Clinic(String name, String address, double lat, double lng, String specialty) {
            this.name = name;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
            this.specialty = specialty;
        }
    }
}