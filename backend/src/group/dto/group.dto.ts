import { Exclude, Type } from 'class-transformer';
import {
  ArrayNotEmpty,
  IsArray,
  IsNotEmpty,
  IsNumber,
  Length,
  ValidateNested,
} from 'class-validator';
import { Optional } from '@nestjs/common';
import { UserDto } from 'src/user/dto/user.dto';
import { Identification } from 'src/identification/identification.entity';
import { MessageDto } from 'src/message/dto/message.dto';

export class GroupDto {
  @IsNumber()
  id: number;

  @IsNotEmpty()
  @Length(1, 50)
  name: string;

  @IsNotEmpty()
  @IsArray()
  users: UserDto[];

  @IsArray()
  messages: MessageDto[];

  @IsArray()
  identifications: Identification[];

  @Exclude()
  created_at: Date;
  @Exclude()
  updated_at: Date;
}
