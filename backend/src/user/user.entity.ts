import {
  Column,
  Entity,
  ManyToMany,
  OneToMany,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { UserRole } from './user.enum';
import { Friendship } from './friendship.entity';
import { Group } from 'src/group/group.entity';
import { Message } from 'src/message/message.entity';
import { Identification } from 'src/identification/identification.entity';
import { Notification } from 'src/notification/notification.entity';

@Entity()
export class User {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  name: string;

  @Column()
  first_name: string;

  @Column()
  email: string;

  @Column()
  password_hash: string;

  @Column()
  created_at: Date;

  @Column()
  updated_at: Date;

  @Column()
  last_login: string;

  @Column({
    type: 'enum',
    enum: UserRole,
  })
  role: UserRole;

  @Column()
  location: string;

  @Column({ nullable: true })
  fcmToken: string;

  // Relation avec l'entité Friendship pour récupérer les amis
  @OneToMany(() => Friendship, (friendship) => friendship.user, {
    nullable: true,
  })
  friendships: Friendship[];

  // Relation avec l'entité Friendship pour récupérer les demandes d'amitié en attente
  @OneToMany(() => Friendship, (friendship) => friendship.friend, {
    nullable: true,
  })
  pendingRequests: Friendship[];

  // Relation avec l'entité Group pour récupérer les groupes auxquels l'utilisateur appartient
  @ManyToMany(() => Group, (group) => group.users)
  groups: Group[];

  // Relation avec l'entité Message pour récupérer les messages envoyés par l'utilisateur
  @OneToMany(() => Message, (message) => message.user, { nullable: true })
  messages: Message[];

  @OneToMany(() => Identification, (id) => id.user)
  identifications: Identification[];

  @OneToMany(() => Notification, (notification) => notification.user, {
    nullable: true,
  })
  notifications: Notification[];
}
