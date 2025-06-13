import {
  IsArray,
  IsNumber,
  IsString,
  ValidateNested,
  IsEmail,
} from 'class-validator';
import { Type } from 'class-transformer';

export class LoginRequest {
  @IsEmail()
  @IsString()
  email: string;

  @IsString()
  password: string;
}
