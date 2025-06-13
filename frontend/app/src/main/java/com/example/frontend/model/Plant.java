package com.example.frontend.model;

import android.util.Log;

import com.example.frontend.adapter.ListItem;
import com.example.frontend.adapter.ListItemType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Plant implements ListItem {
    private long id;
    private String scientificName;    // Nom scientifique
    private String commonName;        // Nom commun
    private String family;            // Famille botanique
    private String telaBotanicaUrl;   // URL Tela Botanica

    private String description;
    private String date;              // Date d'identification
    private String latestScanDate;    // Date de la dernière identification
    private String image;             // Image base64 de la plante (NOUVEAU)
    private ArrayList<Identification> identifications;
    private int identificationCount;  // Nombre total d'identifications depuis le backend

    // Scores écologiques
    private int soilStructureScore;   // Score de structure du sol (0-100)
    private int waterRetentionScore;  // Score de rétention d'eau (0-100)
    private int nitrogenFixationScore; // Score de fixation d'azote (0-100)

    // Détails écologiques (stockés dans une Map pour flexibilité)
    private Map<String, String> ecologicalDetails;

    // Constructeur complet (avec image)
    public Plant(long id, String scientificName, String commonName, String family,
                 String telaBotanicaUrl, String date, ArrayList<Identification> identifications,
                 int soilStructureScore, int waterRetentionScore, int nitrogenFixationScore,
                 Map<String, String> ecologicalDetails, String description, String image) {
        this.id = id;
        this.scientificName = scientificName;
        this.commonName = commonName;
        this.family = family;
        this.telaBotanicaUrl = telaBotanicaUrl;
        this.date = date;
        this.latestScanDate = date; // Par défaut, même date
        this.identifications = identifications;
        this.soilStructureScore = soilStructureScore;
        this.waterRetentionScore = waterRetentionScore;
        this.nitrogenFixationScore = nitrogenFixationScore;
        this.ecologicalDetails = ecologicalDetails;
        this.description = description;
        this.image = image; // NOUVEAU
        this.identificationCount = identifications != null ? identifications.size() : 1;
    }

    // Constructeur complet (sans image - pour compatibilité)
    public Plant(long id, String scientificName, String commonName, String family,
                 String telaBotanicaUrl, String date, ArrayList<Identification> identifications,
                 int soilStructureScore, int waterRetentionScore, int nitrogenFixationScore,
                 Map<String, String> ecologicalDetails, String description) {
        this(id, scientificName, commonName, family, telaBotanicaUrl, date, identifications,
                soilStructureScore, waterRetentionScore, nitrogenFixationScore,
                ecologicalDetails, description, ""); // Image vide par défaut
    }

    public Plant() {
        this.identifications = new ArrayList<>();
        this.identificationCount = 0;
        this.image = "";
    }

    @Override
    public ListItemType getType() {
        return ListItemType.PLANT;
    }

    // === GETTERS EXISTANTS ===
    public long getId() {
        return id;
    }

    public String getScientificName() {
        return scientificName;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getFamily() {
        return family;
    }

    public String getTelaBotanicaUrl() {
        return telaBotanicaUrl;
    }

    public String getDate() {
        return date;
    }

    public ArrayList<Identification> getIdentifications() {
        return identifications;
    }

    public int getSoilStructureScore() {
        return soilStructureScore;
    }

    public int getWaterRetentionScore() {
        return waterRetentionScore;
    }

    public int getNitrogenFixationScore() {
        return nitrogenFixationScore;
    }

    public Map<String, String> getEcologicalDetails() {
        return ecologicalDetails;
    }

    public String getDescription() {
        return description;
    }

    // === NOUVEAUX GETTERS ===
    public String getLatestScanDate() {
        return latestScanDate;
    }

    public int getIdentificationCount() {
        return identificationCount;
    }

    public String getImage() {
        return image;
    }

    // Méthodes d'aide pour vérifier si les scores sont à afficher
    public boolean hasSoilStructureScore() {
        return soilStructureScore > 0;
    }

    public boolean hasWaterRetentionScore() {
        return waterRetentionScore > 0;
    }

    public boolean hasNitrogenFixationScore() {
        return nitrogenFixationScore > 0;
    }

    // === SETTERS EXISTANTS ===
    public void setId(long id) {
        this.id = id;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public void setTelaBotanicaUrl(String telaBotanicaUrl) {
        this.telaBotanicaUrl = telaBotanicaUrl;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void addIdentification(Identification identification) {
        if (this.identifications == null) {
            this.identifications = new ArrayList<>();
        }
        this.identifications.add(identification);
    }

    public void setSoilStructureScore(int soilStructureScore) {
        this.soilStructureScore = soilStructureScore;
    }

    public void setWaterRetentionScore(int waterRetentionScore) {
        this.waterRetentionScore = waterRetentionScore;
    }

    public void setNitrogenFixationScore(int nitrogenFixationScore) {
        this.nitrogenFixationScore = nitrogenFixationScore;
    }

    public void setEcologicalDetails(Map<String, String> ecologicalDetails) {
        this.ecologicalDetails = ecologicalDetails;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // === NOUVEAUX SETTERS ===
    public void setLatestScanDate(String latestScanDate) {
        this.latestScanDate = latestScanDate;
    }

    public void setIdentificationCount(int identificationCount) {
        this.identificationCount = identificationCount;
    }

    public void setImage(String image) {
        this.image = image;
    }

    // === MÉTHODE JsonToPlant COMPLÈTEMENT MISE À JOUR ===
    public void JsonToPlant(JSONObject plant) {
        try {
            // Champs de base
            long id = plant.optLong("id", 0);
            String scientificName = plant.optString("scientific_name", "Inconnu");
            String commonName = plant.optString("common_name", "Aucun nom commun");
            String family = plant.optString("family", "Famille inconnue");
            String telaBotanicaUrl = plant.optString("tela_botanica_url", "");
            String date = plant.optString("scan_date", "Date inconnue");
            String latestScanDate = plant.optString("latest_scan_date", date);
            String description = plant.optString("description", "");

            // NOUVEAU : Image de la plante
            String image = plant.optString("image", "");

            // Scores (déjà en pourcentage 0-100 depuis le backend)
            int soil_structure_score = plant.optInt("soil_structure_score", 0);
            int water_retention_score = plant.optInt("water_retention_score", 0);
            int nitrogen_fixation_score = plant.optInt("nitrogen_fixation_score", 0);

            // Nouveau : nombre d'identifications depuis le backend
            int identificationCount = plant.optInt("identification_count", 1);

            // Détails écologiques
            Map<String, String> ecologicalDetails = new HashMap<>();
            JSONObject ecologicalJson = plant.optJSONObject("ecological_details");
            if (ecologicalJson != null) {
                ecologicalDetails.put("habitat", ecologicalJson.optString("habitat", "Non spécifié"));
                ecologicalDetails.put("growthRate", ecologicalJson.optString("growth_rate", "Non spécifié"));
                ecologicalDetails.put("lifespan", ecologicalJson.optString("lifespan", "Non spécifié"));
            } else {
                // Valeurs par défaut
                ecologicalDetails.put("habitat", "Non spécifié");
                ecologicalDetails.put("growthRate", "Non spécifié");
                ecologicalDetails.put("lifespan", "Non spécifié");
            }

            // Affecter toutes les valeurs
            this.setId(id);
            this.setScientificName(scientificName);
            this.setCommonName(commonName);
            this.setFamily(family);
            this.setTelaBotanicaUrl(telaBotanicaUrl);
            this.setDate(date);
            this.setLatestScanDate(latestScanDate);
            this.setSoilStructureScore(soil_structure_score);
            this.setWaterRetentionScore(water_retention_score);
            this.setNitrogenFixationScore(nitrogen_fixation_score);
            this.setDescription(description);
            this.setEcologicalDetails(ecologicalDetails);
            this.setIdentificationCount(identificationCount);
            this.setImage(image); // NOUVEAU

            Log.d("Plant", "Données de base parsées - ID: " + id + ", Nom: " + scientificName);
            Log.d("Plant", "Image présente: " + (!image.isEmpty()) + " (longueur: " + image.length() + ")");

            // NOUVEAU : Parser les identifications avec vraies coordonnées
            JSONArray identificationsArray = plant.optJSONArray("identifications");
            if (identificationsArray != null) {
                Log.d("Plant", "Parsing " + identificationsArray.length() + " identifications");

                for (int i = 0; i < identificationsArray.length(); i++) {
                    JSONObject identificationJSON = identificationsArray.getJSONObject(i);
                    Identification identification = new Identification();

                    // Parser les données essentielles pour les coordonnées
                    long identificationId = identificationJSON.optLong("id", 0);
                    double latitude = identificationJSON.optDouble("latitude", 0.0);
                    double longitude = identificationJSON.optDouble("longitude", 0.0);

                    identification.setId(identificationId);
                    identification.setLatitude(latitude);
                    identification.setLongitude(longitude);

                    // Parser la date avec format ISO
                    String dateString = identificationJSON.optString("date", "");
                    Date parsedDate = parseDate(dateString);
                    identification.setDate(parsedDate);

                    // Ajouter d'autres champs si disponibles
                    identification.setPlantId(id);
                    identification.setUserId(identificationJSON.optLong("userId", 0));
                    identification.setGroupId(identificationJSON.optLong("groupId", 0));

                    this.addIdentification(identification);

                    Log.d("Plant", "Identification " + (i + 1) + " ajoutée: " +
                            "ID=" + identificationId +
                            ", lat=" + latitude +
                            ", lon=" + longitude +
                            ", date=" + parsedDate);
                }
            } else {
                Log.d("Plant", "Aucune identification trouvée dans le JSON");
            }

            // Identifications individuelles legacy (pour compatibilité avec l'ancien format)
            if (this.identifications.isEmpty()) {
                JSONArray legacyIdentificationsArray = plant.optJSONArray("legacy_identifications");
                if (legacyIdentificationsArray != null) {
                    for (int i = 0; i < legacyIdentificationsArray.length(); i++) {
                        JSONObject identificationJSON = legacyIdentificationsArray.getJSONObject(i);
                        Identification identification = new Identification();
                        identification.JsonToIdentification(identificationJSON);
                        this.addIdentification(identification);
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("Plant", "Erreur parsing JSON: " + e.toString());
        }
    }

    // === MÉTHODE UTILITAIRE : Parser la date avec plusieurs formats ===
    private Date parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return new Date(); // Date actuelle par défaut
        }

        // Formats de date possibles (adapte selon ton backend)
        String[] dateFormats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", // ISO avec Z
                "yyyy-MM-dd'T'HH:mm:ss.SSS",    // ISO avec millisecondes
                "yyyy-MM-dd'T'HH:mm:ss'Z'",     // ISO avec Z sans millisecondes
                "yyyy-MM-dd'T'HH:mm:ss",        // ISO format standard
                "yyyy-MM-dd HH:mm:ss",          // Format SQL
                "yyyy-MM-dd",                   // Date seulement
                "dd/MM/yyyy",                   // Format français
                "MM/dd/yyyy"                    // Format américain
        };

        for (String format : dateFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                return sdf.parse(dateString);
            } catch (Exception e) {
                // Continuer avec le format suivant
            }
        }

        // Si aucun format ne fonctionne, essayer le parsing par défaut
        try {
            SimpleDateFormat sdf = new SimpleDateFormat();
            return sdf.parse(dateString);
        } catch (Exception e) {
            Log.w("Plant", "Could not parse date: " + dateString + ", using current date");
            return new Date(); // Date actuelle par défaut
        }
    }

    // === MÉTHODE UTILITAIRE : Information de debug ===
    public String getDebugInfo() {
        return "Plant{" +
                "id=" + id +
                ", scientificName='" + scientificName + '\'' +
                ", commonName='" + commonName + '\'' +
                ", identificationCount=" + identificationCount +
                ", realIdentifications=" + (identifications != null ? identifications.size() : 0) +
                ", hasImage=" + (image != null && !image.isEmpty()) +
                ", scores=[soil:" + soilStructureScore + ", water:" + waterRetentionScore + ", nitrogen:" + nitrogenFixationScore + "]" +
                '}';
    }
}