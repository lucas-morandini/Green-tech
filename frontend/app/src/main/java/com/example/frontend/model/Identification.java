package com.example.frontend.model;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Identification {
    private long id;

    private Date date;

    private double latitude;

    private double longitude;

    private long plantId;

    private long userId;

    private long groupId;

    public Identification(long id, Date date, double latitude, double longitude, long plantId, long userId, long groupId) {
        this.id = id;
        this.date = date;
        this.latitude = latitude;
        this.longitude = longitude;
        this.plantId = plantId;
        this.userId = userId;
        this.groupId = groupId;
    }

    public Identification()
    {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getPlantId() {
        return plantId;
    }

    public void setPlantId(long plantId) {
        this.plantId = plantId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public void JsonToIdentification(JSONObject identification) {
        // ID
        long id = identification.optLong("id", 0);
        this.setId(id);

        // Date avec parsing plus robuste
        String date_identification = identification.optString("date", "");
        Date date = parseDate(date_identification);
        this.setDate(date);

        // Coordonnées avec valeurs par défaut
        double latitude = identification.optDouble("latitude", 0.0);
        double longitude = identification.optDouble("longitude", 0.0);
        this.setLatitude(latitude);
        this.setLongitude(longitude);

        // IDs avec valeurs par défaut
        long plantId = identification.optLong("plantId", 0);
        long groupId = identification.optLong("groupId", 0);
        long userId = identification.optLong("userId", 0);

        this.setPlantId(plantId);
        this.setUserId(userId);
        this.setGroupId(groupId);

        Log.d("Identification", "Parsed identification ID=" + id +
                ", lat=" + latitude + ", lon=" + longitude);

    }

    private Date parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return new Date(); // Date actuelle par défaut
        }

        // Formats de date possibles (adapte selon ton backend)
        String[] dateFormats = {
                "yyyy-MM-dd'T'HH:mm:ss",       // ISO format
                "yyyy-MM-dd'T'HH:mm:ss.SSS",   // ISO avec millisecondes
                "yyyy-MM-dd HH:mm:ss",         // Format SQL
                "yyyy-MM-dd",                  // Date seulement
                "dd/MM/yyyy",                  // Format français
                "MM/dd/yyyy"                   // Format américain
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
            Log.w("Identification", "Could not parse date: " + dateString + ", using current date");
            return new Date(); // Date actuelle par défaut
        }
    }
}
