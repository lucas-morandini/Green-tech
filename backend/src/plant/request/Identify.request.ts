import { IsArray, IsNumber, IsString, ValidateNested } from 'class-validator';
import { Type } from 'class-transformer';

class LocationDto {
  @IsNumber()
  latitude: number;

  @IsNumber()
  longitude: number;
}

export class IdentifyRequest {
  @IsArray()
  @IsString({ each: true })
  images: string[];

  @IsString()
  scan_date: string;

  @ValidateNested()
  @Type(() => LocationDto)
  location: LocationDto;
}
