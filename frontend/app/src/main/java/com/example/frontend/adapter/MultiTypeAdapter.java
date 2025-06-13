package com.example.frontend.adapter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.Connexion;
import com.example.frontend.Inscription;
import com.example.frontend.R;
import com.example.frontend.model.User;
import com.example.frontend.model.Plant;
import com.example.frontend.utils.Constants;
import com.example.frontend.utils.TokenManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import android.os.Handler;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MultiTypeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_PLANT = 2;

    private List<ListItem> items;

    public MultiTypeAdapter(List<ListItem> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        ListItem item = items.get(position);
        if (item.getType() == ListItemType.USER) {
            return TYPE_USER;
        } else if (item.getType() == ListItemType.PLANT) {
            return TYPE_PLANT;
        }
        return super.getItemViewType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        } else if (viewType == TYPE_PLANT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant, parent, false);
            return new PlantViewHolder(view);
        }
        throw new IllegalArgumentException("Invalid view type");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ListItem item = items.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind((User) item);
        } else if (holder instanceof PlantViewHolder) {
            ((PlantViewHolder) holder).bind((Plant) item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView full_nameTextView;
        private TextView role_locationTextView;
        private TextView identificationCountTextView;

        private ImageButton addUser;

        private final OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        private ProgressDialog progressDialog;

        private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

        private TokenManager tokenManager;
        public UserViewHolder(View itemView) {
            super(itemView);
            full_nameTextView = itemView.findViewById(R.id.user_full_name);
            role_locationTextView = itemView.findViewById(R.id.user_role_location);
            identificationCountTextView = itemView.findViewById(R.id.user_identification);
            addUser = itemView.findViewById(R.id.contact_button);
            progressDialog = new ProgressDialog(itemView.getContext());
            progressDialog.setMessage("Chargement...");
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            tokenManager = new TokenManager(itemView.getContext());
        }

        public void bind(User user) {
            String full_name = user.getName() + " " + user.getFirst_name();
            String role_location = user.getRole() + " à " + user.getLocation();
            full_nameTextView.setText(full_name);
            role_locationTextView.setText(role_location);
            String identification = user.getIdentifications().size() + " identifications";
            identificationCountTextView.setText(identification);
            boolean is_friend = user.getIsFriend();
            if (is_friend) {
                addUser.setVisibility(View.GONE);
                addUser.setOnClickListener(null);
            } else {
                addUser.setOnClickListener(v -> {
                    JSONObject jsonBody = new JSONObject();
                    try {
                        jsonBody.put("friend_id", user.getId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return;
                    }
                    String url = Constants.API_BASE_URL + "/user/friends/add";
                    RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(url)
                            .post(body)
                            .addHeader("Authorization", "Bearer " + tokenManager.getAccessToken())
                            .build();
                    httpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(itemView.getContext(), "Erreur réseau : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                progressDialog.dismiss();
                                if (response.isSuccessful()) {
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(itemView.getContext(), "Action réalisé avec succès", Toast.LENGTH_LONG).show();
                                    });
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

                                    Toast.makeText(itemView.getContext(), errorMessage, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                });
            }
        }
    }

    public static class PlantViewHolder extends RecyclerView.ViewHolder {
        private TextView scientificNameTextView;
        private TextView commonNameTextView;

        private TextView identificationCountTextView;
        public PlantViewHolder(View itemView) {
            super(itemView);
            scientificNameTextView = itemView.findViewById(R.id.plant_name);
            commonNameTextView = itemView.findViewById(R.id.plant_type);
            identificationCountTextView = itemView.findViewById(R.id.identification_count);
        }

        public void bind(Plant plant) {
            scientificNameTextView.setText(plant.getScientificName());
            commonNameTextView.setText(plant.getCommonName());
            String identification = plant.getIdentifications().size() + " identifications";
            identificationCountTextView.setText(identification);
        }
    }
}
