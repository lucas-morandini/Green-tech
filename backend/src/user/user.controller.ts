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
import { UserService } from './user.service';
import { RegisterResponse } from '../auth/response/register.response';
import { RegisterRequest } from '../auth/request/register.request';
import { UserDto } from './dto/user.dto';
import { SearchRequest } from './request/search.request';
import { AuthService } from 'src/auth/auth.service';
import { GetFriendsResponse } from './response/getfriends.response';
import { AddFriendRequest } from './request/addfriend.request';
import { AddFriendResponse } from './response/addfriend.response';
import { CurrentUser } from 'src/decorators/currentuser.decorator';
import { Public } from 'src/decorators/public.decorator';
import { plainToInstance } from 'class-transformer';
import { PlantsHistoryResponse } from './response/plants-history.response';

@ApiTags('users')
@Controller('user')
export class UserController {
  constructor(
    private readonly userService: UserService,
    private readonly authService: AuthService,
  ) {}

  @Get('search')
  @ApiOperation({ summary: 'Search global in user' })
  @ApiOkResponse({
    description: 'You have been logged in',
    type: UserDto,
    isArray: true,
  })
  @ApiNotFoundResponse({ description: 'User not found' })
  async search(@CurrentUser() user : UserDto, @Query() params: SearchRequest): Promise<UserDto[]> {
    const { search } = params;
    return await this.userService.search(search, user);
  }

  @Get('friends')
  @ApiOperation({ summary: 'Get all friends' })
  @ApiOkResponse({
    description: 'You have been logged in',
    type: UserDto,
    isArray: true,
  })
  @ApiNotFoundResponse({ description: 'User not found' })
  async getFriends(@CurrentUser() user: UserDto): Promise<GetFriendsResponse> {
    const friends = await this.userService.getFriends(user.id);
    return {
      friends,
    };
  }

  @Post('/friends/add')
  @ApiOperation({ summary: 'Add a friend' })
  @ApiOkResponse({
    description: 'Add a friend',
    type: UserDto,
  })
  @ApiNotFoundResponse({ description: 'User not found' })
  async addFriend(
    @CurrentUser() user: UserDto,
    @Body() body: AddFriendRequest,
  ): Promise<AddFriendResponse> {
    const { friend_id } = body;
    const added = await this.userService.addFriend(user.id, friend_id);
    return { added };
  }

  @Put('/fcm-token')
  @ApiOperation({ summary: 'Set FCM token for user' })
  @ApiOkResponse({
    description: 'FCM token set successfully',
    type: RegisterResponse,
  })
  @ApiNotFoundResponse({ description: 'User not found' })
  async setFcmToken(@CurrentUser() user: UserDto, @Body('fcmToken') fcmToken: string): Promise<{ updated : boolean }> {
    if (!fcmToken) {
      throw new BadRequestException('FCM token is required');
    }
    const updated = await this.userService.setFcmToken(user.id, fcmToken);
    return { updated };
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get user by id' })
  @ApiOkResponse({
    description: 'User found',
    type: UserDto,
  })
  @ApiNotFoundResponse({ description: 'User not found' })
  async getUserById(@Param('id') id: number): Promise<UserDto> {
    const user = await this.userService.findById(id);
    if (!user) {
      throw new BadRequestException('User not found');
    }
    // transform user to UserDto
    const userDto = plainToInstance(UserDto, user);
    userDto.identifications = [];
    return userDto;
  }

  @Get('plants/history')
  @ApiOperation({ summary: 'Get user plants identification history' })
  @ApiOkResponse({
    description: 'User plants history retrieved successfully',
    type: PlantsHistoryResponse,
  })
  @ApiNotFoundResponse({ description: 'No plants found in history' })
  async getPlantsHistory(
    @CurrentUser() user: UserDto,
  ): Promise<PlantsHistoryResponse> {
    const plants = await this.userService.getPlantsHistory(user.id);

    if (plants.length === 0) {
      // Retourner une réponse vide plutôt qu'une erreur 404
      return { plants: [] };
    }

    return { plants };
  }
}
