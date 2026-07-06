# ClientDocs Processor

ClientDocs Processor is a backend project built with Java and Spring Boot for processing client documents.

The application allows client registration, mock document processing, CPF/CNPJ extraction from file names, document status tracking, and database persistence using PostgreSQL.

This project is being developed as a backend/cloud portfolio project and will later evolve to use AWS services such as S3, SQS, SNS, Lambda, CloudWatch and ECS.

---

## Current Features

- Register clients
- List clients
- Find client by ID
- Process mock document files
- Extract CPF/CNPJ from document file name
- Match document with existing client
- Mark document as processed or client not found
- Store document metadata
- Global exception handling
- Database migrations with Flyway
- PostgreSQL running with Docker Compose

---

## Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- Docker Compose
- Maven
- Jakarta Validation

---

## Architecture - Current Local Version

```text
Postman / HTTP Client
        |
        v
Spring Boot REST API
        |
        v
Service Layer
        |
        v
Spring Data JPA Repositories
        |
        v
PostgreSQL running on Docker
```

---

## Future AWS Architecture

```text
User uploads PDF
        |
        v
Amazon S3
        |
        v
Amazon SQS
        |
        v
Java Worker / ECS
        |
        v
Amazon RDS PostgreSQL
        |
        v
SNS / Lambda alerts
        |
        v
CloudWatch logs and metrics
```

---

## Running Locally

### 1. Start PostgreSQL

```bash
docker compose up -d
```

### 2. Run the application

On Windows:

```bash
mvnw.cmd spring-boot:run
```

On Linux/macOS:

```bash
./mvnw spring-boot:run
```

The API will run on:

```text
http://localhost:8080
```

---

## Health Check

```http
GET /health
```

Example:

```http
GET http://localhost:8080/health
```

Expected response:

```text
ClientDocs API is running
```

---

## Client Endpoints

### Create Client

```http
POST /clients
```

Example request:

```json
{
  "name": "Empresa Borges LTDA",
  "cpfCnpj": "12345678000199",
  "email": "contato@empresaborges.com"
}
```

Example response:

```json
{
  "id": 1,
  "name": "Empresa Borges LTDA",
  "cpfCnpj": "12345678000199",
  "email": "contato@empresaborges.com",
  "active": true,
  "createdAt": "2026-07-06T19:40:00",
  "updatedAt": null
}
```

### List Clients

```http
GET /clients
```

### Find Client by ID

```http
GET /clients/{id}
```

Example:

```http
GET /clients/1
```

---

## Document Endpoints

### Process Mock Document

```http
POST /documents/process-mock
```

Example request:

```json
{
  "fileName": "contrato_12345678000199.pdf"
}
```

If the client exists, the document status will be:

```text
PROCESSED
```

Example response:

```json
{
  "id": 1,
  "fileName": "contrato_12345678000199.pdf",
  "cpfCnpjExtracted": "12345678000199",
  "status": "PROCESSED",
  "clientId": 1,
  "bucketName": null,
  "s3Key": null,
  "contentType": null,
  "fileSize": null,
  "createdAt": "2026-07-06T19:40:00",
  "processedAt": "2026-07-06T19:40:00"
}
```

If the client does not exist, the document status will be:

```text
CLIENT_NOT_FOUND
```

### List Documents

```http
GET /documents
```

### Find Document by ID

```http
GET /documents/{id}
```

---

## Document Statuses

| Status | Description |
|---|---|
| PENDING | Document was created but not processed yet |
| PROCESSING | Document is being processed |
| PROCESSED | Client was found and document was processed |
| CLIENT_NOT_FOUND | CPF/CNPJ was extracted but no client was found |
| ERROR | Unexpected error during processing |

---

## Database Migrations

The project uses Flyway to manage database schema changes.

Current migrations:

```text
V1__create_initial_tables.sql
V2__add_storage_fields_to_documents.sql
```

Important rule:

```text
Do not edit a migration after it has already been applied.
Create a new V3, V4, etc. for future changes.
```


---

## API Documentation

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Application Profiles

The project uses Spring profiles to separate local and production configurations.

Current profiles:

```text
local -> PostgreSQL running on Docker
prod  -> external PostgreSQL database using environment variables
Default active profile -> local

For production, the following environment variables are expected:

DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
```

## Next Steps

- Add real PDF upload
- Integrate with Amazon S3
- Add SQS for asynchronous processing
- Create a Java worker service
- Add SNS/Lambda alerts
- Add CloudWatch logs and metrics
- Add automated tests
- Add Swagger/OpenAPI documentation