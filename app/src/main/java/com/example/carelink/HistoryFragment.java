package com.example.carelink;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carelink.adapters.DeviceHistoryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private DeviceHistoryAdapter adapter;
    private List<DeviceItem> deviceList;
    private FirebaseFirestore db;

    public HistoryFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerHistory);

        if (recyclerView == null) {
            Toast.makeText(getContext(), "Error: RecyclerView not found", Toast.LENGTH_SHORT).show();
            return view;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        deviceList = new ArrayList<>();
        adapter = new DeviceHistoryAdapter(deviceList);
        recyclerView.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_LONG).show();
            startActivity(new Intent(getContext(), LoginActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
            return view;
        }

        loadDeviceHistory();

        return view;
    }

    private void loadDeviceHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        db.collection("users").document(userId)
                .collection("devices")
                .orderBy("linkedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    deviceList.clear();

                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(getContext(), "No linked devices yet", Toast.LENGTH_SHORT).show();
                    }

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            DeviceItem item = new DeviceItem(
                                    doc.getString("type"),
                                    doc.getString("status"),
                                    doc.getLong("linkedAt"),
                                    doc.getString("linkId")
                            );
                            deviceList.add(item);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public static class DeviceItem {
        public String type, status, linkId;
        public long linkedAt;

        public DeviceItem(String type, String status, Long linkedAt, String linkId) {
            this.type = type;
            this.status = status != null ? status : "unknown";
            this.linkedAt = linkedAt != null ? linkedAt : 0;
            this.linkId = linkId;
        }
    }
}