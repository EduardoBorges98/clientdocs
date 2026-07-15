# ClientDocs Processor

ClientDocs Processor is a backend API built with Java and Spring Boot for uploading, storing, and asynchronously processing client PDF documents.

The project simulates a real cloud backend flow: relational database, file storage, message queue, asynchronous processing, containerization, and deployment on AWS ECS Fargate.

---

## Project Goal

The goal of this project is to demonstrate a modern, scalable backend architecture using Java and AWS.

The application allows you to:

- register clients;
- upload PDF documents;
- store files in Amazon S3;
- save metadata in PostgreSQL;
- send events to an SQS queue;
- process documents asynchronously;
- update the document status after processing.

---

## Tech Stack

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Spring Validation
- PostgreSQL
- Flyway
- Maven
- Docker
- Swagger / OpenAPI
- AWS SDK for Java
- Amazon ECR
- Amazon ECS Fargate
- Amazon RDS PostgreSQL
- Amazon S3
- Amazon SQS
- AWS IAM
- AWS Secrets Manager
- Application Load Balancer
- GitHub Actions

---

## General Architecture

The application runs in a Docker container published to Amazon ECR and executed on Amazon ECS Fargate.

External traffic enters through the Application Load Balancer, which forwards requests to the Spring Boot application running on ECS.

```text
Internet / Postman
    ↓
Application Load Balancer :80
    ↓
ECS Fargate / Spring Boot :8080
    ↓
RDS PostgreSQL
    ↓
S3
    ↓
SQS
    ↓
Internal scheduler
    ↓
RDS updated
```

Deployment is automated with GitHub Actions:

```text
git push to main
    ↓
GitHub Actions
    ↓
Maven build
    ↓
Docker image build
    ↓
Push image to Amazon ECR
    ↓
Render new ECS Task Definition
    ↓
Deploy to Amazon ECS Fargate
```

---

## Main Flow

1. The user sends a PDF to the API.
2. The application validates the file.
3. The PDF is saved to Amazon S3.
4. A document record is created in PostgreSQL with status `PENDING`.
5. The application sends a message to an SQS queue.
6. An internal scheduler polls the queue periodically.
7. The consumer processes the message.
8. The application looks up the client by the CPF/CNPJ extracted from the file name.
9. The document is updated to `PROCESSED` or `CLIENT_NOT_FOUND`.
10. The message is removed from the SQS queue.

Initial status example:

```json
{
  "id": 4,
  "status": "PENDING",
  "clientId": null,
  "processedAt": null
}
```

Example after processing:

```json
{
  "id": 4,
  "status": "PROCESSED",
  "clientId": 1,
  "processedAt": "2026-07-13T23:22:01.04786"
}
```

---

## Main Entities

### Client

Represents a client registered in the database.

Main fields:

- `id`
- `name`
- `cpfCnpj`
- `email`
- `active`
- `createdAt`
- `updatedAt`

### Document

Represents a document submitted for processing.

Main fields:

- `id`
- `fileName`
- `cpfCnpjExtracted`
- `status`
- `client`
- `bucketName`
- `s3Key`
- `contentType`
- `fileSize`
- `createdAt`
- `processedAt`

### DocumentStatus

Possible document statuses:

```text
PENDING
PROCESSING
PROCESSED
CLIENT_NOT_FOUND
ERROR
```

---

## Main Endpoints

### API information

```http
GET /
```

Example:

```http
GET http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/
```

Expected response:

```json
{
  "service": "ClientDocs Processor",
  "status": "running",
  "version": "1.0.0",
  "health": "/health",
  "docs": "/swagger-ui/index.html"
}
```

### Health check

```http
GET /health
```

Example:

```http
GET http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/health
```

Expected response:

```json
{
  "status": "UP",
  "service": "clientdocs-api"
}
```

---

### Create client

```http
POST /clients
```

Example body:

```json
{
  "name": "Test Client",
  "cpfCnpj": "12345678000199",
  "email": "client@test.com"
}
```

---

### List clients

```http
GET /clients
```

---

### Find client by ID

```http
GET /clients/{id}
```

---

### Document upload

```http
POST /documents/upload
```

Request type:

```text
multipart/form-data
```

Expected field:

```text
file
```

Example using the Load Balancer URL:

```http
POST http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/documents/upload
```

Example initial response:

```json
{
  "id": 4,
  "fileName": "Contrato Cliente 12345678000199 Final.pdf",
  "cpfCnpjExtracted": "12345678000199",
  "status": "PENDING",
  "clientId": null,
  "bucketName": "clientdocs-eduardo-dev",
  "s3Key": "documents/2026/07/13720c7d-8353-4232-93e9-a07f76b89100-contrato_cliente_12345678000199_final.pdf",
  "contentType": "application/pdf",
  "fileSize": 3294,
  "processedAt": null
}
```

---

### Find document

```http
GET /documents/{id}
```

Example:

```http
GET http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/documents/4
```

Example response after processing:

```json
{
  "id": 4,
  "fileName": "Contrato Cliente 12345678000199 Final.pdf",
  "cpfCnpjExtracted": "12345678000199",
  "status": "PROCESSED",
  "clientId": 1,
  "bucketName": "clientdocs-eduardo-dev",
  "s3Key": "documents/2026/07/13720c7d-8353-4232-93e9-a07f76b89100-contrato_cliente_12345678000199_final.pdf",
  "contentType": "application/pdf",
  "fileSize": 3294,
  "processedAt": "2026-07-13T23:22:01.04786"
}
```

---

### List documents

```http
GET /documents
```

---

### Download document

```http
GET /documents/{id}/download
```

---

### Manually process one message

```http
POST /queue/process-one
```

This endpoint can be used to manually test the consumption of a message from the queue.

---

## Swagger / OpenAPI

While the application is running, Swagger documentation is available at:

```http
GET /swagger-ui/index.html
```

Local example:

```http
http://localhost:8080/swagger-ui/index.html
```

---

## Package Structure

Conceptual project structure:

```text
com.eduardo.clientdocs
    ├── config
    ├── controller
    ├── dto
    ├── entity
    ├── exception
    ├── queue
    ├── repository
    ├── service
    └── storage
```

Main responsibilities:

- `controller`: REST endpoints;
- `dto`: API input/output objects;
- `entity`: JPA entities;
- `repository`: Spring Data JPA interfaces;
- `service`: business rules;
- `storage`: local or S3 storage;
- `queue`: SQS integration;
- `config`: application configuration;
- `exception`: global error handling.

---

## Application Profiles

The application uses profiles to separate environments.

### local

Used for local development with a local PostgreSQL instance or Docker Compose.

```text
SPRING_PROFILES_ACTIVE=local
```

### aws

Used for local testing while consuming real AWS services, such as S3 and SQS.

```text
SPRING_PROFILES_ACTIVE=aws
```

### rds

Used to test the application locally while connecting to RDS.

```text
SPRING_PROFILES_ACTIVE=rds
```

### prod

Used on ECS Fargate.

```text
SPRING_PROFILES_ACTIVE=prod
```

---

## Database and Migrations

The project uses PostgreSQL as its relational database and Flyway for schema versioning.

Main migrations:

```text
V1__create_initial_tables.sql
V2__add_storage_fields_to_documents.sql
```

The migrations create and evolve the tables:

- `clients`
- `documents`

---

## AWS Deployment

The deployment was done using the following services:

- Amazon ECR
- Amazon ECS Fargate
- Amazon RDS PostgreSQL
- Amazon S3
- Amazon SQS
- AWS IAM
- AWS Secrets Manager
- Application Load Balancer
- GitHub Actions

---

## CI/CD Pipeline

The project includes a CI/CD pipeline configured with GitHub Actions.

The workflow is triggered automatically on every push to the `main` branch.

Pipeline flow:

```text
git push
    ↓
GitHub Actions
    ↓
Build application with Maven
    ↓
Build Docker image
    ↓
Push image to Amazon ECR
    ↓
Render a new ECS Task Definition
    ↓
Deploy automatically to Amazon ECS Fargate
```

Main steps executed by the workflow:

- checkout of the repository;
- Java 21 setup;
- Maven build with tests skipped for deployment packaging;
- AWS authentication using GitHub Actions secrets;
- login to Amazon ECR;
- Docker image build;
- Docker image push to ECR;
- download of the current ECS Task Definition;
- rendering of a new ECS Task Definition with the new image;
- deployment to the ECS Service.

The Docker image is tagged using the commit hash (`GITHUB_SHA`), which makes each deployment traceable to a specific Git commit.

GitHub Actions workflow file:

```text
.github/workflows/deploy.yml
```

GitHub repository secrets used by the workflow:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
AWS_ACCOUNT_ID
ECR_REPOSITORY
ECS_CLUSTER
ECS_SERVICE
ECS_TASK_DEFINITION
CONTAINER_NAME
```

Validated CI/CD flow:

```text
GitHub Actions
    ↓
Amazon ECR
    ↓
Amazon ECS Fargate
    ↓
Application Load Balancer
```

---

## Amazon ECR

The application's Docker image was published to Amazon ECR.

Repository:

```text
clientdocs-api
```

Image URI format used in deployments:

```text
<account-id>.dkr.ecr.sa-east-1.amazonaws.com/clientdocs-api:<image-tag>
```

Manual deployments used explicit tags such as `v2` and `v3`. The CI/CD pipeline tags images with the commit hash (`GITHUB_SHA`).

Flow used:

```text
Spring Boot project
    ↓
Docker build
    ↓
Docker tag
    ↓
Docker push
    ↓
Amazon ECR
```

Commands used:

```bash
mvn clean package -DskipTests

docker build -t clientdocs-api .

docker tag clientdocs-api:latest <account-id>.dkr.ecr.sa-east-1.amazonaws.com/clientdocs-api:v2

aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.sa-east-1.amazonaws.com

docker push <account-id>.dkr.ecr.sa-east-1.amazonaws.com/clientdocs-api:v2
```

---

## Amazon ECS Fargate

The application runs on ECS Fargate, with no need to manually provision an EC2 instance.

Cluster:

```text
clientdocs-cluster-dev
```

Service:

```text
clientdocs-api-service
```

Task Definition:

```text
clientdocs-api-task
```

Validated revisions include manual revisions and later revisions generated through CI/CD and infrastructure updates.

```text
clientdocs-api-task:4
clientdocs-api-task:5
clientdocs-api-task:9
```

New revisions are also generated automatically by the GitHub Actions deployment pipeline.

Container port:

```text
8080
```

The ECS Service keeps one task running with the Spring Boot application.

---

## Amazon RDS PostgreSQL

The application's database runs on Amazon RDS PostgreSQL.

DB identifier:

```text
clientdocs-postgres-dev
```

Endpoint:

```text
clientdocs-postgres-dev.clieq46eogak.sa-east-1.rds.amazonaws.com
```

Port:

```text
5432
```

Database name:

```text
clientdocs
```

The application uses RDS to store:

- clients;
- documents;
- processing status;
- file metadata.

Migrations are executed with Flyway.

---

## Amazon S3

PDFs sent to the API are stored in Amazon S3.

Bucket:

```text
clientdocs-eduardo-dev
```

Example generated key:

```text
documents/2026/07/13720c7d-8353-4232-93e9-a07f76b89100-contrato_cliente_12345678000199_final.pdf
```

The application only saves file metadata in the database, such as:

- bucket;
- S3 key;
- original name;
- content type;
- file size.

---

## Amazon SQS

The application uses SQS for asynchronous document processing.

Queue:

```text
clientdocs-document-processing-dev
```

Queue URL:

```text
https://sqs.sa-east-1.amazonaws.com/<account-id>/clientdocs-document-processing-dev
```

After the upload, the API sends a message to the queue containing the data needed to process the document.

Conceptual message example:

```json
{
  "documentId": 4,
  "bucketName": "clientdocs-eduardo-dev",
  "storageKey": "documents/2026/07/arquivo.pdf",
  "cpfCnpjExtracted": "12345678000199"
}
```

---

## Internal Scheduler

The application has a scheduler using `@Scheduled`.

It periodically reads from the SQS queue.

Responsibilities:

- polling the SQS queue;
- receiving messages;
- processing pending documents;
- updating status in RDS;
- deleting processed messages from the queue.

Flow:

```text
Scheduler
    ↓
SQS ReceiveMessage
    ↓
DocumentProcessingService
    ↓
RDS update
    ↓
SQS DeleteMessage
```

---

## IAM Task Role

Initially, the task used credentials via environment variables:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

The project was later adjusted to use an IAM Task Role, which is the correct model for applications running on ECS.

Role created:

```text
clientdocs-ecs-task-role
```

Policy created:

```text
ClientDocsEcsTaskPolicy
```

Permissions granted:

- access to the `clientdocs-eduardo-dev` S3 bucket;
- sending and consuming messages on the `clientdocs-document-processing-dev` SQS queue.

With this, access keys were removed from the Task Definition.

The application now accesses S3 and SQS using the ECS task's own permissions.

Current permission architecture:

```text
ECS Task
    ↓ assume role
clientdocs-ecs-task-role
    ↓
S3 + SQS
```

---

## AWS Secrets Manager

The RDS database password is stored in AWS Secrets Manager instead of being exposed as a plain text environment variable in the ECS Task Definition.

Secret used:

```text
clientdocs/rds/database-password
```

The ECS Task Definition injects the secret into the container as the same environment variable expected by the Spring Boot application:

```text
DATABASE_PASSWORD
```

The application code did not need to change. Spring Boot continues to read `DATABASE_PASSWORD`, but ECS retrieves the value securely from Secrets Manager before starting the container.

Secret injection flow:

```text
AWS Secrets Manager
    ↓
ecsTaskExecutionRole
    ↓
ECS Task Definition ValueFrom
    ↓
DATABASE_PASSWORD environment variable
    ↓
Spring Boot connects to RDS
```

Important implementation detail: because the secret was created as a key/value secret with the key `DATABASE_PASSWORD`, the ECS `ValueFrom` reference uses the JSON key suffix:

```text
arn:aws:secretsmanager:sa-east-1:<account-id>:secret:clientdocs/rds/database-password-xxxxxx:DATABASE_PASSWORD::
```

The permission required to retrieve the secret is attached to the ECS Task Execution Role:

```text
secretsmanager:GetSecretValue
```

This permission is required on `ecsTaskExecutionRole` because ECS retrieves and injects the secret before the application container starts.

---

## Application Load Balancer

An Application Load Balancer was created to expose the API more reliably.

Load Balancer:

```text
clientdocs-alb
```

DNS:

```text
clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com
```

Listener:

```text
HTTP : 80
```

Target Group:

```text
clientdocs-api-tg
```

Target Group protocol:

```text
HTTP
```

Target Group port:

```text
8080
```

Health check path:

```text
/health
```

Flow with the Load Balancer:

```text
Internet
    ↓ HTTP :80
Application Load Balancer
    ↓ HTTP :8080
ECS Fargate / Spring Boot
```

Before, the application was accessed directly through the ECS task's public IP:

```text
http://TASK_PUBLIC_IP:8080
```

After the ALB, it became accessible via:

```text
http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com
```

---

## Security Groups

Separate security groups were configured for the ALB, ECS, and RDS.

### ALB Security Group

Name:

```text
clientdocs-alb-sg
```

Inbound:

```text
HTTP 80 from 0.0.0.0/0
```

Responsibility:

```text
Allow public HTTP access to the Load Balancer
```

### ECS Security Group

Name:

```text
clientdocs-ecs-sg
```

Final inbound:

```text
Custom TCP 8080 from clientdocs-alb-sg
```

The direct public rule was removed:

```text
Custom TCP 8080 from 0.0.0.0/0
```

Responsibility:

```text
Allow access to the application only through the Load Balancer
```

### RDS Security Group

Name:

```text
clientdocs-rds-sg
```

Inbound:

```text
PostgreSQL 5432 from clientdocs-ecs-sg
```

Responsibility:

```text
Allow only the application running on ECS to access the database
```

---

## Production Environment Variables

The Task Definition uses environment variables to configure the application.

Example:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://clientdocs-postgres-dev.clieq46eogak.sa-east-1.rds.amazonaws.com:5432/clientdocs
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=********
AWS_REGION=sa-east-1
AWS_S3_BUCKET_NAME=clientdocs-eduardo-dev
AWS_SQS_DOCUMENT_QUEUE_URL=https://sqs.sa-east-1.amazonaws.com/<account-id>/clientdocs-document-processing-dev
AWS_SQS_POLLING_INTERVAL_MS=10000
```

Note:

```text
AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY are no longer used by the task.
Access to S3 and SQS is done via the IAM Task Role.
DATABASE_PASSWORD is not stored as plain text in the Task Definition.
It is injected from AWS Secrets Manager using ValueFrom.
```

---

## Cloud Flow Validation

The full flow was validated on AWS.

Test performed:

```http
POST http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/documents/upload
```

Initial result:

```json
{
  "id": 4,
  "status": "PENDING",
  "clientId": null,
  "processedAt": null
}
```

Follow-up query:

```http
GET http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/documents/4
```

Processed result:

```json
{
  "id": 4,
  "status": "PROCESSED",
  "clientId": 1,
  "processedAt": "2026-07-13T23:22:01.04786"
}
```

This confirms that the following components are working together:

```text
Application Load Balancer
ECS Fargate
Spring Boot
RDS PostgreSQL
S3
SQS
IAM Task Role
AWS Secrets Manager
Internal scheduler
GitHub Actions CI/CD
```

---

## Running Locally

Start local dependencies:

```bash
docker compose up -d
```

Run the application with the local profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Local health check:

```http
GET http://localhost:8080/health
```

---

## Testing Locally

Create a client:

```http
POST http://localhost:8080/clients
```

Upload a document:

```http
POST http://localhost:8080/documents/upload
```

Query a document:

```http
GET http://localhost:8080/documents/{id}
```

Swagger:

```http
http://localhost:8080/swagger-ui/index.html
```

---

## Making a New Deployment

The preferred deployment flow is now automated through GitHub Actions.

After changing the code:

```bash
git add .
git commit -m "Describe the change"
git push
```

A push to the `main` branch automatically triggers the pipeline:

```text
GitHub Actions
    ↓
Maven build
    ↓
Docker build
    ↓
Push image to ECR
    ↓
New ECS Task Definition
    ↓
Deploy to ECS Service
```

After the workflow finishes successfully, validate the deployed application:

```bash
curl http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/
curl http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/health
```

The manual deployment flow is still possible if needed:

```bash
mvn clean package -DskipTests

docker build -t clientdocs-api .

docker tag clientdocs-api:latest <account-id>.dkr.ecr.sa-east-1.amazonaws.com/clientdocs-api:v3

aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.sa-east-1.amazonaws.com

docker push <account-id>.dkr.ecr.sa-east-1.amazonaws.com/clientdocs-api:v3
```

Then:

1. Create a new Task Definition revision.
2. Update the image to the new tag.
3. Keep the same Task Role.
4. Update the ECS Service.
5. Force a new deployment.
6. Test `/health`.
7. Test a document upload.

---

## Costs and Shutdown

To reduce costs when not actively using the project:

- set the ECS Service to `desired tasks = 0`;
- temporarily stop the RDS instance;
- keep S3 and SQS running — they tend to have low cost at this testing volume.

Warning: the Application Load Balancer and RDS can generate charges even with light usage.

---

## Amazon SQS Dead Letter Queue

The document processing queue is configured with a Dead Letter Queue to isolate messages that fail multiple processing attempts.

Main queue:

```text
clientdocs-document-processing-dev
```

Dead Letter Queue:

```text
clientdocs-document-processing-dlq-dev
```

The main queue uses a redrive policy with:

```text
maxReceiveCount = 3
```

This means that if a message is received multiple times and is not deleted by the application after processing, Amazon SQS automatically moves it to the DLQ.

Failure flow:

```text
Message sent to main queue
    ↓
Application receives the message
    ↓
Application tries to process it
    ↓
If processing succeeds, the message is deleted
    ↓
If processing fails, the message is not deleted
    ↓
SQS retries delivery
    ↓
After the receive limit is reached
    ↓
Message is moved to the DLQ
```

The consumer logs the SQS receive count to make retries easier to observe:

```text
receiveCount=1
receiveCount=2
receiveCount=3
```

The DLQ was validated by sending an invalid message to the main queue:

```json
{
  "invalid": true
}
```

The invalid message was retried by the application and then moved to:

```text
clientdocs-document-processing-dlq-dev
```

This confirms that failed messages are isolated instead of being retried forever or silently lost.

---

## CloudWatch Alarm for DLQ

A CloudWatch alarm was created to monitor the Dead Letter Queue.

Alarm name:

```text
clientdocs-dlq-has-messages-dev
```

Metric:

```text
ApproximateNumberOfMessagesVisible
```

Queue monitored:

```text
clientdocs-document-processing-dlq-dev
```

Condition:

```text
ApproximateNumberOfMessagesVisible >= 1
for 1 datapoint within 1 minute
```

Purpose:

```text
Trigger an alert whenever one or more messages are available in the DLQ.
```

Alarm flow:

```text
Message fails processing
    ↓
Message is moved to the DLQ
    ↓
CloudWatch detects ApproximateNumberOfMessagesVisible >= 1
    ↓
Alarm changes to In alarm
    ↓
SNS sends an email notification
```

The alarm was validated by sending an invalid message to the main queue and confirming that:

```text
1. the message was moved to the DLQ;
2. the CloudWatch alarm changed to In alarm;
3. an email notification was received through SNS.
```

After the test, the message can be deleted from the DLQ and the alarm returns to `OK`.

---

## Future Improvements

- Configure HTTPS on the Application Load Balancer
- Add a custom domain
- Add automated tests
- Add deeper health checks for database, storage, and queue
- Extract the worker into its own dedicated service
- Add a CloudWatch dashboard
- Add AWS Lambda for event-driven processing
- Add Kubernetes manifests for local deployment with Minikube
- Implement authentication/authorization
- Build a frontend for document upload and lookup

---

## Project Status

Cloud flow successfully validated:

```text
PDF upload
    ↓
S3
    ↓
RDS PENDING
    ↓
SQS
    ↓
Scheduler
    ↓
RDS PROCESSED
```

Validated production infrastructure:

```text
Spring Boot
Docker
Amazon ECR
Amazon ECS Fargate
Amazon RDS PostgreSQL
Amazon S3
Amazon SQS
SQS Dead Letter Queue
AWS IAM Task Role
AWS Secrets Manager
Amazon CloudWatch Alarm
Amazon SNS email notification
Application Load Balancer
Security Groups
GitHub Actions CI/CD
```

Validated resilience and monitoring improvements:

```text
Dead Letter Queue configured for failed SQS messages
SQS receiveCount logs added to the consumer
Invalid message tested and moved to the DLQ
CloudWatch alarm triggered when DLQ had messages
SNS email notification received successfully
```

Latest validated security improvement:

```text
DATABASE_PASSWORD removed from plain environment variables
DATABASE_PASSWORD injected from AWS Secrets Manager
ecsTaskExecutionRole allowed to call secretsmanager:GetSecretValue
Application successfully connected to RDS after secret injection