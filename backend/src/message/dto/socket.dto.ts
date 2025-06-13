import { IsNotEmpty, IsNumber, IsArray, ValidateNested } from 'class-validator';
import { Type } from 'class-transformer';

class PieceJointeDto {
  @IsNotEmpty()
  name: string;

  @IsNotEmpty()
  byte64: string;

  @IsNotEmpty()
  date: Date;
}

export class SendMessageDto {
  @IsNumber()
  userId: number;

  @IsNumber()
  groupId: number;

  @IsNotEmpty()
  content: string;

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => PieceJointeDto)
  pieces_jointe: PieceJointeDto[];
}
