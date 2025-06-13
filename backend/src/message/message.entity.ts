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
import { MessagePJ } from './messagepj.entity';
@Entity()
export class Message {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  content: string;

  @Column()
  created_at: Date;

  @Column()
  updated_at: Date;

  @ManyToOne(() => User, (user) => user.messages)
  @JoinColumn({ name: 'userId' })
  user: User;

  @Column()
  userId: number;

  @ManyToOne(() => Group, (group) => group.messages)
  @JoinColumn({ name: 'groupId' })
  group: Group;

  @Column()
  groupId: number;

  // Un message peut avoir plusieurs pièces jointes
  @OneToMany(() => MessagePJ, (messagepj) => messagepj.message, {
    nullable: true,
  })
  pjs: MessagePJ[];
}
