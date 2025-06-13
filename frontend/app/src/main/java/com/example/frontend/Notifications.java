package com.example.frontend;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.adapter.NotificationAdapter;
import com.example.frontend.model.Notification;
import com.example.frontend.utils.Constants;
import com.example.frontend.utils.TokenManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Notifications extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        TokenManager tokenManager = new TokenManager(this);
        token = tokenManager.getAccessToken();
        recyclerView = findViewById(R.id.recyclerView);
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchNotifications();
    }

    private void fetchNotifications() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Constants.API_BASE_URL + "/notification/")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(Notifications.this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONArray notificationsJson = new JSONArray(responseBody);
                        ArrayList<Notification> notifications = new ArrayList<>();
                        for(int i = 0; i<notificationsJson.length(); i++)
                        {
                            Notification notification = new Notification();
                            notification.JsonToNotification(notificationsJson.getJSONObject(i));
                            notifications.add(notification);
                        }
                        runOnUiThread(() -> {
                            notificationList.clear();
                            notificationList.addAll(notifications);
                            adapter.notifyDataSetChanged();
                            setReadNotification();
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d("API CALL", e.toString());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(Notifications.this, "Erreur serveur", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void setReadNotification()
    {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Constants.API_BASE_URL + "/notification/read")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(Notifications.this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("API CALL", "Notification read");
                } else {
                    runOnUiThread(() -> Toast.makeText(Notifications.this, "Erreur serveur", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}