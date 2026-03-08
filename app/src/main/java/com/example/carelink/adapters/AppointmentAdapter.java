package com.example.carelink.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carelink.R;
import com.example.carelink.models.Appointment;
import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Appointment> appointments;
    private OnAppointmentClickListener listener;

    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appointment);
    }

    public AppointmentAdapter(List<Appointment> appointments, OnAppointmentClickListener listener) {
        this.appointments = appointments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment apt = appointments.get(position);

        holder.tvDoctor.setText(apt.getDoctorName());
        holder.tvSpecialty.setText(apt.getSpecialty());
        holder.tvClinic.setText(apt.getClinicName());
        holder.tvDateTime.setText(apt.getDate() + " | " + apt.getTime());
        holder.tvPrice.setText("RM " + String.format("%.2f", apt.getPrice()));
        holder.tvStatus.setText(apt.getStatus().toUpperCase());

        int color;
        switch (apt.getStatus()) {
            case "upcoming": color = holder.itemView.getContext().getColor(R.color.blue); break;
            case "completed": color = holder.itemView.getContext().getColor(R.color.green); break;
            case "canceled": color = holder.itemView.getContext().getColor(R.color.red); break;
            default: color = holder.itemView.getContext().getColor(R.color.gray);
        }
        holder.tvStatus.setTextColor(color);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAppointmentClick(apt);
        });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDoctor, tvSpecialty, tvClinic, tvDateTime, tvPrice, tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvDoctor = itemView.findViewById(R.id.tvDoctor);
            tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
            tvClinic = itemView.findViewById(R.id.tvClinic);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}