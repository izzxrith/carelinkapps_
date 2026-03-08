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

        if (student.isSos()) {
            holder.tvState.setText("SOS");
            holder.tvState.setBackgroundResource(R.drawable.badge_red);
            holder.tvState.setTextColor(Color.WHITE);
        } else if ("Warning".equals(state)) {
            holder.tvState.setBackgroundResource(R.drawable.badge_yellow);
            holder.tvState.setTextColor(Color.BLACK);
        } else {
            holder.tvState.setBackgroundResource(R.drawable.badge_green);
            holder.tvState.setTextColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBpm, tvState;
        LinearLayout container;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvBpm = itemView.findViewById(R.id.tvBpmStatus);
            tvState = itemView.findViewById(R.id.tvStateLabel);
            container = itemView.findViewById(R.id.containerStatus);
        }
    }
}