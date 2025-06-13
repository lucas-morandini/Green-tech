package com.example.frontend;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend.utils.Constants;
import com.example.frontend.utils.TokenManager;
import com.example.frontend.utils.UIUtils;
import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Connexion extends AppCompatActivity {
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
        setContentView(R.layout.activity_connexion);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Chargement...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        Button connexion_button = findViewById(R.id.connexion_button);
        connexion_button.setOnClickListener(view -> {
            TextInputEditText emailText = findViewById(R.id.email);
            TextInputEditText passwordText = findViewById(R.id.password);
            String email = emailText.getText().toString().trim();
            String password = passwordText.getText().toString().trim();

            boolean isValid = true;

            if (email.isEmpty()) {
                emailText.setError("Champ requis");
                isValid = false;
            }

            if (password.isEmpty()) {
                passwordText.setError("Champ requis");
                isValid = false;
            }

            if (isValid) {
                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("email", email);
                    jsonBody.put("password", password);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                String url = Constants.API_BASE_URL + "/auth/login";
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
                            Toast.makeText(Connexion.this, "Erreur réseau : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            if (response.isSuccessful()) {
                                try {
                                    String responseBody = response.body().string();
                                    JSONObject jsonObject = new JSONObject(responseBody);
                                    String accessToken = jsonObject.getString("access_token");
                                    long userId = jsonObject.getLong("userId");
                                    TokenManager tokenManager = new TokenManager(Connexion.this);

                                    tokenManager.saveAccessToken(accessToken);
                                    tokenManager.saveUserId(userId);
                                    Intent intent = new Intent(Connexion.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();

                                } catch (IOException | JSONException e) {
                                    e.printStackTrace();
                                    Toast.makeText(Connexion.this, "Impossible de se connecter, veuillez relancer l'application", Toast.LENGTH_LONG).show();
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

                                Toast.makeText(Connexion.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }else{
                Toast.makeText(this, "Veuillez remplir tous les champs...", Toast.LENGTH_SHORT).show();
            }
        });
        Button register = findViewById(R.id.create_account);
        register.setOnClickListener(v ->{
            this.GoToInscription();
        });
        TextView forgot_password = findViewById(R.id.password_forgot);
        forgot_password.setOnClickListener(v -> {
            TextInputEditText emailText = findViewById(R.id.email);
            String email = emailText.getText().toString().trim();
            this.ForgotPassword(email, emailText);
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        UIUtils.hideSystemUI(this);
    }

    private void GoToInscription()
    {
        Intent intent = new Intent(this, Inscription.class);
        startActivity(intent);
        finish();
    }

    private void ForgotPassword(String email, TextInputEditText emailText)
    {
        if (email.isEmpty()) {
            emailText.setError("Champ requis");
            return;
        }
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        String url = Constants.API_BASE_URL + "/auth/forgot-password";
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
                    Toast.makeText(Connexion.this, "Erreur réseau : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        Toast.makeText(Connexion.this, "Un email vous a été envoyé", Toast.LENGTH_LONG).show();
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

                        Toast.makeText(Connexion.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}