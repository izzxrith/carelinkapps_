package com.example.carelink.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carelink.R;
import com.example.carelink.models.StudentStatus;

import java.util.List;

public class StudentStatusAdapter extends RecyclerView.Adapter<StudentStatusAdapter.ViewHolder> {

    private List<StudentStatus> studentList;
    private Context context;

    public StudentStatusAdapter(List<StudentStatus> studentList, Context context) {
        this.studentList = studentList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentStatus student = studentList.get(position);
        holder.tvName.setText(student.getName());
        holder.tvBpm.setText(student.getHeartRate() > 0 ? student.getHeartRate() + " BPM" : "-- BPM");
        
        String state = student.getState();
        holder.tvState.setText(state);
        
        // SK PINJI: Show Predictive Location Status
        if (holder.tvLocation != null) {
            holder.tvLocation.setText(student.getLocationStatus());
        }

        if (student.isSos()) {
            holder.tvState.setText("SOS");
            holder.tvState.setBackgroundResource(R.drawable.badge_red);
            holder.tvState.setTextColor(Color.parseColor("#D32F2F"));
        } else if ("Warning".equals(state)) {
            holder.tvState.setText("Agitated");
            holder.tvState.setBackgroundResource(R.drawable.badge_yellow);
            holder.tvState.setTextColor(Color.parseColor("#F57C00"));
        } else {
            holder.tvState.setText("Normal");
            holder.tvState.setBackgroundResource(R.drawable.badge_green);
            holder.tvState.setTextColor(Color.parseColor("#388E3C"));
        }
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBpm, tvState, tvLocation;
        LinearLayout container;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvBpm = itemView.findViewById(R.id.tvBpmStatus);
            tvState = itemView.findViewById(R.id.tvStateLabel);
            tvLocation = itemView.findViewById(R.id.tvLocationStatus);
            container = itemView.findViewById(R.id.containerStatus);
        }
    }
}