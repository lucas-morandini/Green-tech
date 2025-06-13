package com.example.frontend.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.model.User;
import com.example.frontend.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ViewHolder> {

    private List<User> users = new ArrayList<>();
    private final Random random = new Random();

    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView initialsText, fullNameText;

        public ViewHolder(View view) {
            super(view);
            initialsText = view.findViewById(R.id.initials_text);
            fullNameText = view.findViewById(R.id.full_name_text);
        }
    }

    @NonNull
    @Override
    public ParticipantAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_participant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantAdapter.ViewHolder holder, int position) {
        User user = users.get(position);
        String initials = user.getInitials();
        holder.initialsText.setText(initials);
        String full_name = user.getFirst_name() + " " + user.getName();
        holder.fullNameText.setText(full_name);

        // Couleur aléatoire pour la bulle
        int color = ColorUtils.getColorForUserId(user.getId());
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        holder.initialsText.setBackground(drawable);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }
}