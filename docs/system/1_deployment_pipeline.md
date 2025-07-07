# System: Deployment Pipeline

This document illustrates the Continuous Integration and Continuous Deployment (CI/CD) pipeline for the bot, managed via GitHub Actions.

## CI/CD Pipeline Diagram

```mermaid
graph TD
    A[Developer Pushes to Branch] --> B{Trigger GitHub Action};
    
    subgraph "GitHub Actions Workflow (.github/workflows)"
        B --> C[Checkout Code];
        C --> D[Setup Node.js 20];
        D --> E[Install Dependencies (npm ci)];
        E --> F[Run Lint & Tests (npm test)];
        F --> G[Build Docker Image (docker build)];
        G --> H[Push to AWS ECR];
    end

    H --> I[ECR Docker Registry];
    I --> J[Deployment to ECS/EKS];

    subgraph "Branching Strategy"
        K[dev branch] --> L[DEV Environment]
        M[release/* branch] --> N[STAGE Environment]
        O[master branch] --> P[PROD Environment]
    end

    A -- "dev" --> K
    A -- "release/v1.2" --> M
    A -- "master" --> O

    style J fill:#D5E8D4,stroke:#82B366
```

## Explanation of the Pipeline

1.  **Trigger**: The pipeline is automatically triggered when a developer pushes code to one of the main branches (`dev`, `release/**`, or `master`).

2.  **Checkout & Setup**: The GitHub Actions runner checks out the source code and sets up the Node.js 20 environment.

3.  **Install Dependencies**: It uses `npm ci` to install the exact dependency versions specified in `package-lock.json`, ensuring a consistent and reproducible build.

4.  **Quality Gates**: Before building, the workflow runs the linter (`npm run lint`) and the full test suite (`npm run test`). If either of these steps fails, the pipeline stops, preventing low-quality code from being deployed.

5.  **Docker Build**: A production-optimized Docker image is built using the `Dockerfile`. This process includes:
    *   Running `npm run build` to compile TypeScript to JavaScript.
    *   Creating a minimal final image with only the necessary runtime artifacts.

6.  **Push to Registry**: The newly built Docker image is tagged appropriately and pushed to the Amazon Elastic Container Registry (ECR).

7.  **Deployment**: From ECR, the image can be pulled and deployed to the target environment (e.g., Amazon ECS or EKS). This final step is typically handled by a separate deployment system that is triggered by the successful completion of the GitHub Actions workflow.

## Branching and Environment Strategy

The pipeline is configured to deploy to different environments based on the branch name:
*   Pushes to `dev` are deployed to the **Development** environment.
*   Pushes to `release/*` or `hotfix/*` branches are deployed to the **Staging** environment.
*   Pushes to `master` are deployed to the **Production** environment.

This strategy ensures that code is tested in progressively more stable environments before reaching production users.
