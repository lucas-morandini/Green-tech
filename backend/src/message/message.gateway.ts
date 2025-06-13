// src/chat/chat.gateway.ts
import { InjectRepository } from '@nestjs/typeorm';
import {
  SubscribeMessage,
  WebSocketGateway,
  OnGatewayConnection,
  OnGatewayDisconnect,
  MessageBody,
  ConnectedSocket,
  WebSocketServer,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { Message } from './message.entity';
import { Repository } from 'typeorm';
import { MessageDto } from './dto/message.dto';
import { plainToInstance } from 'class-transformer';
import { MessagePjDto } from './dto/messagepj.dto';
import { MessagePJ } from './messagepj.entity';
import { ValidationPipe } from '@nestjs/common';
import { SendMessageDto } from './dto/socket.dto';
import { NotificationService } from 'src/notification/notification.service';
import { Group } from 'src/group/group.entity';

@WebSocketGateway({
  cors: {
    origin: '*',
  },
})
export class MessageGateway
  implements OnGatewayConnection, OnGatewayDisconnect
{
  constructor(
    @InjectRepository(Message)
    private readonly messageRepository: Repository<Message>,
    @InjectRepository(MessagePJ)
    private readonly messagePJRepository: Repository<MessagePJ>,
    @InjectRepository(Group)
    private readonly groupRepository: Repository<Group>,
    private readonly notificationService: NotificationService,
  ) {}

  @WebSocketServer()
  server: Server;

  handleConnection(client: Socket) {
    console.log(`Client connected: ${client.id}`);
  }

  handleDisconnect(client: Socket) {
    console.log(`Client disconnected: ${client.id}`);
  }

  // Rejoindre une room (un groupe)
  @SubscribeMessage('join_group')
  handleJoinGroup(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { groupId: number },
  ) {
    client.join(data.groupId.toString());
    console.log(`Client ${client.id} joined group ${data.groupId}`);
  }

  // Quitter une room (optionnel)
  @SubscribeMessage('leave_group')
  handleLeaveGroup(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { groupId: number },
  ) {
    client.leave(data.groupId.toString());
    console.log(`Client ${client.id} left group ${data.groupId}`);
  }

  // Envoyer un message à un groupe
  @SubscribeMessage('send_message')
  async handleMessage(
    @MessageBody(new ValidationPipe()) data: SendMessageDto,
    @ConnectedSocket() client: Socket,
  ) {
    // save le message en base de données
    if (!data.content) {
      console.error('❌ CONTENU MANQUANT !', data);
    }

    const message = this.messageRepository.create({
      content: data.content,
      userId: data.userId,
      groupId: data.groupId,
      created_at: new Date(),
      updated_at: new Date(),
    });
    await this.messageRepository.save(message);

    // save les pièces jointes
    if (data.pieces_jointe) {
      for (const pj of data.pieces_jointe) {
        const messagePj = this.messagePJRepository.create({
          byte64: pj.byte64,
          name: pj.name,
          created_at: new Date(),
          updated_at: new Date(),
          messageId: message.id,
        });
        await this.messagePJRepository.save(messagePj);
      }
    }
    // Récupérer le message enregistré pour l'envoyer à tous les clients
    const savedMessage = await this.messageRepository.findOne({
      where: { id: message.id },
      relations: ['user', 'group', 'pjs'],
    });
    if (!savedMessage) {
      return { status: 'error', error: 'Failed to retrieve message' };
    }
    const messageDto = plainToInstance(MessageDto, savedMessage);
    messageDto.groupId = savedMessage.group.id;
    messageDto.userId = savedMessage.user.id;
    messageDto.pieces_jointe = await Promise.all(
      savedMessage.pjs.map(async (pj) => {
        const messagePjDto = new MessagePjDto();
        messagePjDto.id = pj.id;
        messagePjDto.byte64 = pj.byte64;
        messagePjDto.messageId = pj.messageId;
        messagePjDto.name = pj.name;
        messagePjDto.created_at = pj.created_at;
        return messagePjDto;
      }),
    );
    this.server.to(data.groupId.toString()).emit('new_message', {
      message: messageDto,
    });
    const group = await this.groupRepository.findOne({
      where: { id: data.groupId },
      relations: ['users'],
    });
    if (!group) {
      console.error('❌ Groupe non trouvé !', data.groupId);
      return { status: 'error', error: 'Group not found' };
    }
    const recipients = group.users.filter((user) => user.id !== data.userId);
    if (recipients.length === 0) {
      console.warn('Aucun destinataire pour la notification');
      return { status: 'ok' };
    }
    for (const recipient of recipients) {
      if (recipient.fcmToken) {
        try {
          await this.notificationService.sendNotificationToToken(
            recipient.fcmToken,
            `Groupe ${group.name}`,
            message.content,
            { groupId: group.id.toString(), messageId: message.id.toString() },
          );
          console.log(`✅ Notification envoyée à ${recipient.id}`);
        } catch (error) {
          console.error('❌ Erreur d\'envoi de notification :', error);
        }
      } else {
        console.warn(`Aucun token FCM pour l'utilisateur ${recipient.id}`);
      }
    }
    return { status: 'ok' };
  }
}
