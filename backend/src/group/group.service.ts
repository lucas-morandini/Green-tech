import { forwardRef, Inject, Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Group } from './group.entity';
import { GroupDto } from './dto/group.dto';
import { plainToInstance } from 'class-transformer';
import { UserDto } from 'src/user/dto/user.dto';
import { Message } from 'src/message/message.entity';
import { UserService } from 'src/user/user.service';
import { User } from 'src/user/user.entity';
import { MessageService } from 'src/message/message.service';
@Injectable()
export class GroupService {
  constructor(
    @InjectRepository(Group)
    private readonly groupRepository: Repository<Group>,
    @InjectRepository(Message)
    private readonly messageRepository: Repository<Message>,
    @Inject(forwardRef(() => UserService))
    private userService: UserService,
    private readonly messageService: MessageService,
  ) {}

  async findGroupById(
    groupId: number,
    userId: number,
  ): Promise<GroupDto | null> {
    // Récupérer le groupe avec tous ses utilisateurs et identifications
    const group = await this.groupRepository
      .createQueryBuilder('group')
      .leftJoinAndSelect('group.users', 'user')
      .leftJoinAndSelect('group.identifications', 'identification')
      .where('group.id = :groupId', { groupId })
      .getOne();

    if (!group) {
      return null;
    }

    // Vérifier que l'utilisateur fait bien partie du groupe
    if (!group.users.some((user) => user.id === userId)) {
      return null;
    }

    const groupDto = new GroupDto();
    groupDto.id = group.id;
    groupDto.name = group.name;
    groupDto.users = group.users.map((user) => {
      const userDto = plainToInstance(UserDto, user);
      userDto.identifications = [];
      return userDto;
    });
    groupDto.messages = await this.messageService.findMessagesByGroupId(
      group.id,
    );
    groupDto.identifications = group.identifications;

    return groupDto;
  }

  async getAllGroups(userId: number): Promise<GroupDto[]> {
    const groups = await this.groupRepository
      .createQueryBuilder('group')
      .leftJoinAndSelect('group.users', 'user')
      .where((qb) => {
        const subQuery = qb
          .subQuery()
          .select('group_sub.id') // ✅ On sélectionne les IDs des groupes
          .from(Group, 'group_sub')
          .leftJoin('group_sub.users', 'user_sub')
          .where('user_sub.id = :userId')
          .getQuery();
        return 'group.id IN ' + subQuery;
      })
      .setParameter('userId', userId)
      .leftJoinAndSelect('group.identifications', 'identification')
      .getMany();

    const groupDtos = await Promise.all(
      groups.map(async (group) => {
        const groupDto = new GroupDto();
        groupDto.id = group.id;
        // group.name si le group n'a que 2 utilisateurs alors le nom du groupe est le nom de l'autre utilisateur
        groupDto.name = group.name;
        if (group.name == null || group.name == '') {
          if (group.users.length === 2) {
            const otherUser = group.users.find((user) => user.id !== userId);
            groupDto.name = otherUser
              ? otherUser.name + ' ' + otherUser.first_name
              : 'Unknown';
          } else {
            // le group est "Groupe " + index du groupe qui sont + de 2 utilisateurs
            const groupsOfTwo = groups.filter((g) => g.users.length === 2);
            const index = groupsOfTwo.indexOf(group);
            groupDto.name = `Groupe ${index + 1}`;
          }
        }

        groupDto.users = group.users.map((user) => {
          const userDto = plainToInstance(UserDto, user);
          userDto.identifications = [];
          return userDto;
        });

        groupDto.messages = await this.messageService.findMessagesByGroupId(
          group.id,
        );
        groupDto.identifications = group.identifications;
        return groupDto;
      }),
    );

    return groupDtos;
  }

  async createGroup(name: string, users: UserDto[]): Promise<GroupDto> {
    const group = this.groupRepository.create({
      name,
      created_at: new Date(),
      updated_at: new Date(),
    });

    // Résolution des promesses pour obtenir des User[]
    const userEntities: User[] = await Promise.all(
      users.map(async (user) => {
        const user_db = await this.userService.findById(user.id);
        if (!user_db) {
          throw new Error(`User with id ${user.id} not found`);
        }
        return user_db;
      }),
    );

    group.users = userEntities;

    const savedGroup = await this.groupRepository.save(group);
    const groupDto = plainToInstance(GroupDto, savedGroup);
    return groupDto;
  }
}
