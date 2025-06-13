import { Injectable } from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';

@ApiBearerAuth()
@ApiTags('app')
@Injectable()
export class AppService {
  @ApiOperation({ summary: 'Get hello world message' })
  @ApiResponse({ status: 200, description: 'Hello world message' })
  @ApiResponse({ status: 403, description: 'Forbidden' })
  getHello(): string {
    return 'Hello World with Hot Reload!';
  }
}
