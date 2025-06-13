import { IsString } from 'class-validator';

export class SearchRequest {
  @IsString()
  search: string;
}
