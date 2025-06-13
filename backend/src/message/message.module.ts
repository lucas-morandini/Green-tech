import { Module } from '@nestjs/common';
import { MessageController } from './message.controller';
import { MessageService } from './message.service';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Message } from './message.entity';
import { MessagePJ } from './messagepj.entity';
import { MessageGateway } from './message.gateway';
import { NotificationModule } from 'src/notification/notification.module';
import { Group } from 'src/group/group.entity';

@Module({
  controllers: [MessageController],
  providers: [MessageService, MessageGateway],
  exports: [MessageService],
  imports: [
    TypeOrmModule.forFeature([Message]),
    TypeOrmModule.forFeature([MessagePJ]),
    NotificationModule,
    TypeOrmModule.forFeature([Group]),
  ],
})
export class MessageModule {}
