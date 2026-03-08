package com.example.carelink.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carelink.R;
import com.example.carelink.models.TimeSlot;
import java.util.List;

public class DoctorSlotAdapter extends RecyclerView.Adapter<DoctorSlotAdapter.ViewHolder> {

    private List<TimeSlot> slots;
    private OnSlotClickListener listener;

    public interface OnSlotClickListener {
        void onSlotClick(TimeSlot slot);
    }

    public DoctorSlotAdapter(List<TimeSlot> slots, OnSlotClickListener listener) {
        this.slots = slots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimeSlot slot = slots.get(position);

        holder.tvTime.setText(slot.getTimeRange());
        holder.tvDoctorName.setText(slot.getDoctor().getName());
        holder.tvSpecialty.setText(slot.getDoctor().getSpecialty() + " | " + slot.getDuration());
        holder.tvPrice.setText("RM " + String.format("%.2f", slot.getPrice()));

        holder.btnBook.setOnClickListener(v -> {
            if (listener != null) listener.onSlotClick(slot);
        });
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvDoctorName, tvSpecialty, tvPrice;
        Button btnBook;
        ImageView ivDoctor;

        ViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnBook = itemView.findViewById(R.id.btnBook);
            ivDoctor = itemView.findViewById(R.id.ivDoctor);
        }
    }
}