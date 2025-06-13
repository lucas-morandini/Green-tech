import { UserDto } from 'src/user/dto/user.dto';
import { User } from '../../user/user.entity';

export class RegisterResponse {
  user: UserDto;
}
