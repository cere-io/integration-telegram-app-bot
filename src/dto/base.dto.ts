import { IsOptional, IsString, IsNumber, IsBoolean } from 'class-validator';

export class BaseDto {
  @IsOptional()
  @IsString()
  id?: string;
}

export class ValidationResponseDto {
  @IsBoolean()
  success: boolean;

  @IsOptional()
  @IsString()
  message?: string;

  @IsOptional()
  errors?: string[];
} 