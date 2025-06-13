import { IsNotEmpty, IsNumber } from 'class-validator';

export class AddFriendRequest {
  @IsNumber()
  @IsNotEmpty()
  friend_id: number;
}
