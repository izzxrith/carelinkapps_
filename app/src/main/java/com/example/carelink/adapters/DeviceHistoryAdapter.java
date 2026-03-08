package com.example.carelink.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carelink.HistoryFragment;
import com.example.carelink.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeviceHistoryAdapter extends RecyclerView.Adapter<DeviceHistoryAdapter.ViewHolder> {

    private List<HistoryFragment.DeviceItem> devices;

    public DeviceHistoryAdapter(List<HistoryFragment.DeviceItem> devices) {
        this.devices = devices;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryFragment.DeviceItem device = devices.get(position);

        holder.tvDeviceType.setText(device.type != null ? device.type.toUpperCase() : "WATCH");
        holder.tvDeviceStatus.setText(device.status != null ? device.status : "unknown");

        String shortId = device.linkId != null && device.linkId.length() > 8
                ? device.linkId.substring(0, 8) + "..."
                : "N/A";
        holder.tvLinkId.setText("ID: " + shortId);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        String dateStr = device.linkedAt > 0 ? sdf.format(new Date(device.linkedAt)) : "Unknown";
        holder.tvLinkedDate.setText("Linked: " + dateStr);

        if ("active".equals(device.status)) {
            holder.tvDeviceStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
        } else {
            holder.tvDeviceStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.darker_gray));
        }
    }

    @Override
    public int getItemCount() {
        return devices != null ? devices.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceType, tvDeviceStatus, tvLinkId, tvLinkedDate;

        ViewHolder(View itemView) {
            super(itemView);
            tvDeviceType = itemView.findViewById(R.id.tvDeviceType);
            tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus);
            tvLinkId = itemView.findViewById(R.id.tvLinkId);
            tvLinkedDate = itemView.findViewById(R.id.tvLinkedDate);
        }
    }
}