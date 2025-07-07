# Phase 1: Project Setup and Foundation

## Objectives
- Initialize Node.js project with TypeScript and NestJS
- Configure build tools and development environment
- Establish project structure following NestJS best practices
- Setup basic dependency management and scripts

## Prerequisites
- Node.js 20+ installed
- npm or yarn package manager
- Git repository initialized
- IDE/Editor with TypeScript support

## Tasks

### Task 1.1: Initialize Node.js Project
**Duration**: 30 minutes  
**Description**: Create the basic Node.js project structure

```bash
# Navigate to project directory
cd integration-telegram-app-bot-ts

# Initialize npm project
npm init -y

# Install NestJS CLI globally
npm install -g @nestjs/cli
```

**Deliverables**:
- `package.json` with basic project metadata
- NestJS CLI available globally

### Task 1.2: Install Core Dependencies
**Duration**: 45 minutes  
**Description**: Install all required dependencies for the project

```bash
# Install NestJS core dependencies
npm install @nestjs/common @nestjs/core @nestjs/platform-express
npm install @nestjs/config @nestjs/axios
npm install reflect-metadata rxjs

# Install TypeScript and development dependencies
npm install -D typescript @nestjs/cli @nestjs/schematics
npm install -D @types/node @types/express
npm install -D ts-node ts-loader tsconfig-paths

# Install application-specific dependencies
npm install class-validator class-transformer
npm install axios uuid @noble/ed25519
npm install -D @types/uuid

# Install testing dependencies
npm install -D @nestjs/testing jest ts-jest
npm install -D @types/jest @types/supertest supertest
```

**Deliverables**:
- Complete `package.json` with all dependencies
- `node_modules` directory with installed packages
- Dependency versions matching blueprint specifications

### Task 1.3: Configure TypeScript
**Duration**: 30 minutes  
**Description**: Setup TypeScript configuration for NestJS

Create `tsconfig.json`:
```json
{
  "compilerOptions": {
    "module": "commonjs",
    "declaration": true,
    "removeComments": true,
    "emitDecoratorMetadata": true,
    "experimentalDecorators": true,
    "allowSyntheticDefaultImports": true,
    "target": "ES2021",
    "sourceMap": true,
    "outDir": "./dist",
    "baseUrl": "./",
    "incremental": true,
    "skipLibCheck": true,
    "strictNullChecks": false,
    "noImplicitAny": false,
    "strictBindCallApply": false,
    "forceConsistentCasingInFileNames": false,
    "noFallthroughCasesInSwitch": false,
    "resolveJsonModule": true
  }
}
```

**Deliverables**:
- `tsconfig.json` with proper NestJS configuration
- TypeScript compiler configured for ES2021 target

### Task 1.4: Configure NestJS CLI
**Duration**: 15 minutes  
**Description**: Setup NestJS CLI configuration

Create `nest-cli.json`:
```json
{
  "$schema": "https://json.schemastore.org/nest-cli",
  "collection": "@nestjs/schematics",
  "sourceRoot": "src",
  "compilerOptions": {
    "deleteOutDir": true
  }
}
```

**Deliverables**:
- `nest-cli.json` with proper source root configuration
- NestJS schematics ready for code generation

### Task 1.5: Create Project Structure
**Duration**: 45 minutes  
**Description**: Establish the complete project directory structure

```bash
mkdir -p src/{config,interfaces,services,controllers,dto}
mkdir -p config
mkdir -p docker
mkdir -p test
```

Create directory structure:
```
integration-telegram-app-bot-ts/
├── src/
│   ├── config/
│   ├── interfaces/
│   ├── services/
│   ├── controllers/
│   ├── dto/
│   ├── app.module.ts (placeholder)
│   └── main.ts (placeholder)
├── config/
├── docker/
├── test/
├── docs/ (already exists)
├── package.json
├── tsconfig.json
├── nest-cli.json
└── README.md
```

**Deliverables**:
- Complete directory structure
- Placeholder files for main application entry points

### Task 1.6: Configure Package Scripts
**Duration**: 30 minutes  
**Description**: Setup npm scripts for development and build processes

Update `package.json` scripts section:
```json
{
  "scripts": {
    "build": "nest build",
    "format": "prettier --write \"src/**/*.ts\" \"test/**/*.ts\"",
    "start": "nest start",
    "start:dev": "nest start --watch",
    "start:debug": "nest start --debug --watch",
    "start:prod": "node dist/main",
    "lint": "eslint \"{src,apps,libs,test}/**/*.ts\" --fix",
    "test": "jest",
    "test:watch": "jest --watch",
    "test:cov": "jest --coverage",
    "test:debug": "node --inspect-brk -r tsconfig-paths/register -r ts-node/register node_modules/.bin/jest --runInBand",
    "test:e2e": "jest --config ./test/jest-e2e.json"
  }
}
```

**Deliverables**:
- Complete npm scripts for all development workflows
- Build, test, and development scripts configured

### Task 1.7: Create Environment Configuration Template
**Duration**: 30 minutes  
**Description**: Setup environment variable template

Create `.env.example`:
```bash
# Node.js Configuration
NODE_ENV=development
PORT=8080

# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=your:bot_token
TELEGRAM_WEBHOOK_URL=https://your-domain.com/telegram/webhook
TELEGRAM_WEBHOOK_TOKEN=your-webhook-token
TELEGRAM_WEBHOOK_MAX_CONNECTIONS=80

# Event Service Configuration
EVENT_SERVICE_URL=https://ai-event.stage.cere.io
EVENT_APP_ID=2105
EVENT_DATA_SERVICE_ID=2105
EVENT_ACCOUNT_ID=0x4fcbae9ac6d9ffec5da0475285d267093cdceed74fb69ba47d7a6eaafe6eb2a9
EVENT_SIGNATURE=0x624718cb16b4ca95ebf26a39f6ba74e00ed27772464af9b745cbb80f5e1e1cb7ac40624db84da233c86086caeefe048d2901851ccb16fd6d97d2bd0e86e2630e
EVENT_ID=ad9b2467-94c4-4407-918c-cc5da18271bc
EVENT_TYPE=TELEGRAM_MESSAGE
EVENT_TIMESTAMP=2025-01-03T15:07:48.168Z
EVENT_USER_PUB_KEY=0x4fcbae9ac6d9ffec5da0475285d267093cdceed74fb69ba47d7a6eaafe6eb2a9
```

Create `.gitignore`:
```
# Dependencies
node_modules/
npm-debug.log*
yarn-debug.log*
yarn-error.log*

# Runtime data
pids
*.pid
*.seed
*.pid.lock

# Build outputs
dist/
build/

# Environment variables
.env

# IDE
.vscode/
.idea/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# Logs
logs
*.log

# Coverage
coverage/
.nyc_output/

# Temporary files
*.tmp
*.temp
```

**Deliverables**:
- `.env.example` with all required environment variables
- `.gitignore` with comprehensive exclusions
- Environment configuration template ready

### Task 1.8: Create Basic Application Files
**Duration**: 45 minutes  
**Description**: Create minimal application entry points

Create `src/main.ts`:
```typescript
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  
  const port = process.env.PORT || 8080;
  await app.listen(port);
  console.log(`Application running on port ${port}`);
}

bootstrap();
```

Create `src/app.module.ts`:
```typescript
import { Module } from '@nestjs/common';

@Module({
  imports: [],
  controllers: [],
  providers: [],
})
export class AppModule {}
```

**Deliverables**:
- Basic `main.ts` application entry point
- Minimal `app.module.ts` NestJS module
- Application can compile and start

## Validation Steps

### Validation 1.1: Build Verification
```bash
# Test TypeScript compilation
npm run build

# Expected output: dist/ directory created with compiled JavaScript
```

### Validation 1.2: Development Server Start
```bash
# Test development server
npm run start:dev

# Expected output: Application running on port 8080
# Should be able to access http://localhost:8080 (404 expected)
```

### Validation 1.3: Package Scripts Test
```bash
# Test all major scripts
npm run lint    # Should pass with no errors
npm run test    # Should run Jest (no tests yet)
npm run format  # Should format code files
```

## Deliverables Summary

✅ **Project Structure**: Complete directory structure established  
✅ **Dependencies**: All required packages installed  
✅ **Configuration**: TypeScript and NestJS properly configured  
✅ **Scripts**: Development and build workflows setup  
✅ **Environment**: Configuration template created  
✅ **Foundation**: Basic application files created and working  

## Next Phase Prerequisites

Before proceeding to [Phase 2: Core Services Infrastructure](./phase-2-core-services.md):

1. ✅ All validation steps completed successfully
2. ✅ Development server can start without errors
3. ✅ Build process generates clean dist/ output
4. ✅ All npm scripts execute without errors
5. ✅ Project structure matches blueprint specifications

## Dependencies for Next Phase

The following components from Phase 1 will be referenced in Phase 2:
- **Project Structure**: `src/` directories for services and config
- **Package Configuration**: Dependencies and scripts
- **TypeScript Setup**: Compilation and module resolution
- **Environment Template**: Configuration variable structure
- **Application Foundation**: `main.ts` and `app.module.ts` files

Phase 2 will build upon this foundation by implementing the core services infrastructure and configuration management system. 