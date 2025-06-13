import { Exclude } from "class-transformer";
import { IsBoolean, IsNotEmpty, IsNumber, IsString } from "class-validator";
import { UserDto } from "src/user/dto/user.dto";

export class NotificationDto {
    @IsNumber()
    id: number;

    @IsString()
    @IsNotEmpty()
    title: string;

    @IsString()
    @IsNotEmpty()
    content: string;

    @IsBoolean()
    read: boolean;

    @Exclude()
    created_at: Date;

    @Exclude()
    updated_at: Date;

    @IsNotEmpty()
    user: UserDto;
}
