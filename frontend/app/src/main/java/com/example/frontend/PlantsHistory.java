package com.example.frontend;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.adapter.PlantAdapter;
import com.example.frontend.model.Plant;
import com.example.frontend.model.Identification;
import com.example.frontend.utils.Constants;
import com.example.frontend.utils.TokenManager;
import com.example.frontend.utils.UIUtils;
import com.example.frontend.utils.Scan;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlantsHistory extends AppCompatActivity implements PlantAdapter.OnItemClickListener {

    private RecyclerView plantRecyclerView;
    private PlantAdapter plantAdapter;
    private List<Plant> plantList;
    private TokenManager tokenManager;
    private ProgressDialog progressDialog;

    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private static final String API_URL = Constants.API_BASE_URL + "/user/plants/history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plants_history);

        // Initialiser le TokenManager
        tokenManager = new TokenManager(this);

        // Configuration du ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Chargement de votre historique...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);

        // Initialiser la RecyclerView
        plantRecyclerView = findViewById(R.id.plant_list_recycler);
        plantRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialiser la liste des plantes
        plantList = new ArrayList<>();

        // Configurer l'adaptateur avec le listener de clic
        plantAdapter = new PlantAdapter(plantList);
        plantAdapter.setOnItemClickListener(this); // AJOUT : Listener pour navigation
        plantRecyclerView.setAdapter(plantAdapter);

        UIUtils.hideSystemUI(this);

        // Charger les données depuis le backend
        loadPlantsFromBackend();
    }

    // === GESTION DU CLIC SUR UN ÉLÉMENT ===
    @Override
    public void onItemClick(int position) {
        if (position >= 0 && position < plantList.size()) {
            Plant selectedPlant = plantList.get(position);
            openPlantDetails(selectedPlant);
            Log.d("PlantsHistory", "Clic sur plante: " + selectedPlant.getScientificName() + " (position " + position + ")");
        }
    }

    // === NAVIGATION VERS DETAILSPLANT ===
    private void openPlantDetails(Plant plant) {
        Log.d("Navigation", "=== DÉBUT NAVIGATION ===");
        Log.d("Navigation", "Plant: " + plant.getScientificName());

        Intent intent = new Intent(this, DetailsPlant.class);

        // Données de base
        intent.putExtra("description", plant.getDescription() != null ? plant.getDescription() : "Aucune description disponible.");
        intent.putExtra("common_name", plant.getCommonName() != null ? plant.getCommonName() : "Nom commun inconnu");
        intent.putExtra("sc_name", plant.getScientificName() != null ? plant.getScientificName() : "Nom scientifique inconnu");

        // Image de la plante (depuis le nouveau champ)
        String plantImage = plant.getImage() != null ? plant.getImage() : "";
        intent.putExtra("image", plantImage);

        if (!plantImage.isEmpty()) {
            Log.d("Navigation", "Image de plante incluse (longueur: " + plantImage.length() + ")");
        } else {
            Log.d("Navigation", "Aucune image de plante disponible");
        }

        // Détails écologiques formatés
        String ecologicalDetailsString = formatEcologicalDetails(plant);
        intent.putExtra("ecological_details", ecologicalDetailsString);

        // Scores écologiques (déjà en format 0-100 depuis le backend)
        intent.putExtra("nitrogen_score", plant.getNitrogenFixationScore());
        intent.putExtra("soil_score", plant.getSoilStructureScore());
        intent.putExtra("water_score", plant.getWaterRetentionScore());

        // Convertir les identifications en scans pour la carte
        ArrayList<Scan> scans = createScansFromPlant(plant);
        intent.putParcelableArrayListExtra("scans", scans);

        Log.d("Navigation", "Scores: Sol=" + plant.getSoilStructureScore() +
                ", Eau=" + plant.getWaterRetentionScore() +
                ", Azote=" + plant.getNitrogenFixationScore());
        Log.d("Navigation", "Scans créés: " + scans.size());
        Log.d("Navigation", "Identifications avec coordonnées: " + plant.getIdentifications().size());

        startActivity(intent);
        Log.d("Navigation", "=== FIN NAVIGATION ===");
    }

    // === FORMATER LES DÉTAILS ÉCOLOGIQUES ===
    private String formatEcologicalDetails(Plant plant) {
        StringBuilder details = new StringBuilder();

        // Ajouter les détails écologiques de la Map
        Map<String, String> ecoDetails = plant.getEcologicalDetails();
        if (ecoDetails != null && !ecoDetails.isEmpty()) {
            String habitat = ecoDetails.get("habitat");
            if (habitat != null && !habitat.equals("Non spécifié")) {
                details.append("🌍 Habitat : ").append(habitat).append("\n\n");
            }

            String growthRate = ecoDetails.get("growthRate");
            if (growthRate != null && !growthRate.equals("Non spécifié")) {
                details.append("🌱 Croissance : ").append(growthRate).append("\n\n");
            }

            String lifespan = ecoDetails.get("lifespan");
            if (lifespan != null && !lifespan.equals("Non spécifié")) {
                details.append("⏰ Durée de vie : ").append(lifespan).append("\n\n");
            }
        }

        // Ajouter des informations sur les services écosystémiques
        details.append("🔬 Services écosystémiques :\n");
        if (plant.getSoilStructureScore() > 0) {
            details.append("• Amélioration du sol : ").append(plant.getSoilStructureScore()).append("%\n");
        }
        if (plant.getWaterRetentionScore() > 0) {
            details.append("• Rétention d'eau : ").append(plant.getWaterRetentionScore()).append("%\n");
        }
        if (plant.getNitrogenFixationScore() > 0) {
            details.append("• Fixation d'azote : ").append(plant.getNitrogenFixationScore()).append("%\n");
        }

        // Ajouter info sur les identifications
        int count = plant.getIdentificationCount() > 0 ? plant.getIdentificationCount() : plant.getIdentifications().size();
        details.append("\n📊 Identifiée ").append(count).append(" fois");

        return details.length() > 0 ? details.toString() : "Aucun détail écologique disponible.";
    }

    // === CRÉER DES SCANS DEPUIS LES IDENTIFICATIONS ===
    private ArrayList<Scan> createScansFromPlant(Plant plant) {
        ArrayList<Scan> scans = new ArrayList<>();

        // Vérifier si on a des identifications avec coordonnées valides
        if (plant.getIdentifications() != null && !plant.getIdentifications().isEmpty()) {
            for (Identification identification : plant.getIdentifications()) {
                if (hasValidCoordinates(identification)) {
                    Scan scan = createScanFromIdentification(identification);
                    scans.add(scan);
                    Log.d("Navigation", "Scan réel créé: lat=" + identification.getLatitude() +
                            ", lon=" + identification.getLongitude());
                }
            }
        }

        // Si aucun scan réel, créer des scans factices basés sur le nombre d'identifications
        if (scans.isEmpty()) {
            int scanCount = Math.max(1, plant.getIdentificationCount());
            for (int i = 0; i < scanCount; i++) {
                scans.add(createFacticeScan(i));
            }
            Log.d("Navigation", "Scans factices créés: " + scans.size());
        } else {
            Log.d("Navigation", "Scans réels utilisés: " + scans.size());
        }

        return scans;
    }

    // === VÉRIFIER COORDONNÉES VALIDES ===
    private boolean hasValidCoordinates(Identification identification) {
        return identification.getLatitude() != 0.0 && identification.getLongitude() != 0.0;
    }

    // === CRÉER SCAN DEPUIS IDENTIFICATION ===
    private Scan createScanFromIdentification(Identification identification) {
        GeoPoint point = new GeoPoint(identification.getLatitude(), identification.getLongitude());
        return new Scan(point, identification.getDate());
    }

    // === CRÉER SCAN FACTICE ===
    private Scan createFacticeScan(int index) {
        // Base : Lyon, France avec variation pour éviter superposition
        double baseLat = 45.7640;
        double baseLon = 4.8357;

        // Variation selon l'index et aléatoire
        double latVariation = (index * 0.002) + (Math.random() - 0.5) * 0.001;
        double lonVariation = (index * 0.002) + (Math.random() - 0.5) * 0.001;

        GeoPoint point = new GeoPoint(baseLat + latVariation, baseLon + lonVariation);

        // Date avec variation de quelques jours
        Date date = new Date(System.currentTimeMillis() - (index * 24 * 60 * 60 * 1000L));

        return new Scan(point, date);
    }

    // === CHARGEMENT DEPUIS LE BACKEND ===
    private void loadPlantsFromBackend() {
        String token = tokenManager.getAccessToken();

        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Erreur d'authentification", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressDialog.show();

        Request request = new Request.Builder()
                .url(API_URL)
                .get()
                .header("Authorization", "Bearer " + token)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(PlantsHistory.this, "Erreur réseau : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Charger des données de test en cas d'erreur pour le développement
                    loadSamplePlants();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                runOnUiThread(() -> progressDialog.dismiss());

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        if (response.code() == 401) {
                            Toast.makeText(PlantsHistory.this, "Session expirée, veuillez vous reconnecter", Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 404) {
                            Toast.makeText(PlantsHistory.this, "Aucun historique trouvé", Toast.LENGTH_SHORT).show();
                            showEmptyState();
                        } else {
                            Toast.makeText(PlantsHistory.this, "Erreur serveur", Toast.LENGTH_SHORT).show();
                            loadSamplePlants();
                        }
                    });
                }

                String responseBody = response.body().string();
                response.body().close();
                Log.d("PlantsHistory", "API Response: " + responseBody);

                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray plantsArray = jsonResponse.getJSONArray("plants");

                    List<Plant> newPlantList = new ArrayList<>();

                    for (int i = 0; i < plantsArray.length(); i++) {
                        JSONObject plantJson = plantsArray.getJSONObject(i);
                        Plant plant = parsePlantFromJson(plantJson);
                        if (plant != null) {
                            newPlantList.add(plant);
                        }
                    }

                    runOnUiThread(() -> {
                        plantList.clear();
                        plantList.addAll(newPlantList);
                        plantAdapter.notifyDataSetChanged();

                        if (plantList.isEmpty()) {
                            showEmptyState();
                        } else {
                            hideEmptyState();
                        }
                    });

                } catch (JSONException e) {
                    Log.e("PlantsHistory", "JSON parsing error: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(PlantsHistory.this, "Erreur de traitement des données", Toast.LENGTH_SHORT).show();
                        loadSamplePlants();
                    });
                }
            }
        });
    }

    // === PARSING JSON AMÉLIORÉ ===
    private Plant parsePlantFromJson(JSONObject plantJson) {
        try {
            Plant plant = new Plant();
            plant.JsonToPlant(plantJson);

            // NOUVEAU : Parser les vraies identifications avec coordonnées depuis le backend
            if (plantJson.has("identifications")) {
                JSONArray identificationsArray = plantJson.getJSONArray("identifications");

                for (int i = 0; i < identificationsArray.length(); i++) {
                    JSONObject identificationJson = identificationsArray.getJSONObject(i);

                    Identification identification = new Identification();
                    identification.setId(identificationJson.optLong("id", 0));
                    identification.setLatitude(identificationJson.optDouble("latitude", 0.0));
                    identification.setLongitude(identificationJson.optDouble("longitude", 0.0));

                    // Parser la date avec format ISO
                    String dateString = identificationJson.optString("date", "");
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                        Date date = sdf.parse(dateString);
                        identification.setDate(date);
                    } catch (Exception e) {
                        identification.setDate(new Date());
                        Log.w("PlantsHistory", "Could not parse date: " + dateString);
                    }

                    // Ajouter seulement si on a des coordonnées valides
                    if (identification.getLatitude() != 0.0 || identification.getLongitude() != 0.0) {
                        plant.addIdentification(identification);
                        Log.d("PlantsHistory", "Identification avec coordonnées ajoutée: lat=" +
                                identification.getLatitude() + ", lon=" + identification.getLongitude());
                    }
                }
            }

            if (plant.getId() == 0) {
                Log.w("PlantsHistory", "Plant ID is 0, parsing may have failed");
            }

            Log.d("PlantsHistory", "Plant parsée: " + plant.getScientificName() +
                    " (ID: " + plant.getId() + ", Identifications: " + plant.getIdentificationCount() +
                    ", Coordonnées: " + plant.getIdentifications().size() + ")");

            return plant;

        } catch (Exception e) {
            Log.e("PlantsHistory", "Error parsing plant JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // === ÉTAT VIDE ===
    private void showEmptyState() {
        Toast.makeText(this, "Aucune plante dans votre historique", Toast.LENGTH_LONG).show();
    }

    private void hideEmptyState() {
        // Masquer l'état vide si nécessaire
    }

    // === DONNÉES DE TEST ===
    private void loadSamplePlants() {
        // Exemple 1: Scores élevés (≥70%) - Indicateurs verts/bleus
        Map<String, String> ecologicalDetails1 = new HashMap<>();
        ecologicalDetails1.put("habitat", "Meadows, fields, gardens");
        ecologicalDetails1.put("growthRate", "Medium");
        ecologicalDetails1.put("lifespan", "Perennial");

        Plant plant1 = new Plant(1, "Trifolium pratense", "Red Clover", "Fabaceae",
                "https://www.tela-botanica.org/bdtfx-nn-69412", "15 April 2025",
                new ArrayList<>(), 85, 75, 90, ecologicalDetails1,
                "Nitrogen-fixing legume excellent for soil improvement");
        plant1.setLatestScanDate("18 April 2025");
        plant1.setIdentificationCount(3);

        // Ajouter des identifications avec vraies coordonnées pour test
        Identification id1 = new Identification();
        id1.setId(1);
        id1.setLatitude(45.7640);
        id1.setLongitude(4.8357);
        id1.setDate(new Date());
        plant1.addIdentification(id1);

        Identification id2 = new Identification();
        id2.setId(2);
        id2.setLatitude(45.7650);
        id2.setLongitude(4.8367);
        id2.setDate(new Date());
        plant1.addIdentification(id2);

        plantList.add(plant1);

        // Exemple 2: Scores moyens (40-69%) - Indicateurs oranges
        Map<String, String> ecologicalDetails2 = new HashMap<>();
        ecologicalDetails2.put("habitat", "Gardens, borders");
        ecologicalDetails2.put("growthRate", "Fast");
        ecologicalDetails2.put("lifespan", "Perennial");

        Plant plant2 = new Plant(2, "Alstroemeria", "Peruvian Lily", "Alstroemeriaceae",
                "", "10 April 2025", new ArrayList<>(),
                45, 60, 55, ecologicalDetails2,
                "Ornamental flowering plant with moderate ecological benefits");
        plant2.setLatestScanDate("12 April 2025");
        plant2.setIdentificationCount(5);
        plantList.add(plant2);

        // Exemple 3: Scores faibles (1-39%) - Indicateurs oranges pâles
        Map<String, String> ecologicalDetails3 = new HashMap<>();
        ecologicalDetails3.put("habitat", "Various environments");
        ecologicalDetails3.put("growthRate", "Slow");
        ecologicalDetails3.put("lifespan", "Annual");

        Plant plant3 = new Plant(3, "Unknown species", "Unknown plant", "Unknown family",
                "", "08 April 2025", new ArrayList<>(),
                25, 30, 15, ecologicalDetails3,
                "Plant with limited ecological benefits");
        plant3.setLatestScanDate("20 April 2025");
        plant3.setIdentificationCount(1);
        plantList.add(plant3);

        // Exemple 4: Aucun score (0%) - Indicateurs gris
        Map<String, String> ecologicalDetails4 = new HashMap<>();
        ecologicalDetails4.put("habitat", "Non spécifié");
        ecologicalDetails4.put("growthRate", "Non spécifié");
        ecologicalDetails4.put("lifespan", "Non spécifié");

        Plant plant4 = new Plant(4, "Plantae incertae", "Unidentified plant", "Unknown",
                "", "05 April 2025", new ArrayList<>(),
                0, 0, 0, ecologicalDetails4,
                "Plant without ecological data");
        plant4.setLatestScanDate("05 April 2025");
        plant4.setIdentificationCount(2);
        plantList.add(plant4);

        // Notifier l'adapter que les données ont changé
        plantAdapter.notifyDataSetChanged();

        Log.d("PlantsHistory", "Données de test chargées: " + plantList.size() + " plantes");
    }
}