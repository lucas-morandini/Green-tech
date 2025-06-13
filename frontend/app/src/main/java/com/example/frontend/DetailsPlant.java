package com.example.frontend;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend.component.CircularProgressView;
import com.example.frontend.utils.Scan;
import com.example.frontend.utils.UIUtils;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DetailsPlant extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_details_plant);

        // Récupération des infos de base de la plante
        String description = getIntent().getStringExtra("description");
        String common_name = getIntent().getStringExtra("common_name");
        String sc_name = getIntent().getStringExtra("sc_name");
        String image_byte64 = getIntent().getStringExtra("image");
        String ecological_details = getIntent().getStringExtra("ecological_details");

        // Récupérer les scores écologiques (valeurs par défaut à 0)
        int nitrogen_score = getIntent().getIntExtra("nitrogen_score", 0);
        int soil_score = getIntent().getIntExtra("soil_score", 0);
        int water_score = getIntent().getIntExtra("water_score", 0);

        // Log des données reçues pour débogage
        Log.d("DetailsPlant", "=== DONNÉES REÇUES ===");
        Log.d("DetailsPlant", "Nom scientifique: " + sc_name);
        Log.d("DetailsPlant", "Nom commun: " + common_name);
        Log.d("DetailsPlant", "Scores - Sol: " + soil_score + ", Eau: " + water_score + ", Azote: " + nitrogen_score);
        Log.d("DetailsPlant", "Image présente: " + (image_byte64 != null && !image_byte64.isEmpty()));

        // Configuration des CircularProgressView
        CircularProgressView azote_progress = findViewById(R.id.azote_progress);
        CircularProgressView sol_progress = findViewById(R.id.sol_progress);
        CircularProgressView water_progress = findViewById(R.id.water_progress);

        azote_progress.setProgress(nitrogen_score);
        sol_progress.setProgress(soil_score);
        water_progress.setProgress(water_score);

        // Configuration des TextViews
        TextView sc_name_tv = findViewById(R.id.plant_name_latin);
        TextView common_name_tv = findViewById(R.id.plant_name_fr);
        TextView description_tv = findViewById(R.id.plant_description);
        TextView observations_tv = findViewById(R.id.observations);
        ImageView image_iv = findViewById(R.id.plant_image);

        sc_name_tv.setText(sc_name != null ? sc_name : "Nom scientifique inconnu");
        common_name_tv.setText(common_name != null ? common_name : "Nom commun inconnu");
        description_tv.setText(description != null ? description : "Aucune description disponible");
        observations_tv.setText("Aucune observation disponible");

        // NOUVEAU : Gestion de l'image de la plante
        setupPlantImage(image_byte64, image_iv);

        // Configuration de la carte et du toggle
        setupMapAndToggle();

        UIUtils.hideSystemUI(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // NOUVELLE MÉTHODE : Configuration de l'image de la plante
    private void setupPlantImage(String image_byte64, ImageView image_iv) {
        if (image_byte64 != null && !image_byte64.trim().isEmpty()) {
            // Image fournie - la décoder et l'afficher
            try {
                decodeBase64AndSetImage(image_byte64, image_iv);
                Log.d("DetailsPlant", "Image base64 décodée et affichée");
            } catch (Exception e) {
                Log.e("DetailsPlant", "Erreur décodage image base64: " + e.getMessage());
                setDefaultPlantImage(image_iv);
            }
        } else {
            // Pas d'image - afficher une image par défaut
            setDefaultPlantImage(image_iv);
            Log.d("DetailsPlant", "Aucune image fournie, image par défaut utilisée");
        }
    }

    // NOUVELLE MÉTHODE : Image par défaut
    private void setDefaultPlantImage(ImageView image_iv) {
        // Tu peux remplacer par une vraie image de plante par défaut
        image_iv.setImageResource(R.drawable.ic_plant);
        image_iv.setScaleType(ImageView.ScaleType.CENTER);
        image_iv.setBackgroundColor(getResources().getColor(android.R.color.white));
    }

    // MÉTHODE REFACTORISÉE : Configuration carte et toggle
    private void setupMapAndToggle() {
        LinearLayout modeViewBlock = findViewById(R.id.mode_view_block);
        CardView card = findViewById(R.id.plant_info_card);
        MapView map = findViewById(R.id.plant_map_view);
        TextView mode_view_txt = findViewById(R.id.mode_view_txt);
        ImageView mode_view_ic = findViewById(R.id.mode_view_ic);

        // Configuration de la carte
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        // Récupérer et afficher les scans sur la carte
        ArrayList<Scan> scans = getIntent().getParcelableArrayListExtra("scans");
        setupMapMarkers(map, scans);

        // Configuration du toggle entre fiche et carte
        card.post(() -> {
            final int cardHeight = card.getHeight();
            Log.d("DetailsPlant", "Hauteur de la card: " + cardHeight + "px");

            modeViewBlock.setOnClickListener(v -> {
                if (card.getVisibility() == View.VISIBLE) {
                    // Passer à la vue carte
                    card.setVisibility(View.GONE);
                    map.getLayoutParams().height = Math.max(cardHeight, 300); // Hauteur minimum
                    map.requestLayout();
                    map.setVisibility(View.VISIBLE);
                    mode_view_txt.setText("Information de la plante");
                    mode_view_ic.setImageResource(R.drawable.ic_info);
                    Log.d("DetailsPlant", "Vue carte activée");
                } else {
                    // Passer à la vue fiche
                    map.setVisibility(View.GONE);
                    card.setVisibility(View.VISIBLE);
                    mode_view_txt.setText("Localiser mes scans");
                    mode_view_ic.setImageResource(R.drawable.icon_map);
                    Log.d("DetailsPlant", "Vue fiche activée");
                }
            });
        });
    }

    // NOUVELLE MÉTHODE : Configuration des marqueurs sur la carte
    private void setupMapMarkers(MapView map, ArrayList<Scan> scans) {
        if (scans != null && !scans.isEmpty()) {
            Log.d("DetailsPlant", "Configuration de " + scans.size() + " marqueurs sur la carte");

            for (int i = 0; i < scans.size(); i++) {
                Scan scan = scans.get(i);
                GeoPoint point = scan.getScan_point();
                Date date = scan.getScan_date();

                Log.d("DetailsPlant", "Marqueur " + (i + 1) + ": lat=" + point.getLatitude() +
                        ", lon=" + point.getLongitude());

                Marker marker = new Marker(map);
                marker.setPosition(point);

                String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
                marker.setTitle("Scan " + (i + 1) + " - " + currentDate);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                // Couleur différente pour chaque marqueur (optionnel)
                // marker.setIcon(getResources().getDrawable(R.drawable.custom_marker));

                map.getOverlays().add(marker);

                // Centrer sur le dernier scan (le plus récent)
                if (i == scans.size() - 1) {
                    map.getController().setCenter(point);
                    Log.d("DetailsPlant", "Carte centrée sur le dernier scan");
                }
            }

            map.invalidate();
        } else {
            Log.w("DetailsPlant", "Aucun scan disponible pour affichage sur la carte");

            // Position par défaut si aucun scan
            GeoPoint defaultPoint = new GeoPoint(45.7640, 4.8357); // Lyon
            map.getController().setCenter(defaultPoint);

            Marker defaultMarker = new Marker(map);
            defaultMarker.setPosition(defaultPoint);
            defaultMarker.setTitle("Position approximative");
            defaultMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(defaultMarker);
        }
    }

    // MÉTHODE EXISTANTE : Décoder l'image base64
    private void decodeBase64AndSetImage(String base64Image, ImageView imageView) {
        try {
            // Nettoyer la chaîne base64 si elle contient un préfixe data:
            String cleanBase64 = base64Image;
            if (base64Image.contains(",")) {
                cleanBase64 = base64Image.substring(base64Image.indexOf(",") + 1);
            }

            byte[] decodedString = Base64.decode(cleanBase64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            if (decodedByte != null) {
                imageView.setImageBitmap(decodedByte);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Log.d("DetailsPlant", "Image bitmap créée: " + decodedByte.getWidth() + "x" + decodedByte.getHeight());
            } else {
                Log.e("DetailsPlant", "Échec création bitmap depuis base64");
                setDefaultPlantImage(imageView);
            }
        } catch (Exception e) {
            Log.e("DetailsPlant", "Erreur décodage base64: " + e.getMessage());
            setDefaultPlantImage(imageView);
        }
    }
}