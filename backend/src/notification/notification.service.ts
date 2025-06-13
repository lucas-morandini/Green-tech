import { Injectable } from '@nestjs/common';
import * as admin from 'firebase-admin';
import { Notification } from './notification.entity';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { NotificationDto } from './dto/notification.dto';
import { plainToInstance } from 'class-transformer';
const serviceAccount = require('../../green-techno-firebase-adminsdk-fbsvc-525b6d96a5.json');

@Injectable()
export class NotificationService {

    constructor(
        @InjectRepository(Notification)
        private readonly notificationRepository: Repository<Notification>,
    ) {
        if (!admin.apps.length) {
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount as admin.ServiceAccount),
            });
        }
    }

    async sendNotificationToToken(token: string, title: string, body: string, data?: any) {
        const message: admin.messaging.Message = {
            token,
            notification: {
                title,
                body,
            },
            data: data || {},
        };

        try {
            const response = await admin.messaging().send(message);
            console.log('✅ Notification envoyée :', response);
            return response;
        } catch (error) {
            console.error('❌ Erreur d\'envoi :', error);
            throw error;
        }
    }

    async createNotification(notification : Notification): Promise<Notification> {
        try {
            return await this.notificationRepository.save(notification);
        } catch (error) {
            console.error('❌ Erreur de création de notification :', error);
            throw error;
        }
    }

    async getNotifications(userId: number): Promise<NotificationDto[]> {
        try {
            const notifications = await this.notificationRepository.find({
                where: { user : { id: userId }, read: false },
                order: { created_at: 'DESC' },
                relations: ['user'],
            });
            return await Promise.all(
                notifications.map(notification => plainToInstance(NotificationDto, notification))
            );
        } catch (error) {
            console.error('❌ Erreur de récupération des notifications :', error);
            throw error;
        }
    }

    async updateAllNotificationsToRead(userId: number): Promise<NotificationDto[]> {
        try {
            const notifications = await this.notificationRepository.find({
                where: { user: { id: userId }, read: false },
            });

            if (notifications.length === 0) {
                return [];
            }

            notifications.forEach(notification => notification.read = true);
            await this.notificationRepository.save(notifications);

            return await Promise.all(
                notifications.map(notification => plainToInstance(NotificationDto, notification))
            );
        } catch (error) {
            console.error('❌ Erreur de mise à jour des notifications :', error);
            throw error;
        }
    }
}
