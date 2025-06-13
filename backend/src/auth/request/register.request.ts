import {
  IsArray,
  IsNumber,
  IsString,
  ValidateNested,
  IsEmail,
  IsNotEmpty,
} from 'class-validator';
import { Type } from 'class-transformer';
import { UserRole } from '../../user/user.enum';

export class RegisterRequest {
  @IsString()
  @IsNotEmpty()
  name: string;

  @IsString()
  @IsNotEmpty()
  first_name: string;

  @IsEmail()
  @IsString()
  @IsNotEmpty()
  email: string;

  @IsString()
  @IsNotEmpty()
  password: string;

  @IsString()
  @IsNotEmpty()
  role: UserRole;

  @IsString()
  @IsNotEmpty()
  location: string;
}
