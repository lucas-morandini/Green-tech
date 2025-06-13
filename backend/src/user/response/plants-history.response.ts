export interface EcologicalDetails {
  habitat: string;
  growth_rate: string;
  lifespan: string;
}

export interface IdentificationDetails {
  id: number;
  date: string;
  latitude: number;
  longitude: number;
}

export interface PlantHistoryItem {
  id: number;
  scientific_name: string;
  common_name: string;
  family: string;
  description: string;
  scan_date: string;
  latest_scan_date: string;
  image: string; // Image base64 de la plante
  soil_structure_score: number;
  water_retention_score: number;
  nitrogen_fixation_score: number;
  ecological_details: EcologicalDetails;
  identification_count: number;
  identifications: IdentificationDetails[]; // NOUVEAU : Liste des identifications avec coordonnées
}

export class PlantsHistoryResponse {
  plants: PlantHistoryItem[];
}