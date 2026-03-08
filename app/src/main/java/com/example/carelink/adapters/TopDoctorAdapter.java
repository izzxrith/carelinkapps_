package com.example.carelink.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carelink.DashboardActivity;
import com.example.carelink.R;
import java.util.List;

public class TopDoctorAdapter extends RecyclerView.Adapter<TopDoctorAdapter.ViewHolder> {

    private List<DashboardActivity.DoctorItem> doctors;
    private OnDoctorClickListener listener;

    public interface OnDoctorClickListener {
        void onDoctorClick(DashboardActivity.DoctorItem doctor);
    }

    public TopDoctorAdapter(List<DashboardActivity.DoctorItem> doctors, OnDoctorClickListener listener) {
        this.doctors = doctors;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_doctor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DashboardActivity.DoctorItem doctor = doctors.get(position);

        holder.ivDoctor.setImageResource(doctor.imageRes);
        holder.tvName.setText(doctor.name);
        holder.tvSpecialty.setText(doctor.specialty);
        holder.tvRating.setText("★ " + doctor.rating);
        holder.tvDistance.setText("• " + doctor.distance);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDoctorClick(doctor);
        });
    }

    @Override
    public int getItemCount() {
        return doctors != null ? doctors.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDoctor;
        TextView tvName, tvSpecialty, tvRating, tvDistance;

        ViewHolder(View itemView) {
            super(itemView);
            ivDoctor = itemView.findViewById(R.id.ivDoctor);
            tvName = itemView.findViewById(R.id.tvName);
            tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvDistance = itemView.findViewById(R.id.tvDistance);
        }
    }
}