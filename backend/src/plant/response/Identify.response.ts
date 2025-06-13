import { Plant } from '../plant.entity';

export class IdentifyResponse {
  plants: Plant[];
  scans: [
    {
      scan_date: string;
      location: { latitude: number; longitude: number } | null;
    },
  ];
}
