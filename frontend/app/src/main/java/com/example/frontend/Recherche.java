package com.example.frontend;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.adapter.ListItem;
import com.example.frontend.adapter.MultiTypeAdapter;
import com.example.frontend.component.SearchBar;
import com.example.frontend.model.Identification;
import com.example.frontend.model.Plant;
import com.example.frontend.model.User;
import com.example.frontend.utils.Constants;
import com.example.frontend.utils.TokenManager;
import com.example.frontend.utils.UIUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Recherche extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MultiTypeAdapter adapter;
    private List<ListItem> items;
    private boolean userFetched = false;
    private boolean plantFetched = false;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private ProgressDialog progressDialog;

    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recherche);
        tokenManager = new TokenManager(this);
        tokenManager.CheckConnexion();
        SearchBar searchBar = findViewById(R.id.searchbar);
        EditText searchInput = searchBar.findViewById(R.id.search_input);
        searchInput.setText(getIntent().getStringExtra("recherche_text"));
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Chargement...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);

        recyclerView = findViewById(R.id.research_list_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        items = new ArrayList<>();
        progressDialog.show();
        // Lancer les appels réseau pour récupérer les utilisateurs et les plantes
        fetchUsers();
        fetchPlants();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        UIUtils.hideSystemUI(this);
    }

    private void fetchUsers() {
        Log.d("API CALL", "Trying to fetch user");
        String rechercheText = getIntent().getStringExtra("recherche_text");
        String url = Constants.API_BASE_URL + "/user/search?search=" + rechercheText;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + tokenManager.getAccessToken())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(Recherche.this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
                    Log.d("API CALL", e.toString());
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray usersArray = new JSONArray(response.body().string());
                        response.body().close();
                        Log.d("API CALL", usersArray.toString());
                        for (int i = 0; i < usersArray.length(); i++) {
                            JSONObject userJson = usersArray.getJSONObject(i);
                            User user = new User();
                            user.JsonToUser(userJson);
                            items.add(user);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("API CALL", e.toString());
                        runOnUiThread(() -> {
                            Toast.makeText(Recherche.this, "Erreur serveur : " + response.code(), Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        });
                    }
                    finally {
                        userFetched = true;
                        updateRecyclerView();
                    }
                }else{
                    runOnUiThread(() -> {
                        Log.d("API CALL", response.message());
                        Toast.makeText(Recherche.this, "Erreur serveur : " + response.code(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    });
                }

            }
        });
    }

    private void fetchPlants() {
        Log.d("API CALL", "Trying to fetch plant");

        String rechercheText = getIntent().getStringExtra("recherche_text");
        String url = Constants.API_BASE_URL + "/plant/search?search=" + rechercheText;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + tokenManager.getAccessToken())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(Recherche.this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
                    Log.d("API CALL", e.toString());
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray plantsArray = new JSONArray(response.body().string());
                        response.body().close();
                        Log.d("API CALL", plantsArray.toString());
                        for (int i = 0; i < plantsArray.length(); i++) {
                            JSONObject plantJson = plantsArray.getJSONObject(i);
                            Plant plant = new Plant();
                            plant.JsonToPlant(plantJson);
                            items.add(plant);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("API CALL", e.toString());
                        runOnUiThread(() -> {
                            Toast.makeText(Recherche.this, "Erreur serveur : " + response.code(), Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        });
                    }finally {
                        plantFetched = true;
                        updateRecyclerView();
                    }
                }else{
                    runOnUiThread(() -> {
                        Log.d("API CALL", response.message());
                        Toast.makeText(Recherche.this, "Erreur serveur : " + response.code(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    });
                }
            }
        });
    }

    private void updateRecyclerView() {
        Log.d("RECYCLER", "userFetched = " + userFetched + ", plantFetched = " + plantFetched);
        if (userFetched && plantFetched) {
            Log.d("RECYCLER", "Updating RecyclerView");
            runOnUiThread(() -> {
                adapter = new MultiTypeAdapter(items);
                recyclerView.setAdapter(adapter);
                progressDialog.dismiss();
            });
        }
    }
}
