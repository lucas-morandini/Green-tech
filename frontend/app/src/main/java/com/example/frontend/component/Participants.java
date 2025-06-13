package com.example.frontend.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.adapter.ParticipantAdapter;
import com.example.frontend.model.User;

import java.util.List;

public class Participants extends ConstraintLayout {

    private RecyclerView recyclerView;
    private ParticipantAdapter adapter;

    public Participants(Context context) {
        super(context);
        init(context);
    }

    public Participants(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Participants(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.participants, this, true);
        recyclerView = view.findViewById(R.id.participants_recycler);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        );
        adapter = new ParticipantAdapter();
        recyclerView.setAdapter(adapter);
    }

    public void setParticipants(List<User> users) {
        adapter.setUsers(users);
    }
}
