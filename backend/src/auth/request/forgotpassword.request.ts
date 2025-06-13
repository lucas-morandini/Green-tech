import { IsString, IsEmail } from 'class-validator';

export class ForgotPasswordRequest {
  @IsEmail()
  @IsString()
  email: string;
}
