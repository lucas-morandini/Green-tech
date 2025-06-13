import {
  Controller,
  Post,
  Body,
  UnauthorizedException,
  BadRequestException,
  Get,
  Res,
  StreamableFile,
} from '@nestjs/common';
import { AuthService } from './auth.service';
import { UserDto } from 'src/user/dto/user.dto';
import { LoginRequest } from './request/login.request';
import { LoginResponse } from './response/login.response';
import {
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
} from '@nestjs/swagger';
import { RegisterResponse } from './response/register.response';
import { RegisterRequest } from './request/register.request';
import { plainToInstance } from 'class-transformer';
import { User } from '../user/user.entity';
import { Response } from 'express';
import { join } from 'path';
import { ForgotPasswordRequest } from './request/forgotpassword.request';
import { ResetPasswordRequest } from './request/resetpassword.request';
import { createReadStream } from 'fs';
import { CheckLoggedResponse } from './response/checklogged.response';
import { CurrentUser } from 'src/decorators/currentuser.decorator';
import { Public } from 'src/decorators/public.decorator';
@Controller('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Post('login')
  @ApiOperation({ summary: 'Login a user' })
  @ApiOkResponse({
    description: 'You have been logged in',
    type: LoginResponse,
  })
  @ApiNotFoundResponse({ description: 'User not found' })
  @Public()
  async login(@Body() body: LoginRequest): Promise<LoginResponse> {
    const { email, password } = body;
    const user: UserDto | null = await this.authService.validateUser(
      email,
      password,
    );
    if (!user) throw new BadRequestException('Invalid credentials');
    return this.authService.login(user);
  }

  @Post('/register')
  @ApiOperation({ summary: 'Register a user' })
  @ApiOkResponse({
    description: 'You have been registered',
    type: RegisterResponse,
  })
  @ApiNotFoundResponse({ description: 'User not found' })
  @Public()
  async identify(@Body() body: RegisterRequest) {
    const { name, first_name, email, password, role, location } = body;
    return this.authService.register(
      name,
      first_name,
      email,
      password,
      role,
      location,
    );
  }

  @Get('reset-password')
  @Public()
  getResetPassword(@Res() res: Response) {
    return res.sendFile(
      join(__dirname, '..', '..', 'view', 'reset-password.html'),
    );
  }

  @Post('reset-password')
  @Public()
  async resetPassword(
    @Body() body: ResetPasswordRequest,
    @Res() res: Response,
  ) {
    const { token, password, confirm_password } = body;
    // try {
    //     await this.authService.resetPassword(token, password, confirm_password);
    //     return res.sendFile(join(__dirname, '..', '..', 'view', 'reset-password-success.html'));
    // } catch (e) {
    //     // Redirige vers la page de reset avec le token et un flag d'erreur
    //     return res.redirect(`/auth/reset-password?token=${encodeURIComponent(token)}&error=1`);
    // }
    await this.authService.resetPassword(token, password, confirm_password);
    return res.sendFile(
      join(__dirname, '..', '..', 'view', 'reset-password-success.html'),
    );
  }

  @Post('forgot-password')
  @Public()
  async forgotPassword(@Body() body: ForgotPasswordRequest) {
    const { email } = body;
    await this.authService.forgotPassword(email);
    return { message: 'Password reset link sent to your email' };
  }

  @Get('check_logged')
  @ApiOperation({ summary: 'Check if the user is logged in' })
  @ApiOkResponse({
    description: 'User is logged in',
    type: CheckLoggedResponse,
  })
  @ApiNotFoundResponse({ description: 'User not found' })
  async checkLogged(
    @CurrentUser() user: UserDto,
  ): Promise<CheckLoggedResponse> {
    return { is_logged: !!user };
  }
}
