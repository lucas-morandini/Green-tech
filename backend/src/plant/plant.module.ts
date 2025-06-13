import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { PlantController } from './plant.controller';
import { PlantService } from './plant.service';
import { Plant } from './plant.entity';
import { Identification } from 'src/identification/identification.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Plant, Identification])],
  controllers: [PlantController],
  providers: [PlantService],
})
export class PlantModule {}
