import { ApiProperty } from '@nestjs/swagger';
import {
  IsArray,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  IsUrl,
  Max,
  Min,
} from 'class-validator';
import { Identification } from 'src/identification/identification.entity';

export class PlantDto {
  @ApiProperty({
    description: 'Scientific name of the plant',
    example: 'Trifolium pratense',
  })
  @IsNotEmpty()
  @IsString()
  scientific_name: string;

  @ApiProperty({
    description: 'Common name of the plant',
    example: 'Red Clover',
  })
  @IsNotEmpty()
  @IsString()
  common_name: string;

  @ApiProperty({
    description: 'Plant family',
    example: 'Fabaceae',
  })
  @IsNotEmpty()
  @IsString()
  family: string;

  @ApiProperty({
    description: 'Plant description',
    example: 'Perennial herb with red-purple flowers',
  })
  @IsNotEmpty()
  @IsString()
  description: string;

  @ApiProperty({
    description: 'URL to Tela Botanica page',
    example: 'https://www.tela-botanica.org/bdtfx-nn-69412',
  })
  @IsOptional()
  @IsUrl()
  tela_botanica_url: string;

  @ApiProperty({
    description: 'Nitrogen fixation score (0-1)',
    minimum: 0,
    maximum: 1,
    example: 0.8,
  })
  @IsNumber()
  @Min(0)
  @Max(1)
  nitrogen_fixation_score: number;

  @ApiProperty({
    description: 'Soil structure improvement score (0-1)',
    minimum: 0,
    maximum: 1,
    example: 0.7,
  })
  @IsNumber()
  @Min(0)
  @Max(1)
  soil_structure_score: number;

  @ApiProperty({
    description: 'Water retention score (0-1)',
    minimum: 0,
    maximum: 1,
    example: 0.6,
  })
  @IsNumber()
  @Min(0)
  @Max(1)
  water_retention_score: number;

  @ApiProperty({
    description: 'Ecological details and benefits',
    example: 'Good for pollinators and fixing nitrogen',
  })
  @IsOptional()
  @IsString()
  ecological_details: string;

  @IsArray()
  identifications: Identification[];
}
