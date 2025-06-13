import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  ManyToOne,
  CreateDateColumn,
} from 'typeorm';
import { User } from 'src/user/user.entity';
import { Group } from 'src/group/group.entity';
import { Plant } from 'src/plant/plant.entity';

@Entity()
export class Identification {
  @PrimaryGeneratedColumn()
  id: number;

  @CreateDateColumn()
  date: Date;

  @Column('float', { nullable: true })
  latitude: number;

  @Column('float', { nullable: true })
  longitude: number;

  @ManyToOne(() => Plant, { nullable: false, onDelete: 'CASCADE' })
  plante: Plant;

  @ManyToOne(() => User, (user) => user.identifications, {
    nullable: true,
    onDelete: 'CASCADE',
  })
  user: User;

  @ManyToOne(() => Group, (group) => group.identifications, {
    nullable: true,
    onDelete: 'CASCADE',
  })
  group: Group;
}
