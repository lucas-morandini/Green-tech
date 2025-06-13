import { Exclude, Type } from 'class-transformer';
import {
  ArrayNotEmpty,
  IsArray,
  IsBoolean,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  Length,
  ValidateNested,
} from 'class-validator';
import { UserRole } from '../user.enum';
import { Optional } from '@nestjs/common';
import { Identification } from 'src/identification/identification.entity';

export class UserDto {
  @IsNumber()
  id: number;

  @IsNotEmpty()
  @Length(1, 50)
  name: string;

  @IsNotEmpty()
  @Length(1, 50)
  first_name: string;

  @IsNotEmpty()
  role: UserRole;

  @IsNotEmpty()
  location: string;

  @IsArray()
  identifications: Identification[];

  @IsOptional()
  @IsBoolean()
  is_friend?: boolean;

  @Exclude()
  created_at: Date;

  @Exclude()
  updated_at: Date;

  @Exclude()
  last_login: Date;

  @Exclude()
  email: string;

  @Exclude() // Exclure le mot de passe de la transformation
  password_hash: string;

  @Exclude()
  fcmToken?: string;
}
