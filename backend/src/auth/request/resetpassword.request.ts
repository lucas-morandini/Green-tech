import { IsString, IsEmail } from 'class-validator';

export class ResetPasswordRequest {
  @IsString()
  token: string;

  @IsString()
  password: string;

  @IsString()
  confirm_password: string;
}
