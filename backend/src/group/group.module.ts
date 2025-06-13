import { forwardRef, Module } from '@nestjs/common';
import { GroupController } from './group.controller';
import { GroupService } from './group.service';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Group } from './group.entity';
import { Message } from 'src/message/message.entity';
import { UserModule } from 'src/user/user.module';
import { MessageModule } from 'src/message/message.module';

@Module({
  controllers: [GroupController],
  providers: [GroupService],
  exports: [GroupService],
  imports: [
    TypeOrmModule.forFeature([Group]),
    TypeOrmModule.forFeature([Message]),
    forwardRef(() => UserModule),
    MessageModule,
  ],
})
export class GroupModule {}
