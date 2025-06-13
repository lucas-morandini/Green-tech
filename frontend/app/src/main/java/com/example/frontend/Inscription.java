package com.example.frontend;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend.utils.Constants;
import com.example.frontend.utils.UIUtils;
import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Inscription extends AppCompatActivity {

    String role = "lambda";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inscription);
        Spinner spinner = findViewById(R.id.profession);
        List<String> professions = Arrays.asList("researcher", "student", "lambda", "lab_technician", "engineer", "Phd student", "intern");
        // ProgressDialog configuration
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Chargement...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                professions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                role = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        Button registerButton = findViewById(R.id.register_button);
        registerButton.setOnClickListener(view -> {
            TextInputEditText nomText = findViewById(R.id.nom);
            TextInputEditText prenomText = findViewById(R.id.prenom);
            TextInputEditText emailText = findViewById(R.id.email);
            TextInputEditText passwordText = findViewById(R.id.password);
            TextInputEditText locationText = findViewById(R.id.ville_labo);

            String nom = nomText.getText().toString().trim();
            String prenom = prenomText.getText().toString().trim();
            String email = emailText.getText().toString().trim();
            String password = passwordText.getText().toString().trim();
            String location = locationText.getText().toString().trim();

            boolean isValid = true;

            if (nom.isEmpty()) {
                nomText.setError("Champ requis");
                isValid = false;
            }

            if (prenom.isEmpty()) {
                prenomText.setError("Champ requis");
                isValid = false;
            }

            if (email.isEmpty()) {
                emailText.setError("Champ requis");
                isValid = false;
            }

            if (password.isEmpty()) {
                passwordText.setError("Champ requis");
                isValid = false;
            }

            if (location.isEmpty()) {
                locationText.setError("Champ requis");
                isValid = false;
            }

            if (isValid) {
                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("name", nom);
                    jsonBody.put("first_name", prenom);
                    jsonBody.put("email", email);
                    jsonBody.put("password", password);
                    jsonBody.put("role", role);
                    jsonBody.put("location", location);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                String url = Constants.API_BASE_URL + "/auth/register";
                RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url)
                        .post(body)
                        .build();
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(Inscription.this, "Erreur réseau : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            if (response.isSuccessful()) {
                                Intent intent = new Intent(Inscription.this, Connexion.class);
                                startActivity(intent);
                                finish();
                            } else {
                                String errorMessage = "Erreur inconnue";
                                try {
                                    String responseBody = response.body().string();
                                    response.body().close();
                                    JSONObject json = new JSONObject(responseBody);
                                    if (json.has("message")) {
                                        errorMessage = json.getString("message");
                                    }
                                } catch (Exception e) {
                                    errorMessage = "Erreur lors de la lecture du message d'erreur";
                                }

                                Toast.makeText(Inscription.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }else{
                Toast.makeText(this, "Veuillez remplir tous les champs...", Toast.LENGTH_SHORT).show();
            }
        });

        Button login_button = findViewById(R.id.login_button);
        login_button.setOnClickListener(v ->{
            this.GoToConnexion();
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        UIUtils.hideSystemUI(this);
    }


    public void GoToConnexion()
    {
        Intent intent = new Intent(this, Connexion.class);
        startActivity(intent);
        finish();
    }
}