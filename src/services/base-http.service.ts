import { Injectable, Logger } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { AxiosRequestConfig, AxiosResponse } from 'axios';
import { firstValueFrom } from 'rxjs';

@Injectable()
export class BaseHttpService {
  protected readonly logger = new Logger(this.constructor.name);

  constructor(protected readonly httpService: HttpService) {}

  protected async makeRequest<T>(
    method: 'GET' | 'POST' | 'PUT' | 'DELETE',
    url: string,
    data?: any,
    config?: AxiosRequestConfig,
  ): Promise<AxiosResponse<T>> {
    try {
      this.logger.debug(`Making ${method} request to ${url}`);
      
      const requestConfig: AxiosRequestConfig = {
        method,
        url,
        data,
        ...config,
      };

      const response = await firstValueFrom(
        this.httpService.request<T>(requestConfig)
      );

      this.logger.debug(`Request successful: ${response.status}`);
      return response;
    } catch (error) {
      this.logger.error(`Request failed to ${url}:`, error.message);
      throw error;
    }
  }

  protected async get<T>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.makeRequest<T>('GET', url, undefined, config);
  }

  protected async post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.makeRequest<T>('POST', url, data, config);
  }
} 