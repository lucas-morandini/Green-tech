import { Exclude, Type } from 'class-transformer';
import {
  ArrayNotEmpty,
  IsArray,
  IsDate,
  IsNotEmpty,
  IsNumber,
  IsString,
  Length,
  ValidateNested,
} from 'class-validator';
import { Optional } from '@nestjs/common';
import { UserDto } from 'src/user/dto/user.dto';
import { Identification } from 'src/identification/identification.entity';

export class MessagePjDto {
  @IsNumber()
  id: number;

  @IsDate()
  created_at: Date;

  @IsString()
  byte64: string;

  @IsString()
  @Length(1, 255)
  name: string;

  @IsNumber()
  messageId: number;
}
