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
import { MessagePjDto } from './messagepj.dto';

export class MessageDto {
  @IsNumber()
  id: number;

  @IsDate()
  created_at: Date;

  @IsString()
  content: string;

  @IsNumber()
  @IsNotEmpty()
  userId: number;

  @IsNumber()
  @IsNotEmpty()
  groupId: number;

  @IsArray()
  pieces_jointe: MessagePjDto[];

  @Exclude()
  updated_at: Date;
}
