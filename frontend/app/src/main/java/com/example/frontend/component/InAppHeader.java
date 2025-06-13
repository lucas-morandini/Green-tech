package com.example.frontend.component;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.constraintlayout.widget.ConstraintLayout;


import com.example.frontend.R;

public class InAppHeader extends ConstraintLayout {

    public InAppHeader(Context context) {
        super(context);
        init(context);
    }

    public InAppHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public InAppHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Gonfler le layout XML de ton composant
        LayoutInflater.from(context).inflate(R.layout.in_app_header, this, true);

        FrameLayout retour = findViewById(R.id.header_back_button);

        retour.setOnClickListener(v -> {
            if (context instanceof Activity) {
                ((Activity) context).onBackPressed();
                ((Activity) context).finish();
            }
        });
    }
}
