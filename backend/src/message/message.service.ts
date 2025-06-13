import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Message } from './message.entity';
import { In, Repository } from 'typeorm';
import { MessagePJ } from './messagepj.entity';
import { MessageDto } from './dto/message.dto';
import { MessagePjDto } from './dto/messagepj.dto';

@Injectable()
export class MessageService {
  constructor(
    @InjectRepository(Message)
    private readonly messageRepository: Repository<Message>,
    @InjectRepository(MessagePJ)
    private readonly messagePJRepository: Repository<MessagePJ>,
  ) {}

  async findMessagesByGroupId(groupId: number): Promise<MessageDto[]> {
    const messages = await this.messageRepository.find({
      where: { group: { id: groupId } },
      order: { created_at: 'DESC' },
      relations: ['user', 'group', 'pjs'],
    });

    const messageDtos = messages.map((message) => {
      const messageDto = new MessageDto();
      messageDto.id = message.id;
      messageDto.created_at = message.created_at;
      messageDto.content = message.content;
      messageDto.userId = message.user.id;
      messageDto.groupId = message.group.id;
      messageDto.pieces_jointe = message.pjs.map((pj) => {
        const messagePjDto = new MessagePjDto();
        messagePjDto.id = pj.id;
        messagePjDto.byte64 = pj.byte64;
        messagePjDto.messageId = pj.messageId;
        messagePjDto.name = pj.name;
        messagePjDto.created_at = pj.created_at;
        return messagePjDto;
      });
      return messageDto;
    });
    return messageDtos;
  }
}
