package com.example.frontend.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.model.Plant;

import java.util.List;

public class PlantAdapter extends RecyclerView.Adapter<PlantAdapter.PlantViewHolder> {

    private List<Plant> plants;
    private Context context;
    private OnItemClickListener listener;

    public PlantAdapter(List<Plant> plants) {
        this.plants = plants;
    }

    public static class PlantViewHolder extends RecyclerView.ViewHolder {
        public ImageView plantIcon;
        public TextView plantName;       // Affiche le nom scientifique
        public TextView plantType;       // Affiche le nom commun
        public TextView plantDate;
        public TextView identificationCount;
        public ImageView soilStructureIndicator;
        public ImageView waterRetentionIndicator;
        public ImageView nitrogenFixationIndicator;

        public PlantViewHolder(View itemView) {
            super(itemView);
            plantIcon = itemView.findViewById(R.id.plant_icon);
            plantName = itemView.findViewById(R.id.plant_name);
            plantType = itemView.findViewById(R.id.plant_type);
            plantDate = itemView.findViewById(R.id.plant_date);
            identificationCount = itemView.findViewById(R.id.identification_count);
            soilStructureIndicator = itemView.findViewById(R.id.soil_structure_indicator);
            waterRetentionIndicator = itemView.findViewById(R.id.water_retention_indicator);
            nitrogenFixationIndicator = itemView.findViewById(R.id.nitrogen_fixation_indicator);
        }
    }

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_plant, parent, false);
        return new PlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        Plant plant = plants.get(position);

        // Définir les données de la plante
        holder.plantName.setText(plant.getScientificName());
        holder.plantType.setText(plant.getCommonName());

        // Utiliser la date la plus récente si disponible, sinon la date originale
        String dateToShow = plant.getLatestScanDate() != null ?
                plant.getLatestScanDate() : plant.getDate();
        holder.plantDate.setText(dateToShow);

        // Utiliser le nombre d'identifications du backend si disponible
        int count = plant.getIdentificationCount() > 0 ?
                plant.getIdentificationCount() : plant.getIdentifications().size();
        holder.identificationCount.setText("identifiée " + count + " fois");

        // Configurer les indicateurs avec couleurs selon les scores
        setupIndicator(holder.soilStructureIndicator, plant.getSoilStructureScore(), "soil");
        setupIndicator(holder.waterRetentionIndicator, plant.getWaterRetentionScore(), "water");
        setupIndicator(holder.nitrogenFixationIndicator, plant.getNitrogenFixationScore(), "nitrogen");

        // Gérer le clic sur l'élément
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return plants.size();
    }

    /**
     * Configure l'apparence et la visibilité d'un indicateur selon le score
     * @param indicator L'ImageView de l'indicateur
     * @param score Le score (0-100)
     * @param type Le type d'indicateur ("soil", "water", "nitrogen")
     */
    private void setupIndicator(ImageView indicator, int score, String type) {
        // Toujours rendre l'indicateur visible
        indicator.setVisibility(View.VISIBLE);

        // Déterminer la couleur/niveau selon le score
        int backgroundRes;
        int alpha = 255; // Opacité complète par défaut

        if (score >= 70) {
            // Score élevé - Vert
            backgroundRes = getBackgroundForType(type, "high");
        } else if (score >= 40) {
            // Score moyen - Orange/Jaune
            backgroundRes = getBackgroundForType(type, "medium");
            alpha = 200; // Légèrement transparent
        } else if (score > 0) {
            // Score faible - Orange pâle
            backgroundRes = getBackgroundForType(type, "low");
            alpha = 150; // Plus transparent
        } else {
            // Pas de score - Gris
            backgroundRes = R.drawable.circle_indicator_gray;
            alpha = 100; // Très transparent
        }

        // Appliquer le background
        indicator.setBackgroundResource(backgroundRes);

        // Appliquer la transparence
        if (indicator.getBackground() != null) {
            indicator.getBackground().setAlpha(alpha);
        }

        // Définir l'icône selon le type
        int iconRes = getIconForType(type);
        indicator.setImageResource(iconRes);

        // Ajouter une description pour l'accessibilité
        String description = getDescriptionForType(type) + " score: " + score + "%";
        indicator.setContentDescription(description);
    }

    /**
     * Retourne le drawable de background selon le type et le niveau
     */
    private int getBackgroundForType(String type, String level) {
        switch (type) {
            case "soil":
                return level.equals("high") ? R.drawable.circle_indicator_green : R.drawable.circle_indicator_orange;
            case "water":
                return level.equals("high") ? R.drawable.circle_indicator_blue : R.drawable.circle_indicator_orange;
            case "nitrogen":
                return level.equals("high") ? R.drawable.circle_indicator_orange : R.drawable.circle_indicator_orange;
            default:
                return R.drawable.circle_indicator_gray;
        }
    }

    /**
     * Retourne l'icône selon le type d'indicateur
     */
    private int getIconForType(String type) {
        switch (type) {
            case "soil":
                return R.drawable.soil_structure_indicator;
            case "water":
                return R.drawable.water_retention_indicator;
            case "nitrogen":
                return R.drawable.nitrogen_fixation_indicator;
            default:
                return R.drawable.ic_plant; // Icône par défaut
        }
    }

    /**
     * Retourne la description pour l'accessibilité
     */
    private String getDescriptionForType(String type) {
        switch (type) {
            case "soil":
                return "Structure du sol";
            case "water":
                return "Rétention d'eau";
            case "nitrogen":
                return "Fixation d'azote";
            default:
                return "Indicateur";
        }
    }

    /**
     * Méthode pour définir le listener de clic
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Interface pour gérer les clics sur les éléments
     */
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
}