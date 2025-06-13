import {
    BadRequestException,
    Body,
    Controller,
    Get,
    Param,
    Post,
    Put,
    Query,
} from '@nestjs/common';
import {
    ApiNotFoundResponse,
    ApiOkResponse,
    ApiOperation,
    ApiTags,
} from '@nestjs/swagger';
import { RegisterResponse } from '../auth/response/register.response';
import { RegisterRequest } from '../auth/request/register.request';
import { AuthService } from 'src/auth/auth.service';
import { CurrentUser } from 'src/decorators/currentuser.decorator';
import { Public } from 'src/decorators/public.decorator';
import { plainToInstance } from 'class-transformer';
import { NotificationService } from './notification.service';
import { UserDto } from 'src/user/dto/user.dto';
import { NotificationDto } from './dto/notification.dto';

@ApiTags('notifications')
@Controller('notification')
export class NotificationController {
    constructor(
        private readonly notificationService: NotificationService
    ) { }

    @Get('/')
    @ApiOperation({ summary: 'Get all notifications' })
    @ApiOkResponse({
        description: 'List of notifications',
        type: RegisterResponse,
        isArray: true,
    })
    async getNotifications(@CurrentUser() user: UserDto): Promise<NotificationDto[]> {
        const notifications = await this.notificationService.getNotifications(user.id);
        return notifications;
    }

    @Get('/read')
    @ApiOperation({ summary: 'Update all notification to read' })
    @ApiOkResponse({
        description: 'All notifications updated to read',
        type: RegisterResponse,
    })
    async updateAllNotificationsToRead(@CurrentUser() user: UserDto): Promise<NotificationDto[]> {
        const notifications = await this.notificationService.updateAllNotificationsToRead(user.id);
        return notifications;
    }
}