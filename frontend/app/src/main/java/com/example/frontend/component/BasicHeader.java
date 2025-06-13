package com.example.frontend.component;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;


import com.example.frontend.MainActivity;
import com.example.frontend.Notifications;
import com.example.frontend.R;
import com.example.frontend.utils.UIUtils;

public class BasicHeader extends ConstraintLayout {

    public BasicHeader(Context context) {
        super(context);
        init(context);
    }

    public BasicHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BasicHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Gonfler le layout XML de ton composant
        LayoutInflater.from(context).inflate(R.layout.basic_header, this, true);
        FrameLayout notif_button = findViewById(R.id.bloc_notif);
        TextView badge_nb_notif = findViewById(R.id.badge_notif);
        if(context instanceof Activity)
        {
            UIUtils.getNotifications(context,
            notifications -> ((Activity) context).runOnUiThread(() -> {
                // ici tu mets à jour ton UI
                int notifCount = notifications.size();
                Log.d("NOTIFS", "📥 Notifications reçues : " + notifications.size());
                if (notifCount > 0) {
                    badge_nb_notif.setVisibility(View.VISIBLE);
                    badge_nb_notif.setText(notifCount > 9 ? "9+" : String.valueOf(notifCount));
                } else {
                    badge_nb_notif.setVisibility(View.GONE);
                }

            }),
            error -> ((Activity) context).runOnUiThread(() -> {
                // ici tu mets à jour ton UI
                Log.d("API CALL", error.toString());
            }));

            notif_button.setOnClickListener(v -> {
                Intent intent = new Intent(context, Notifications.class);
                context.startActivity(intent);
            });
        }
    }
}
