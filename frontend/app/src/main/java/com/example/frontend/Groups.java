package com.example.frontend;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.adapter.GroupAdapter;
import com.example.frontend.model.Group;
import com.example.frontend.utils.Constants;
import com.example.frontend.utils.TokenManager;
import com.example.frontend.utils.UIUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class Groups extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GroupAdapter groupAdapter;
    private List<Group> groupList;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private ProgressDialog progressDialog;

    TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);
        UIUtils.hideSystemUI(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Chargement...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        recyclerView = findViewById(R.id.message_list_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tokenManager = new TokenManager(this);
        tokenManager.CheckConnexion();
        // Création et affectation de l'adaptateur
        String url = Constants.API_BASE_URL + "/group/";
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + tokenManager.getAccessToken())
                .build();
        groupList = new ArrayList<>();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(Groups.this, "Erreur réseau : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("Groups", e.getMessage());
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                });
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    response.body().close();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray groups = jsonResponse.getJSONArray("groups");
                        for(int i = 0; i< groups.length(); i++)
                        {
                            JSONObject groupJSON = groups.getJSONObject(i);
                            Group group = new Group();
                            group.JsonToGroup(groupJSON);
                            groupList.add(group);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            new AlertDialog.Builder(Groups.this)
                                    .setTitle("Erreur de traitement")
                                    .setMessage("La réponse du serveur est invalide.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
                    } finally {
                        runOnUiThread(() -> {
                            groupAdapter = new GroupAdapter(groupList);
                            recyclerView.setAdapter(groupAdapter);
                        });
                    }

                } else {
                    String errorMessage = "Erreur inconnue";
                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        if (json.has("message")) {
                            errorMessage = json.getString("message");
                        }
                    } catch (Exception e) {
                        errorMessage = "Erreur lors de la lecture du message d'erreur";
                    }
                    String finalErrorMessage = errorMessage;
                    runOnUiThread(() -> {
                        Toast.makeText(Groups.this, finalErrorMessage, Toast.LENGTH_LONG).show();
                        Log.d("Groups", finalErrorMessage);
                    });
                }
            }
        });
    }

    public void filterGroups(String query) {
        List<Group> filteredList = new ArrayList<>();
        for (Group group : groupList) {
            if (group.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(group);
            }
        }
        groupAdapter.updateList(filteredList); // méthode qu'on ajoute ci-dessous
    }
}
