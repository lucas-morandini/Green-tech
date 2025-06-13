import { GroupDto } from '../dto/group.dto';

export class FindByIdResponse {
  'group': GroupDto | null;
}
