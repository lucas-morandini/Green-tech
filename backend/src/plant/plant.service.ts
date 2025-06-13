/* eslint-disable @typescript-eslint/no-unsafe-member-access */
/* eslint-disable @typescript-eslint/no-unsafe-assignment */
import {
  Injectable,
  BadRequestException,
  NotFoundException,
  OnModuleInit,
} from '@nestjs/common';
import { Repository, DataSource } from 'typeorm';
import { InjectRepository } from '@nestjs/typeorm';
import { Plant } from './plant.entity';
import { Identification } from '../identification/identification.entity';
import { User } from '../user/user.entity';
import { PlantDto } from './dto/plant.dto';
import * as FormData from 'form-data';
import axios from 'axios';
import { IdentifyResponse } from './response/Identify.response';
import * as fs from 'fs';
import * as path from 'path';
import * as csv from 'csv-parser';
import { plainToInstance } from 'class-transformer';

interface PlantScore {
  service: string;
  species: string;
  value: number;
  reliability: number;
  cultural_condition: string;
}

@Injectable()
export class PlantService implements OnModuleInit {
  private plantScores: PlantScore[] = [];

  constructor(
    @InjectRepository(Plant)
    private readonly plantRepository: Repository<Plant>,
    @InjectRepository(Identification)
    private readonly identificationRepository: Repository<Identification>,
    private dataSource: DataSource,
  ) {}

  // Chargement du CSV au démarrage du module
  async onModuleInit() {
    await this.loadPlantScores();
  }

  // Méthode pour charger le CSV des scores
  private async loadPlantScores(): Promise<void> {
    const csvFilePath = path.join(__dirname, 'data', 'scores.csv');

    return new Promise((resolve, reject) => {
      fs.createReadStream(csvFilePath)
        .pipe(csv({ separator: ';' }))
        .on('data', (data: any) => {
          // Convertir les valeurs numériques
          const score: PlantScore = {
            service: data.service,
            species: data.species,
            value: parseFloat(data.value),
            reliability: parseFloat(data.reliability),
            cultural_condition: data.cultural_condition,
          };
          this.plantScores.push(score);
        })
        .on('end', () => {
          console.log(
            `Loaded ${this.plantScores.length} plant scores from CSV`,
          );
          resolve();
        })
        .on('error', (error) => {
          console.error('Error loading plant scores:', error);
          reject(error);
        });
    });
  }

  private cleanScientificName(scientificName: string): string {
    if (!scientificName) return '';
    
    // Enlever les espaces en trop et mettre en minuscules
    let cleaned = scientificName.trim().toLowerCase();
    
    // Enlever tout ce qui vient après le binôme (auteurs, sous-espèces, etc.)
    // Pattern pour capturer juste "Genre espèce"
    const binomialPattern = /^([a-z]+)\s+([a-z]+)/i;
    const match = cleaned.match(binomialPattern);
    
    if (match) {
      cleaned = `${match[1]} ${match[2]}`;
    }
    
    return cleaned;
  }

  private getPlantScores(scientificName: string): {
    nitrogen_fixation_score: number;
    soil_structure_score: number;
    water_retention_score: number;
  } {
    // Valeurs par défaut
    const defaultScores = {
      nitrogen_fixation_score: 0,
      soil_structure_score: 0,
      water_retention_score: 0,
    };

    if (!scientificName) {
      console.log('❌ Nom scientifique vide');
      return defaultScores;
    }

    console.log(`🔍 Recherche scores pour: "${scientificName}"`);

    // NOUVEAU : Normaliser le nom en enlevant l'auteur
    const cleanName = this.cleanScientificName(scientificName);
    console.log(`🧹 Nom nettoyé: "${cleanName}"`);

    // Filtrer les scores pour cette plante (avec nom nettoyé)
    const plantScores = this.plantScores.filter(score => {
      const csvName = this.cleanScientificName(score.species);
      return csvName === cleanName;
    });

    console.log(`📊 Scores trouvés: ${plantScores.length} entrées`);

    if (plantScores.length === 0) {
      // Log des noms disponibles pour debug
      const availableNames = [...new Set(this.plantScores.map(s => this.cleanScientificName(s.species)))]
        .slice(0, 5);
      console.log(`📋 Exemples disponibles: ${availableNames.join(', ')}...`);
      return defaultScores;
    }

    // Trouver les scores correspondant à chaque service
    const nitrogenScore = plantScores.find(score => score.service === 'nitrogen_provision');
    const soilScore = plantScores.find(score => score.service === 'soil_structuration');
    const waterScore = plantScores.find(score => score.service === 'storage_and_return_water');

    const result = {
      nitrogen_fixation_score: nitrogenScore ? nitrogenScore.value : 0,
      soil_structure_score: soilScore ? soilScore.value : 0,
      water_retention_score: waterScore ? waterScore.value : 0,
    };

    console.log(`✅ Scores trouvés - Sol: ${result.soil_structure_score}, Eau: ${result.water_retention_score}, Azote: ${result.nitrogen_fixation_score}`);

    return result;
  }

  // === MÉTHODES EXISTANTES (inchangées) ===

  async findAll(): Promise<Plant[]> {
    return this.plantRepository.find();
  }

  async findOne(id: number): Promise<Plant> {
    const plant = await this.plantRepository.findOneBy({ id });
    if (!plant) {
      throw new BadRequestException('Plant not found');
    }
    return plant;
  }

  async create(plantDto: PlantDto): Promise<Plant> {
    const plant = this.plantRepository.create(plantDto);
    return this.plantRepository.save(plant);
  }

  async update(id: number, plantDto: PlantDto): Promise<Plant> {
    await this.plantRepository.update(id, plantDto);
    return this.findOne(id);
  }

  async deleteOne(id: number): Promise<void> {
    const plant = await this.findOne(id);
    await this.plantRepository.remove(plant);
  }

  async search(search: string): Promise<PlantDto[]> {
    const plants = await this.plantRepository
      .createQueryBuilder('plant')
      .where('plant.scientific_name LIKE :search', { search: `%${search}%` })
      .orWhere('plant.common_name LIKE :search', { search: `%${search}%` })
      .orWhere('plant.family LIKE :search', { search: `%${search}%` })
      .orWhere('plant.description LIKE :search', { search: `%${search}%` })
      .leftJoinAndSelect('plant.identifications', 'identification')
      .getMany();
    const plantDtos = await Promise.all(
      plants.map(async (plant) => {
        const plantDto = plainToInstance(PlantDto, plant);
        return plantDto;
      }),
    );
    return plantDtos;
  }

  async identify(
    images: string[],
    date: string,
    location?: { latitude: number; longitude: number },
    userId?: number,
  ): Promise<IdentifyResponse> {
    console.log('=== IDENTIFY START ===');
    console.log(
      `Input: ${images.length} images, date: ${date}, location:`,
      location || 'none',
      `userId: ${userId || 'none'}`,
    );

    if (!images || images.length === 0) {
      console.error('ERROR: No images provided');
      throw new BadRequestException('No images provided');
    }
    if (!date) {
      console.error('ERROR: No date provided');
      throw new BadRequestException('No date provided');
    }
    if (!Array.isArray(images)) {
      console.error('ERROR: Images is not an array, type:', typeof images);
      throw new BadRequestException('Images should be an array');
    }

    const apiKey = process.env.PLANTNET_API;
    const base_url = process.env.PLANTNET_API_BASE_URL;
    console.log(
      `Config - API Key exists: ${!!apiKey}, Base URL exists: ${!!base_url}`,
    );

    if (!apiKey || !base_url) {
      console.error(
        'ERROR: API key or base URL not set in environment variables',
      );
      throw new BadRequestException('API key or base URL not set');
    }

    if (!Array.isArray(images) || images.length === 0) {
      console.error('ERROR: Invalid images array:', images);
      throw new BadRequestException('Invalid images array');
    }

    console.log('Preparing FormData with images...');
    const form = new FormData();
    for (let i = 0; i < images.length; i++) {
      try {
        const base64 = images[i];
        console.log(`Image ${i} starts with: ${base64.substring(0, 50)}...`);

        const mimeMatch = base64.match(/^data:(.+);base64,/);
        const mimeType = mimeMatch?.[1] || 'image/png';
        console.log(`Image ${i} MIME type: ${mimeType}`);

        const base64Data = base64.replace(/^data:image\/\w+;base64,/, '');

        try {
          const buffer = Buffer.from(base64Data, 'base64');
          console.log(`Image ${i} buffer length: ${buffer.length} bytes`);

          const extension = mimeType.split('/')[1];
          form.append('images', buffer, {
            filename: `image_${i}.${extension}`,
            contentType: mimeType,
          });
        } catch (error) {
          console.error(`ERROR: Invalid base64 for image ${i}:`, error);
          throw new BadRequestException(`Invalid base64 for image ${i}`);
        }
      } catch (error) {
        console.error(`ERROR processing image ${i}:`, error);
        throw new BadRequestException(
          `Error processing image ${i}: ${error.message}`,
        );
      }
    }

    const params = new URLSearchParams({
      'api-key': apiKey,
      'nb-results': '1',
      lang: 'en',
      'include-related-images': 'true',
    });

    console.log(`Calling PlantNet API at: ${base_url}/v2/identify/all`);

    try {
      console.log('Sending request to PlantNet...');
      const response = await axios.post(
        `${base_url}/v2/identify/all?${params.toString()}`,
        form,
        {
          headers: form.getHeaders(),
        },
      );

      console.log('PlantNet API response received, status:', response.status);
      const data = response.data;

      if (data.error) {
        console.error('ERROR from PlantNet API:', data.error);
        throw new BadRequestException('Error from PlantNet API: ' + data.error);
      }

      console.log(`PlantNet returned ${data.results?.length || 0} results`);

      if (!data.results || data.results.length === 0) {
        console.error('ERROR: No results from PlantNet API');
        throw new BadRequestException('No results from PlantNet API');
      }

      console.log(
        'First result species:',
        data.results[0].species?.scientificName || 'Unknown',
      );

      const wikipedia_base_url = process.env.WIKIPEDIA_API_BASE_URL;
      console.log(`Wikipedia base URL: ${wikipedia_base_url}`);

      const scientificNameWithoutAuthor =
        data.results[0].species?.scientificNameWithoutAuthor;
      console.log(
        `Fetching Wikipedia data for: ${scientificNameWithoutAuthor}`,
      );

      if (!scientificNameWithoutAuthor) {
        console.error('ERROR: No scientific name without author available');
        throw new BadRequestException(
          'No scientific name without author available',
        );
      }

      const wikipedia_url = `${wikipedia_base_url}/${encodeURIComponent(scientificNameWithoutAuthor)}`;
      console.log(`Wikipedia API URL: ${wikipedia_url}`);

      const response_wikipedia = await axios.get(wikipedia_url);
      console.log(
        'Wikipedia API response received, status:',
        response_wikipedia.status,
      );

      if (!response_wikipedia.data || response_wikipedia.data.error) {
        console.error(
          'ERROR from Wikipedia API:',
          response_wikipedia.data?.error || 'No data',
        );
        throw new BadRequestException(
          'Error from Wikipedia API: ' +
            (response_wikipedia.data?.error || 'No data'),
        );
      }

      if (!response_wikipedia.data.extract) {
        console.error('ERROR: No description available from Wikipedia API');
        throw new BadRequestException(
          'No description available from Wikipedia API',
        );
      }

      const description = response_wikipedia.data.extract;
      console.log(`Wikipedia description length: ${description.length} chars`);

      console.log('Processing results and fetching scores...');
      const plants = await Promise.all(
        data.results.map(async (result: any, index: number) => {
          console.log(`Processing result ${index + 1}/${data.results.length}`);

          const imageUrl = result.images
            ? result.images[0].url.m ||
              result.images[0].url.s ||
              result.images[0].url.o ||
              result.images[0].url.l
            : null;

          console.log(`Result ${index + 1} image URL:`, imageUrl || 'none');

          let base64Image: string | null = null;
          if (imageUrl) {
            try {
              base64Image = await this.convertImageToBase64(imageUrl);
              console.log(
                `Result ${index + 1} image converted to base64, length: ${base64Image?.length || 0}`,
              );
            } catch (error) {
              console.error(
                `ERROR converting image to base64 for result ${index + 1}:`,
                error,
              );
            }
          }

          // MODIFICATION : Utiliser le nom scientifique complet avec auteur pour les scores
          const scientificName = result.species?.scientificName || 'Unknown';
          console.log(`🌱 Getting scores for: ${scientificName}`);

          const scores = this.getPlantScores(scientificName);
          console.log(`📊 Final scores for ${scientificName}:`, scores);

          // Créer un nouvel objet avec les propriétés nécessaires pour la réponse
          const plantData = {
            scientific_name: scientificName,
            common_name: result.species?.commonNames?.join(', ') || 'Unknown',
            family: result.species?.family?.scientificName || 'Unknown',
            description: description || 'No description available',
            image: base64Image,
            nitrogen_fixation_score: scores.nitrogen_fixation_score,
            soil_structure_score: scores.soil_structure_score,
            water_retention_score: scores.water_retention_score,
            ecological_details: this.generateEcologicalDetails(scores),
          };

          console.log(`User ID: ${userId}`);
          // Si l'utilisateur est connecté, sauvegarder en base de données
          if (userId) {
            console.log(
              `User is logged in (ID: ${userId}), saving to database`,
            );

            // Vérifier si la plante existe déjà
            let plant = await this.plantRepository.findOne({
              where: { scientific_name: plantData.scientific_name },
            });

            // Si la plante n'existe pas, la créer
            if (!plant) {
              console.log(
                `Plant ${plantData.scientific_name} not found in database, creating new entry`,
              );

              // Créer sans l'image qui ne doit pas être stockée
              const plantToSave = {
                scientific_name: plantData.scientific_name,
                common_name: plantData.common_name,
                family: plantData.family,
                description: plantData.description,
                nitrogen_fixation_score: Math.round(
                  scores.nitrogen_fixation_score * 100,
                ),
                soil_structure_score: Math.round(
                  scores.soil_structure_score * 100,
                ),
                water_retention_score: Math.round(
                  scores.water_retention_score * 100,
                ),
                ecological_details: plantData.ecological_details,
                tela_botanica_url: '', // À compléter si disponible
              };

              plant = this.plantRepository.create(plantToSave);
              plant = await this.plantRepository.save(plant);
              console.log(`Plant saved with ID: ${plant.id}`);
            } else {
              console.log(
                `Plant already exists in database with ID: ${plant.id}`,
              );
            }

            // Créer l'identification
            console.log(
              `Creating identification entry for plant ID: ${plant.id}`,
            );
            const identification = new Identification();
            identification.date = new Date(date);

            if (location) {
              identification.latitude = location.latitude;
              identification.longitude = location.longitude;
            }

            identification.plante = plant;

            // Associer l'utilisateur
            const userRepository = this.dataSource.getRepository(User);
            const user = await userRepository.findOne({
              where: { id: userId },
            });
            if (user) {
              identification.user = user;
              console.log(`Associated with user ID: ${user.id}`);
            } else {
              console.log(`User with ID ${userId} not found`);
            }

            // Sauvegarder l'identification
            const savedIdentification =
              await this.identificationRepository.save(identification);
            console.log(
              `Identification saved with ID: ${savedIdentification.id}`,
            );

            // Ajouter les IDs à la réponse
            return {
              ...plantData,
              id: plant.id,
              identification_id: savedIdentification.id,
            };
          } else {
            // Si l'utilisateur n'est pas connecté, ne rien sauvegarder
            console.log('User not logged in, skipping database save');
            return plantData; // Retourner directement les données sans IDs de base de données
          }
        }),
      );

      console.log(`Processed ${plants.length} plants successfully`);

      const identifyResponse: IdentifyResponse = {
        plants: plants as any,
        scans: [
          {
            scan_date: date,
            location: location || null,
          },
        ],
      };

      console.log('=== IDENTIFY COMPLETE ===');
      console.log(
        `Returning ${identifyResponse.plants.length} plants and ${identifyResponse.scans.length} scans`,
      );

      return identifyResponse;
    } catch (error) {
      console.error('=== IDENTIFY ERROR ===');
      console.error('Error type:', error.constructor.name);
      console.error('Error message:', error.message);
      console.error('Error stack:', error.stack);

      if (axios.isAxiosError(error)) {
        console.error('Axios error details:');
        console.error('Status:', error.response?.status);
        console.error('Status text:', error.response?.statusText);
        console.error('Response data:', error.response?.data);
        console.error('Request config:', error.config);

        if (error.response?.status === 404) {
          throw new NotFoundException('Species not found');
        }
      }

      throw new BadRequestException(
        'Error from PlantNet API: ' + error.message,
      );
    }
  }

  async convertImageToBase64(imageUrl: string): Promise<string | null> {
    try {
      const response = await axios.get(imageUrl, {
        responseType: 'arraybuffer',
      });
      const buffer = Buffer.from(response.data, 'binary');
      return buffer.toString('base64');
    } catch (error) {
      console.error('Error converting image to base64:', error);
      return null;
    }
  }

  // Méthode utilitaire pour générer un texte de détails écologiques
  private generateEcologicalDetails(scores: {
    nitrogen_fixation_score: number;
    soil_structure_score: number;
    water_retention_score: number;
  }): string {
    const details: string[] = [];

    if (scores.nitrogen_fixation_score > 0.5) {
      details.push("Forte capacité de fixation d'azote");
    } else if (scores.nitrogen_fixation_score > 0.2) {
      details.push("Capacité modérée de fixation d'azote");
    }

    if (scores.soil_structure_score > 0.5) {
      details.push('Excellente amélioration de la structure du sol');
    } else if (scores.soil_structure_score > 0.2) {
      details.push("Contribue à l'amélioration de la structure du sol");
    }

    if (scores.water_retention_score > 0.5) {
      details.push("Haute capacité de rétention d'eau");
    } else if (scores.water_retention_score > 0.2) {
      details.push("Capacité modérée de rétention d'eau");
    }

    return details.length > 0
      ? details.join('. ') + '.'
      : 'Aucun détail écologique notable disponible pour cette plante.';
  }

  // === MÉTHODES UTILITAIRES POUR DEBUG  ===

  // Méthode pour lister toutes les espèces disponibles
  listAvailableSpecies(): string[] {
    return [...new Set(this.plantScores.map(score => this.cleanScientificName(score.species)))]
      .filter(name => name.length > 0)
      .sort();
  }

  // Méthode pour débugger le matching
  debugPlantMatching(scientificName: string) {
    const cleaned = this.cleanScientificName(scientificName);
    const scores = this.getPlantScores(scientificName);
    
    return {
      input: scientificName,
      cleaned: cleaned,
      scores: scores,
      hasScores: Object.values(scores).some(score => score > 0),
      totalSpeciesInCsv: this.listAvailableSpecies().length
    };
  }
}