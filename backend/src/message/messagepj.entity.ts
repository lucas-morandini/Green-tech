import { User } from 'src/user/user.entity';
import {
  Column,
  Entity,
  JoinColumn,
  ManyToOne,
  OneToMany,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { Group } from 'src/group/group.entity';
import { Message } from './message.entity';
@Entity()
export class MessagePJ {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ type: 'longtext' })
  byte64: string;

  @Column()
  name: string;

  @Column()
  created_at: Date;

  @Column()
  updated_at: Date;

  // un message peut avoir plusieurs pjs
  @ManyToOne(() => Message, (message) => message.pjs)
  @JoinColumn({ name: 'messageId' })
  message: Message;

  @Column()
  messageId: number;
}
