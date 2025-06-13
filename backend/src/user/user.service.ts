import { BadRequestException, Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { User } from './user.entity';
import { Repository } from 'typeorm';
import { UserRole } from './user.enum';
import { UserDto } from './dto/user.dto';
import { plainToInstance } from 'class-transformer';
import { Friendship } from './friendship.entity';
import { GroupService } from 'src/group/group.service';
import { NotificationService } from 'src/notification/notification.service';
import { Notification } from 'src/notification/notification.entity';
import { Identification } from 'src/identification/identification.entity';
import { Plant } from 'src/plant/plant.entity';
import {
  PlantHistoryItem,
  EcologicalDetails,
} from './response/plants-history.response';

@Injectable()
export class UserService {
  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    @InjectRepository(Friendship)
    private readonly friendshipRepository: Repository<Friendship>,
    @InjectRepository(Identification)
    private readonly identificationRepository: Repository<Identification>,
    private readonly groupService: GroupService,
    private readonly notificationService: NotificationService
  ) { }

  async findByEmail(email: string): Promise<User | null> {
    return this.userRepository.findOneBy({ email });
  }

  async findById(id: number): Promise<User | null> {
    return this.userRepository.findOneBy({ id });
  }

  async updateUser(user: User): Promise<User> {
    return this.userRepository.save(user);
  }

  async search(search: string, me: UserDto): Promise<UserDto[]> {
    const users = await this.userRepository
      .createQueryBuilder('user')
      .where('(user.name LIKE :search OR user.first_name LIKE :search)', { search: `%${search}%` })
      .andWhere('user.id != :userId', { userId: me.id })
      .leftJoinAndSelect('user.identifications', 'identification')
      .getMany();

    const userDtos = await Promise.all(
      users.map(async (user) => {
        const userDto = plainToInstance(UserDto, user);

        // Vérifie si une relation d’amitié acceptée existe dans les deux sens
        const friend = await this.friendshipRepository.findOne({
          where: [
            { user: { id: me.id }, friend: { id: userDto.id }, accepted: true },
            { user: { id: userDto.id }, friend: { id: me.id }, accepted: true },
          ],
        });

        userDto.is_friend = !!friend;
        return userDto;
      }),
    );

    return userDtos;
  }

  async createUser(
    name: string,
    first_name: string,
    email: string,
    password_hash: string,
    role: UserRole,
    location: string,
  ): Promise<User> {
    const user = this.userRepository.create({
      name,
      first_name,
      email,
      password_hash: password_hash,
      created_at: new Date(),
      updated_at: new Date(),
      last_login: new Date().toString(),
      role,
      location: location,
    });
    return this.userRepository.save(user);
  }

  async getFriends(userId: number): Promise<UserDto[]> {
    const userEntity = await this.userRepository.findOneBy({ id: userId });

    if (!userEntity) {
      return [];
    }

    // Amis ajoutés par moi et acceptés
    const outgoing = await this.friendshipRepository.find({
      where: { user: { id: userId }, accepted: true },
      relations: ['friend'],
    });

    // Amis qui m’ont ajouté et que j’ai acceptés
    const incoming = await this.friendshipRepository.find({
      where: { friend: { id: userId }, accepted: true },
      relations: ['user'],
    });

    const outgoingFriends = outgoing.map((f) =>
      plainToInstance(UserDto, f.friend),
    );
    const incomingFriends = incoming.map((f) =>
      plainToInstance(UserDto, f.user),
    );

    return [...outgoingFriends, ...incomingFriends];
  }

  async addFriend(userId: number, friendId: number): Promise<boolean> {
    if (userId === friendId) {
      throw new BadRequestException('You cannot add yourself as a friend');
    }

    const user = await this.userRepository.findOneBy({ id: userId });
    const friend = await this.userRepository.findOneBy({ id: friendId });

    if (!user || !friend) {
      throw new BadRequestException('User or friend not found');
    }

    // Vérifie si l'utilisateur a déjà envoyé une demande (empêche les doublons)
    const existing = await this.friendshipRepository.findOne({
      where: {
        user: { id: userId },
        friend: { id: friendId },
      },
    });

    if (existing) {
      return true;
    }

    const inverse = await this.friendshipRepository.findOne({
      where: {
        user: { id: friendId },
        friend: { id: userId },
      },
    });

    if (inverse) {
      inverse.accepted = true;
      await this.friendshipRepository.save(inverse);
      const users: UserDto[] = [
        plainToInstance(UserDto, user),
        plainToInstance(UserDto, friend),
      ];
      await this.groupService.createGroup('', users);
      const notification: Notification = new Notification();
      notification.title = 'Demande d\'ami acceptée';
      notification.content = `${friend.name} ${friend.first_name} a accepté votre demande d'amitié.`;
      notification.user = user;
      await this.notificationService.createNotification(notification);
      try{
        await this.notificationService.sendNotificationToToken(
          user.fcmToken,
          notification.title,
          notification.content,
          { groupId: '', messageId: '' },
        );
      }catch (error) {
        console.error('Error sending notification:', error);
      }
    } else {
      const newFriendship = this.friendshipRepository.create({
        user,
        friend,
        accepted: false,
      });
      await this.friendshipRepository.save(newFriendship);
      const notification : Notification = new Notification();
      notification.title = 'Nouvelle demande d\'ami';
      notification.content = `${user.name} ${user.first_name} vous a envoyé une demande d'amitié.`;
      notification.user = friend;
      await this.notificationService.createNotification(notification);
    }

    return true;
  }

  async setFcmToken(userId: number, fcmToken: string): Promise<boolean> {
    const user = await this.userRepository.findOneBy({ id: userId });
    if (!user) {
      throw new BadRequestException('User not found');
    }
    user.fcmToken = fcmToken;
    await this.userRepository.save(user);
    return true;
  }
  async getPlantsHistory(userId: number): Promise<PlantHistoryItem[]> {
  // Récupérer toutes les identifications de l'utilisateur avec leurs plantes
  const identifications = await this.identificationRepository.find({
    where: { user: { id: userId } },
    relations: ['plante'],
    order: { date: 'DESC' },
  });

  if (!identifications || identifications.length === 0) {
    return [];
  }

  // Grouper les identifications par plante
  const plantGroups = new Map<number, {
    plant: Plant;
    identifications: Identification[];
    firstDate: Date;
    latestDate: Date;
  }>();

  identifications.forEach((identification) => {
    const plantId = identification.plante.id;
    
    if (!plantGroups.has(plantId)) {
      plantGroups.set(plantId, {
        plant: identification.plante,
        identifications: [],
        firstDate: identification.date,
        latestDate: identification.date,
      });
    }
    
    const group = plantGroups.get(plantId)!;
    group.identifications.push(identification);
    
    // Mettre à jour les dates
    if (identification.date < group.firstDate) {
      group.firstDate = identification.date;
    }
    if (identification.date > group.latestDate) {
      group.latestDate = identification.date;
    }
  });

  // Transformer les données groupées
  const plantsHistory: PlantHistoryItem[] = await Promise.all(
    Array.from(plantGroups.values()).map(async (group) => {
      const plant = group.plant;
      
      // Générer les détails écologiques
      const ecologicalDetails = this.generateEcologicalDetailsFromPlant(plant);
      
      // NOUVEAU : Obtenir une image pour cette plante
      const plantImage = await this.getPlantImage(plant.scientific_name);
      
      // NOUVEAU : Formater les identifications avec coordonnées
      const identificationsDetails = group.identifications.map(identification => ({
        id: identification.id,
        date: identification.date.toISOString(),
        latitude: identification.latitude || 0,
        longitude: identification.longitude || 0,
      }));
      
      return {
        id: plant.id,
        scientific_name: plant.scientific_name,
        common_name: plant.common_name,
        family: plant.family,
        description: plant.description,
        scan_date: this.formatScanDate(group.firstDate),
        latest_scan_date: this.formatScanDate(group.latestDate),
        image: plantImage, // NOUVEAU : Image de la plante
        soil_structure_score: Math.round(plant.soil_structure_score * 100),
        water_retention_score: Math.round(plant.water_retention_score * 100),
        nitrogen_fixation_score: Math.round(plant.nitrogen_fixation_score * 100),
        ecological_details: ecologicalDetails,
        identification_count: group.identifications.length,
        identifications: identificationsDetails, // NOUVEAU : Vraies coordonnées
      };
    })
  );

  // Trier par date de dernière identification
  plantsHistory.sort((a, b) => new Date(b.latest_scan_date).getTime() - new Date(a.latest_scan_date).getTime());

  return plantsHistory;
}

  private formatScanDate(date: Date): string {
    const options: Intl.DateTimeFormatOptions = {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    };
    return new Intl.DateTimeFormat('en-US', options).format(date);
  }

  private generateEcologicalDetailsFromPlant(plant: Plant): EcologicalDetails {
  // Si les détails écologiques existent déjà, essayer de les parser
  if (plant.ecological_details) {
    try {
      const parsed = JSON.parse(plant.ecological_details);
      if (parsed.habitat && parsed.growth_rate && parsed.lifespan) {
        return {
          habitat: parsed.habitat,
          growth_rate: parsed.growth_rate,
          lifespan: parsed.lifespan,
        };
      }
    } catch (error) {
      // Continuer avec la génération automatique
    }
  }

  // Générer des détails basés sur les scores et la famille
  let habitat = 'Various environments';
  let growthRate = 'Medium';
  let lifespan = 'Perennial';

  // Déterminer l'habitat basé sur la famille
  const family = plant.family.toLowerCase();
  if (family.includes('fabaceae')) {
    habitat = 'Meadows, fields, gardens';
  } else if (family.includes('rosaceae')) {
    habitat = 'Gardens, woodlands, hedgerows';
  } else if (family.includes('asteraceae')) {
    habitat = 'Fields, roadsides, disturbed areas';
  } else if (family.includes('alstroemeriaceae')) {
    habitat = 'Gardens, borders, greenhouse cultivation';
  }

  // Déterminer le taux de croissance basé sur les scores
  const averageScore = (plant.soil_structure_score + plant.water_retention_score + plant.nitrogen_fixation_score) / 3;
  if (averageScore > 0.7) {
    growthRate = 'Fast';
  } else if (averageScore < 0.3) {
    growthRate = 'Slow';
  }

  // Déterminer la durée de vie
  if (plant.soil_structure_score > 0.6) {
    lifespan = 'Perennial';
  } else {
    lifespan = 'Annual to perennial';
  }

  return {
    habitat,
    growth_rate: growthRate,
    lifespan,
  };
}

  private async getPlantImage(scientificName: string): Promise<string> {
  try {
    // Option 1 : Depuis une API d'images de plantes (ex: PlantNet, iNaturalist)
    const imageUrl = await this.fetchPlantImageFromAPI(scientificName);
    if (imageUrl) {
      const base64Image = await this.convertImageUrlToBase64(imageUrl);
      return base64Image;
    }
    
    // Option 2 : Depuis un stockage local d'images
    const localImage = await this.getLocalPlantImage(scientificName);
    if (localImage) {
      return localImage;
    }
    
    // Option 3 : Image par défaut
    return '';
    
  } catch (error) {
    console.error('Erreur récupération image plante:', error);
    return '';
  }
}

private async fetchPlantImageFromAPI(scientificName: string): Promise<string | null> {
  try {
    // Exemple avec une API publique (à adapter selon tes besoins)
    const apiKey = process.env.PLANT_IMAGE_API_KEY;
    if (!apiKey) return null;
    
    // Exemple d'URL - remplace par l'API de ton choix
    const apiUrl = `https://api.plant-images.com/search?q=${encodeURIComponent(scientificName)}&key=${apiKey}`;
    
    const response = await fetch(apiUrl);
    const data = await response.json();
    
    if (data.images && data.images.length > 0) {
      return data.images[0].url; // Première image trouvée
    }
    
    return null;
  } catch (error) {
    console.error('Erreur API image plante:', error);
    return null;
  }
}


private async convertImageUrlToBase64(imageUrl: string): Promise<string> {
  try {
    const response = await fetch(imageUrl);
    const arrayBuffer = await response.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);
    const base64 = buffer.toString('base64');
    
    // Ajouter le préfixe data: selon le type MIME
    const contentType = response.headers.get('content-type') || 'image/jpeg';
    return `data:${contentType};base64,${base64}`;
    
  } catch (error) {
    console.error('Erreur conversion image URL vers base64:', error);
    return '';
  }
}

private async getLocalPlantImage(scientificName: string): Promise<string | null> {
  try {
    // Exemple : images stockées dans public/plant-images/
    const imagePath = `./public/plant-images/${scientificName.toLowerCase().replace(' ', '_')}.jpg`;
    
    const fs = require('fs').promises;
    if (await fs.access(imagePath).then(() => true).catch(() => false)) {
      const imageBuffer = await fs.readFile(imagePath);
      const base64 = imageBuffer.toString('base64');
      return `data:image/jpeg;base64,${base64}`;
    }
    
    return null;
  } catch (error) {
    console.error('Erreur image locale:', error);
    return null;
  }
}
}
