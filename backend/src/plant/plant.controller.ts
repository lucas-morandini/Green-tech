import {
  BadRequestException,
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  Post,
  Put,
  Query,
  Req,
} from '@nestjs/common';
import { PlantService } from './plant.service';
import { PlantDto } from './dto/plant.dto';
import { FindOneDto } from './dto/findone.dto';
import {
  ApiTags,
  ApiOperation,
  ApiParam,
  ApiBody,
  ApiCreatedResponse,
  ApiAcceptedResponse,
  ApiOkResponse,
  ApiNotFoundResponse,
  getSchemaPath,
} from '@nestjs/swagger';
import { IdentifyResponse } from './response/Identify.response';
import { IdentifyRequest } from './request/Identify.request';
import { SearchRequest } from './request/search.request';
import { Public } from 'src/decorators/public.decorator';
import { CurrentUser } from 'src/decorators/currentuser.decorator';
import { User } from 'src/user/user.entity';

@ApiTags('plants')
@Controller('plant')
export class PlantController {
  constructor(private readonly plantService: PlantService) {}

  @Post('/identify')
  @ApiOperation({ summary: 'Identify a plant by its scientific name' })
  @ApiOkResponse({
    description: 'The plant has been identified',
    type: IdentifyResponse,
  })
  @ApiNotFoundResponse({ description: 'Species not found' })
  @Public()
  async identify(@Body() body: IdentifyRequest, @CurrentUser() user: User) {
    const { images, scan_date, location } = body;

    const userId = user?.id;
    console.log(`Identifying plant for user ID: ${userId || 'Anonymous'}`);
    return this.plantService.identify(images, scan_date, location, userId);
  }

  @Get('search')
  @ApiOperation({ summary: 'Global search for plants' })
  @ApiParam({
    name: 'search',
    description: 'Search term',
    required: true,
    schema: { type: 'string' },
    example: 'Clover',
  })
  @ApiOkResponse({
    description: 'List of plants matching the search term',
    type: PlantDto,
    isArray: true,
  })
  @ApiNotFoundResponse({ description: 'No plants found' })
  async search(@Query() params: SearchRequest): Promise<PlantDto[]> {
    const { search } = params;
    return this.plantService.search(search);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get a plant by ID' })
  @ApiParam({
    name: 'id',
    description: 'Plant ID',
    required: true,
    schema: { type: 'integer' },
    example: 1,
  })
  @ApiOkResponse({
    description: 'The plant has been found',
    type: PlantDto,
    schema: {
      example: {
        id: 1,
        scientific_name: 'Trifolium pratense',
        common_name: 'Red Clover',
        family: 'Fabaceae',
        description: 'Perennial herb with red-purple flowers',
        tela_botanica_url: 'https://www.tela-botanica.org/bdtfx-nn-69412',
        nitrogen_fixation_score: 0.8,
        soil_structure_score: 0.7,
        water_retention_score: 0.6,
        ecological_details: 'Good for pollinators and fixing nitrogen',
      },
    },
  })
  @ApiNotFoundResponse({ description: 'Plant not found' })
  @Public()
  findOne(@Param('id') id: number) {
    return this.plantService.findOne(id);
  }

  @Get()
  @ApiOperation({ summary: 'Get all plants' })
  @ApiOkResponse({
    description: 'List of all plants',
    type: [PlantDto],
    schema: {
      type: 'array',
      items: { $ref: getSchemaPath(PlantDto) },
    },
  })
  @Public()
  findAll() {
    return this.plantService.findAll();
  }

  @Post()
  @HttpCode(201)
  @ApiOperation({ summary: 'Create a new plant' })
  @ApiBody({
    type: PlantDto,
    description: 'Plant data',
    examples: {
      example1: {
        summary: 'Basic plant example',
        value: {
          scientific_name: 'Medicago sativa',
          common_name: 'Alfalfa',
          family: 'Fabaceae',
          description: 'Perennial flowering plant with trifoliate leaves',
          tela_botanica_url: 'https://www.tela-botanica.org/bdtfx-nn-40551',
          nitrogen_fixation_score: 0.9,
          soil_structure_score: 0.8,
          water_retention_score: 0.7,
          ecological_details: 'Deep roots help prevent soil erosion',
        },
      },
    },
  })
  @ApiCreatedResponse({
    description: 'The plant has been created',
    type: PlantDto,
  })
  async create(@Body() plantDto: PlantDto) {
    return this.plantService.create(plantDto);
  }

  @Put(':id')
  @HttpCode(202)
  @ApiOperation({ summary: 'Update a plant' })
  @ApiParam({
    name: 'id',
    description: 'Plant ID',
    required: true,
    schema: { type: 'integer' },
    example: 1,
  })
  @ApiBody({
    type: PlantDto,
    description: 'Updated plant data',
    examples: {
      example1: {
        summary: 'Update example',
        value: {
          scientific_name: 'Medicago sativa',
          common_name: 'Alfalfa',
          family: 'Fabaceae',
          description: 'Updated description',
          tela_botanica_url: 'https://www.tela-botanica.org/bdtfx-nn-40551',
          nitrogen_fixation_score: 0.9,
          soil_structure_score: 0.8,
          water_retention_score: 0.7,
          ecological_details: 'Updated ecological details',
        },
      },
    },
  })
  @ApiAcceptedResponse({
    description: 'The plant has been updated',
    type: PlantDto,
  })
  @ApiNotFoundResponse({ description: 'Plant not found' })
  async update(@Param('id') id: number, @Body() plantDto: PlantDto) {
    return this.plantService.update(id, plantDto);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Delete a plant' })
  @ApiParam({
    name: 'id',
    description: 'Plant ID',
    required: true,
    schema: { type: 'integer' },
    example: 1,
  })
  @ApiOkResponse({ description: 'The plant has been deleted' })
  @ApiNotFoundResponse({ description: 'Plant not found' })
  deleteOne(@Param('id') id: number) {
    return this.plantService.deleteOne(id);
  }
}
