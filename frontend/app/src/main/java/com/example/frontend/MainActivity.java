package com.example.frontend;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend.utils.Constants;
import com.example.frontend.utils.Scan;
import com.example.frontend.utils.UIUtils;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.Manifest;
import android.location.Location;
import android.location.LocationManager;

import com.example.frontend.utils.TokenManager;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    private static final String API_URL = Constants.API_BASE_URL+"/plant/identify";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private TokenManager tokenManager;
    private ProgressDialog progressDialog;

    // ActivityResultLauncher pour la sélection d'image depuis la galerie
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        FirebaseMessaging.getInstance().subscribeToTopic("general")
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("FCM", "Abonné au topic général !");
                } else {
                    Log.e("FCM", "Échec de l'abonnement", task.getException());
                }
            });

        FirebaseMessaging.getInstance().getToken()
        .addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("FCM", "Échec de récupération du token", task.getException());
                return;
            }
            String token = task.getResult();
            Log.d("FCM", "🔑 Token récupéré : " + token);
            OkHttpClient client = new OkHttpClient();

            String url = Constants.API_BASE_URL + "/user/fcm-token";

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            JSONObject json = new JSONObject();
            try {
                json.put("fcmToken", token);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RequestBody body = RequestBody.create(json.toString(), JSON);
            TokenManager tokenManager = new TokenManager(this);
            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Authorization", "Bearer " + tokenManager.getAccessToken())
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("FCM", "❌ Erreur envoi token : " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d("FCM", "✅ Token FCM envoyé au backend !");
                    } else {
                        Log.e("FCM", "❌ Échec avec code : " + response.code());
                    }
                }
            });
        });

        tokenManager = new TokenManager(MainActivity.this);
        String token = tokenManager.getAccessToken();

        if (token != null) {
            Log.d("TOKEN", "Token récupéré : " + token);
        } else {
            Log.d("TOKEN", "Aucun token trouvé.");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        UIUtils.hideSystemUI(this);

        // ProgressDialog configuration
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Chargement...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialisation du launcher pour la galerie
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String base64Image = convertUriToBase64(uri);
                        if (base64Image != null) {
                            sendImageToAPI(base64Image);
                        } else {
                            Toast.makeText(this, "Erreur lors du traitement de l'image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }

        checkLocationPermission();

        // Configuration du bouton de capture photo
        ImageButton capture_picture = findViewById(R.id.capture_button);
        capture_picture.setOnClickListener(v -> takePhoto());

        // Configuration du bouton galerie
        ImageButton gallery_button = findViewById(R.id.gallery_button);
        gallery_button.setOnClickListener(v -> openGallery());
    }

    private void openGallery() {
        imagePickerLauncher.launch("image/*");
    }

    private String convertUriToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            inputStream.close();

            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (IOException e) {
            Log.e("Gallery", "Erreur lors de la conversion en base64: " + e.getMessage());
            return null;
        }
    }

    private void startCamera() {
        PreviewView previewView = findViewById(R.id.previewView);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(getOutputDirectory(),
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                        .format(System.currentTimeMillis()) + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NotNull ImageCapture.OutputFileResults output) {
                Log.d("CameraX", "Photo saved: " + photoFile.getAbsolutePath());
                String base64Image = UIUtils.encodeFileToBase64(photoFile);
                Log.d("CameraX", "Base64: " + base64Image);
                sendImageToAPI(base64Image);
            }

            @Override
            public void onError(@NotNull ImageCaptureException exc) {
                Log.e("CameraX", "Photo capture failed: " + exc.getMessage(), exc);
            }
        });
    }

    private void sendImageToAPI(String base64Image) {
        if (base64Image == null) {
            Log.e("API", "Base64 image is null, aborting API call.");
            return;
        }

        runOnUiThread(() -> progressDialog.show());

        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray imagesArray = new JSONArray();
            imagesArray.put(base64Image);
            JSONObject geoPoint = getLocationAsGeoPoint();
            if (geoPoint != null) {
                jsonBody.put("location", geoPoint);
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = sdf.format(new Date());
            jsonBody.put("scan_date", currentDate);
            jsonBody.put("images", imagesArray);
        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> progressDialog.dismiss());
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .header("Authorization", "Bearer " + tokenManager.getAccessToken())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Erreur réseau : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                runOnUiThread(() -> progressDialog.dismiss());

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        String title = (response.code() == 404) ? "Espèce non trouvée" : "Erreur";
                        String message = (response.code() == 404)
                                ? "Nous n'avons pas pu identifier cette plante."
                                : "Une erreur est survenue. Veuillez réessayer plus tard.";
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(title)
                                .setMessage(message)
                                .setPositiveButton("OK", null)
                                .show();
                    });
                }

                String responseBody = response.body().string();
                response.body().close();
                Log.d("API", "API call successful: " + responseBody);

                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray scans = jsonResponse.getJSONArray("scans");
                    JSONArray plantsArray = jsonResponse.getJSONArray("plants");
                    if (plantsArray.length() > 0) {
                        JSONObject plant = plantsArray.getJSONObject(0);
                        String scName = plant.optString("scientific_name", "Inconnu");
                        String commonName = plant.optString("common_name", "Aucun nom commun");
                        String description = plant.optString("description", "Pas de description disponible");
                        String image = plant.optString("image", null);
                        String ecologicalDetails = plant.optString("ecological_details","Pas de détails écologiques");

                        // Données écologiques
                        float nitrogenScore = (float) plant.optDouble("nitrogen_fixation_score", 0.0);
                        float soilScore = (float) plant.optDouble("soil_structure_score", 0.0);
                        float waterScore = (float) plant.optDouble("water_retention_score", 0.0);

                        // Convertir les scores en pourcentage
                        int nitrogenPercentage = (int) (nitrogenScore * 100);
                        int soilPercentage = (int) (soilScore * 100);
                        int waterPercentage = (int) (waterScore * 100);

                        Intent intent = new Intent(MainActivity.this, DetailsPlant.class);
                        intent.putExtra("sc_name", scName);
                        intent.putExtra("common_name", commonName);
                        intent.putExtra("description", description);
                        if(!plant.isNull("image")) intent.putExtra("image", image);
                        intent.putExtra("nitrogen_score", nitrogenPercentage);
                        intent.putExtra("soil_score", soilPercentage);
                        intent.putExtra("water_score", waterPercentage);
                        intent.putExtra("ecological_details", ecologicalDetails);

                        ArrayList<Scan> myScanList = new ArrayList<>();
                        Log.d("Scans API", scans.toString());
                        for(int i = 0; i<scans.length(); i++)
                        {
                            JSONObject location = scans.getJSONObject(i).optJSONObject("location");

                            double latitude = location.optDouble("latitude");
                            double longitude = location.optDouble("longitude");
                            String scanDate = scans.getJSONObject(i).getString("scan_date");
                            SimpleDateFormat dateFormat = new SimpleDateFormat("d/M/yyyy");
                            Date date = null;
                            try {
                                date = dateFormat.parse(scanDate);
                            } catch (ParseException e) {
                                date = new Date();
                                e.printStackTrace();
                            }
                            Scan myScan = new Scan(new GeoPoint(latitude, longitude), date);
                            myScanList.add(myScan);
                        }
                        intent.putParcelableArrayListExtra("scans", myScanList);
                        runOnUiThread(() -> startActivity(intent));
                    } else {
                        runOnUiThread(() -> {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Aucune plante identifiée")
                                    .setMessage("Aucun résultat n'a été retourné.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Erreur de traitement")
                                .setMessage("La réponse du serveur est invalide.")
                                .setPositiveButton("OK", null)
                                .show();
                    });
                }
            }
        });
    }

    private File getOutputDirectory() {
        File mediaDir = getExternalFilesDir(null);
        return mediaDir != null ? mediaDir : getFilesDir();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private JSONObject getLocationAsGeoPoint() {
        JSONObject geoPoint = new JSONObject();
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
            return null;
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            try {
                geoPoint.put("latitude", location.getLatitude());
                geoPoint.put("longitude", location.getLongitude());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return geoPoint;
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }
}