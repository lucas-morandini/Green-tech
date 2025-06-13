package com.example.frontend.component;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;


import com.example.frontend.Groups;
import com.example.frontend.MainActivity;
import com.example.frontend.PlantsHistory;
import com.example.frontend.R;
import com.example.frontend.Recherche;

public class SearchBar extends ConstraintLayout {

    public SearchBar(Context context) {
        super(context);
        init(context);
    }

    public SearchBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SearchBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.search_bar, this, true);
        ImageButton search_button = findViewById(R.id.search_button);
        EditText input_text = findViewById(R.id.search_input);
        search_button.setOnClickListener(v -> {
            String text = input_text.getText().toString();
            if(context instanceof MainActivity) {
                Intent intent = new Intent(context, Recherche.class);
                intent.putExtra("recherche_text", text);
                context.startActivity(intent);
            }
            if(context instanceof Recherche)
            {
                Intent intent = new Intent(context, Recherche.class);
                intent.putExtra("recherche_text", text);
                context.startActivity(intent);
                ((Recherche) context).finish();
            }
            if(context instanceof Groups)
            {
                String query = input_text.getText().toString();
                ((Groups) context).filterGroups(query);
            }
        });
    }
}
