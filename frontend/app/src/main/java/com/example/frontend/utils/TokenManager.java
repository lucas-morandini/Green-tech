package com.example.frontend.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.example.frontend.Connexion;
import com.example.frontend.PlantsHistory;
import com.example.frontend.Recherche;
import com.example.frontend.model.User;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TokenManager {

    private static final String PREF_NAME = "APP_PREFS";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    private static final String KEY_USER_ID = "userId";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    Context context;

    public TokenManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        this.context = context;
    }

    public void saveAccessToken(String token) {
        editor.putString(KEY_ACCESS_TOKEN, token);
        editor.apply();
    }

    public void saveUserId(long userId)
    {
        editor.putLong(KEY_USER_ID, userId);
        editor.apply();
    }

    public long getUserId()
    {
        return sharedPreferences.getLong(KEY_USER_ID, 0);
    }

    public String getAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    // Vérifier si un token est stocké
    public boolean isTokenStored() {
        return sharedPreferences.contains(KEY_ACCESS_TOKEN);
    }

    public boolean isUserIdStored()
    {
        return sharedPreferences.contains(KEY_USER_ID);
    }

    public void removeAccessToken() {
        editor.remove(KEY_ACCESS_TOKEN);
        editor.apply();
    }

    public void removeUserId() {
        editor.remove(KEY_USER_ID);
        editor.apply();
    }

    public void clear() {
        editor.clear();
        editor.apply();
    }

    public void CheckConnexion() {
        String token = this.getAccessToken();
        String url = Constants.API_BASE_URL + "/auth/check_logged";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Erreur réseau", Toast.LENGTH_SHORT).show();
                        Log.d("API CALL", e.toString());
                    });
                }
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    response.body().close();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean is_logged = jsonResponse.getBoolean("is_logged");
                        Log.d("API CALL", String.valueOf(is_logged));
                        if (!is_logged) {
                            Intent intent = new Intent(context, Connexion.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivity(intent);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("API CALL", e.toString());
                    }
                }else{
                    int statusCode = response.code();
                    Log.d("API CALL", "Erreur HTTP : " + statusCode);
                    if (statusCode == 403) {
                        Intent intent = new Intent(context, Connexion.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(intent);
                    }
                }
            }
        });
    }
}
