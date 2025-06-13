import { UserDto } from 'src/user/dto/user.dto';
import { User } from '../../user/user.entity';

export class ResetPasswordResponse {
  user: UserDto;
}
