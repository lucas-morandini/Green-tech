import { BadRequestException, Injectable } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcryptjs';
import { UserDto } from '../user/dto/user.dto';
import { UserService } from '../user/user.service';
import { Repository } from 'typeorm';
import { User } from '../user/user.entity';
import { UserRole } from '../user/user.enum';
import { plainToInstance } from 'class-transformer';
import { MailerService } from '@nestjs-modules/mailer';

@Injectable()
export class AuthService {
  constructor(
    private userService: UserService,
    private jwtService: JwtService,
    private readonly mailerService: MailerService,
  ) {}
  // Hashage du mot de passe
  async hashPassword(password: string): Promise<string> {
    const salt = await bcrypt.genSalt(10); // Crée un salt
    return bcrypt.hash(password, salt); // Retourne le mot de passe hashé
  }

  // Comparaison du mot de passe avec le hash
  async comparePassword(
    password: string,
    hashedPassword: string,
  ): Promise<boolean> {
    return bcrypt.compare(password, hashedPassword); // Compare les deux mots de passe
  }

  async validateUser(email: string, password: string): Promise<UserDto | null> {
    const user = await this.userService.findByEmail(email);
    if (user && (await bcrypt.compare(password, user.password_hash))) {
      const { password_hash, ...result } = user;
      const userDto = plainToInstance(UserDto, result);
      return userDto;
    }
    return null;
  }

  async login(user: UserDto) {
    const payload = {
      sub: user.id,
      role: user.role,
      name: user.name,
      first_name: user.first_name,
      email: user.email,
      location: user.location,
    };
    return {
      access_token: this.jwtService.sign(payload),
      userId: user.id,
    };
  }

  async register(
    name: string,
    first_name: string,
    email: string,
    password: string,
    role: UserRole,
    location: string,
  ): Promise<UserDto> {
    if (await this.userService.findByEmail(email)) {
      throw new BadRequestException('Email already exists');
    }
    const user: User = await this.userService.createUser(
      name,
      first_name,
      email,
      await this.hashPassword(password),
      role,
      location,
    );
    const userDto = plainToInstance(UserDto, user);
    return userDto;
  }

  async forgotPassword(email: string): Promise<void> {
    const user = await this.userService.findByEmail(email);
    if (!user) {
      throw new BadRequestException('User not found');
    }

    // Générer un token et créer le lien de reset
    const resetToken = this.jwtService.sign({ email }, { expiresIn: '1h' });
    const resetUrl = `http://localhost:3000/auth/reset-password?token=${resetToken}`;

    // Envoi de l'e-mail
    await this.mailerService.sendMail({
      to: email,
      subject: 'Réinitialisation de votre mot de passe',
      template: './reset-password', // fichier handlebars dans /templates
      context: {
        name: user.first_name,
        resetUrl: resetUrl,
      },
    });
  }

  async resetPassword(
    token: string,
    newPassword: string,
    confirm_password: string,
  ): Promise<UserDto> {
    const decoded = this.jwtService.verify(token);
    const user = await this.userService.findByEmail(decoded.email);
    if (!user) {
      console.log('User not found');
      throw new BadRequestException('User not found');
    }
    if (newPassword !== confirm_password) {
      console.log('Passwords do not match');
      throw new BadRequestException('Passwords do not match');
    }
    user.password_hash = await this.hashPassword(newPassword);
    await this.userService.updateUser(user);
    // encode to userDto
    const userDto = plainToInstance(UserDto, user);
    return userDto;
  }

  async getUserFromToken(token: string): Promise<UserDto | null> {
    try {
      const decoded = this.jwtService.verify(token);
      const user = await this.userService.findByEmail(decoded.email);
      if (user) {
        const { password_hash, ...result } = user;
        const userDto = plainToInstance(UserDto, result);
        return userDto;
      }
    } catch (error) {
      console.error('Error decoding token:', error);
    }
    return null;
  }
}
