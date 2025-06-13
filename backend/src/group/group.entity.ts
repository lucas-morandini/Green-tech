import { Identification } from 'src/identification/identification.entity';
import { Message } from 'src/message/message.entity';
import { User } from 'src/user/user.entity';
import {
  Column,
  Entity,
  JoinTable,
  ManyToMany,
  OneToMany,
  PrimaryGeneratedColumn,
} from 'typeorm';

@Entity()
export class Group {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  name: string;

  @Column()
  created_at: Date;

  @Column()
  updated_at: Date;

  // des utilisateurs User peuvent appartenir à plusieurs groupes
  @ManyToMany(() => User, (user) => user.groups)
  @JoinTable()
  users: User[];

  // Relation avec l'entité Message pour récupérer les messages envoyés par l'utilisateur
  @OneToMany(() => Message, (message) => message.group, { nullable: true })
  messages: Message[];

  @OneToMany(() => Identification, (id) => id.group)
  identifications: Identification[];
}
