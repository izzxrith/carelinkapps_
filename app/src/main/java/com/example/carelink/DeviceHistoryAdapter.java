package com.example.carelink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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

        holder.tvDeviceType.setText(device.type != null ? device.type.toUpperCase() : "UNKNOWN");
        holder.tvDeviceStatus.setText(device.status);
        holder.tvLinkId.setText("ID: " + (device.linkId != null ? device.linkId.substring(0, 8) + "..." : "N/A"));

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date(device.linkedAt));
        holder.tvLinkedDate.setText("Linked: " + dateStr);

        if ("active".equals(device.status)) {
            holder.tvDeviceStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
        } else {
            holder.tvDeviceStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.darker_gray));
        }
    }

    @Override
    public int getItemCount() {
        return devices.size();
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