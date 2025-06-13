import {
  BadRequestException,
  Body,
  Controller,
  Get,
  Param,
  Post,
  Query,
  Headers,
} from '@nestjs/common';
import {
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiTags,
} from '@nestjs/swagger';
import { GroupService } from './group.service';
import { CurrentUser } from 'src/decorators/currentuser.decorator';
import { UserDto } from 'src/user/dto/user.dto';
import { GroupDto } from './dto/group.dto';
import { GetAllGroupsResponse } from './response/getallgroups.response';
import { FindByIdResponse } from './response/findbyid.reponse';

@ApiTags('groups')
@Controller('group')
export class GroupController {
  constructor(private readonly groupService: GroupService) {}

  @Get('/')
  @ApiOperation({ summary: 'Get all groups' })
  @ApiOkResponse({
    description: 'All groups',
  })
  @ApiNotFoundResponse({ description: 'No groups found' })
  async getAllGroups(
    @CurrentUser() user: UserDto,
  ): Promise<GetAllGroupsResponse> {
    const groups = await this.groupService.getAllGroups(user.id);
    return { groups };
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get group by id' })
  @ApiOkResponse({
    description: 'Group found',
    type: FindByIdResponse,
  })
  @ApiNotFoundResponse({ description: 'Group not found' })
  async findGroupById(
    @Param('id') id: number,
    @CurrentUser() user: UserDto,
  ): Promise<FindByIdResponse> {
    const group = await this.groupService.findGroupById(id, user.id);
    if (!group) {
      throw new BadRequestException('Group not found');
    }
    return { group };
  }
}
