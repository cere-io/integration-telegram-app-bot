import { Controller, Get, Res } from '@nestjs/common';
import { Response } from 'express';
import { join } from 'path';
import * as fs from 'fs';

@Controller()
export class AppController {
  @Get()
  root(@Res() res: Response) {
    // Serve the index.html file directly
    const indexPath = join(process.cwd(), 'public', 'index.html');
    if (fs.existsSync(indexPath)) {
      return res.sendFile(indexPath);
    } else {
      return res.status(404).json({ message: 'Mini App not found', error: 'Not Found', statusCode: 404 });
    }
  }
} 