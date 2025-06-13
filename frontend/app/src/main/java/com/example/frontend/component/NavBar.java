package com.example.frontend.component;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.frontend.Connexion;
import com.example.frontend.Groups;
import com.example.frontend.Inscription;
import com.example.frontend.MainActivity;
import com.example.frontend.PlantsHistory;
import com.example.frontend.R;
import com.example.frontend.Recherche;
import com.example.frontend.utils.TokenManager;

public class NavBar extends ConstraintLayout {

    private TokenManager tokenManager;

    public NavBar(Context context) {
        super(context);
        init(context);
    }

    public NavBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NavBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Gonfler le layout XML de ton composant
        LayoutInflater.from(context).inflate(R.layout.nav_bar, this, true);

        // Initialiser le TokenManager
        tokenManager = new TokenManager(context);

        // Récupération des éléments de la NavBar
        LinearLayout historic = findViewById(R.id.historic);
        LinearLayout home = findViewById(R.id.home);
        LinearLayout contacts = findViewById(R.id.contacts);

        historic.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                Intent intent = new Intent(context, PlantsHistory.class);
                context.startActivity(intent);
                if(context instanceof PlantsHistory) {
                    ((PlantsHistory) context).finish();
                }
            } else {
                redirectToSignup(context);
            }
        });

        home.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
            if(context instanceof MainActivity) {
                ((MainActivity) context).finish();
            }
        });

        contacts.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                Intent intent = new Intent(context, Groups.class);
                context.startActivity(intent);
                if(context instanceof Groups) {
                    ((Groups) context).finish();
                }
            } else {
                redirectToSignup(context);
            }
        });
    }

    /**
     * Vérifie si l'utilisateur est connecté en vérifiant la présence d'un token valide
     * @return true si l'utilisateur est connecté, false sinon
     */
    private boolean isUserLoggedIn() {
        String token = tokenManager.getAccessToken();
        return token != null && !token.isEmpty();
    }

    /**
     * Redirige l'utilisateur vers la page d'inscription
     * @param context Le contexte actuel
     */
    private void redirectToSignup(Context context) {
        Toast.makeText(context, "Vous devez être connecté pour accéder à cette fonctionnalité", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(context, Connexion.class);
        context.startActivity(intent);
    }
}