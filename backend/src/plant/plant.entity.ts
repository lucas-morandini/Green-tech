import { Identification } from 'src/identification/identification.entity';
import { Column, Entity, OneToMany, PrimaryGeneratedColumn } from 'typeorm';

@Entity()
export class Plant {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  scientific_name: string;

  @Column()
  common_name: string;

  @Column()
  family: string;

  @Column('text')
  description: string;

  @Column({ nullable: true })
  tela_botanica_url: string;

  @Column()
  nitrogen_fixation_score: number;

  @Column()
  soil_structure_score: number;

  @Column()
  water_retention_score: number;

  @Column({ nullable: true })
  ecological_details: string;

  @OneToMany(() => Identification, (id) => id.plante)
  identifications: Identification[];
}
