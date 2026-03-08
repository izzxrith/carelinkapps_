package com.example.carelink;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carelink.adapters.DeviceHistoryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private DeviceHistoryAdapter adapter;
    private List<DeviceItem> logList;
    private FirebaseFirestore db;

    public HistoryFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        db = FirebaseFirestore.getInstance();
        RecyclerView recyclerView = view.findViewById(R.id.recyclerHistory);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            logList = new ArrayList<>();
            adapter = new DeviceHistoryAdapter(logList);
            recyclerView.setAdapter(adapter);
        }

        loadPredictiveSafetyHistory();

        return view;
    }

    private void loadPredictiveSafetyHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // FETCHING 90-DAY BEHAVIORAL INSIGHTS
        db.collection("users").document(user.getUid())
                .collection("behavioral_insights")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    logList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String emotion = doc.getString("emotion");
                        Long hr = doc.getLong("heartRate");
                        Long ts = doc.getLong("timestamp");
                        
                        logList.add(new DeviceItem(
                                "Safety Log",
                                emotion + " (" + hr + " BPM)",
                                ts != null ? ts : 0,
                                doc.getId()
                        ));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("History", "Failed to load safety history"));
    }

    public static class DeviceItem {
        public String type, status, linkId;
        public long linkedAt;

        public DeviceItem(String type, String status, long linkedAt, String linkId) {
            this.type = type;
            this.status = status;
            this.linkedAt = linkedAt;
            this.linkId = linkId;
        }
    }
}