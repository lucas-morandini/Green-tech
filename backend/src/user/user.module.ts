import { forwardRef, Module } from '@nestjs/common';
import { UserController } from './user.controller';
import { UserService } from './user.service';
import { User } from './user.entity';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AuthModule } from 'src/auth/auth.module';
import { Friendship } from './friendship.entity';
import { GroupModule } from 'src/group/group.module';
import { NotificationModule } from 'src/notification/notification.module';
import { Identification } from 'src/identification/identification.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([User]),
    TypeOrmModule.forFeature([Friendship]),
    TypeOrmModule.forFeature([Identification]),
    forwardRef(() => AuthModule),
    forwardRef(() => GroupModule),
    NotificationModule,
  ],
  controllers: [UserController],
  providers: [UserService],
  exports: [UserService],
})
export class UserModule {}
