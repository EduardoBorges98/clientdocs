# ClientDocs Processor

ClientDocs Processor is a backend project built with Java and Spring Boot for processing client documents.

The application registers clients, accepts real PDF uploads, extracts the client's CPF/CNPJ from the file name, and matches the document against the client database — **asynchronously**, through an Amazon SQS queue. It is being developed as a backend/cloud portfolio project, evolving step by step from a fully local setup toward a cloud-native AWS architecture.

---

## Current Features

- Register, list and find clients
- Upload real PDF documents (multipart upload)
- Store uploaded files through a pluggable `StorageService` (local filesystem or Amazon S3)
- Extract CPF/CNPJ from the uploaded file name
- **Asynchronous document processing via Amazon SQS**: upload returns immediately with status `PENDING`, a scheduled worker consumes the queue and resolves the match afterward
- Match documents against existing clients and update their status (`PROCESSED` / `CLIENT_NOT_FOUND`)
- Download the original uploaded file back (local or S3)
- Manual queue trigger endpoint for debugging (`POST /queue/process-one`)
- Database migrations with Flyway
- Global exception handling with a consistent JSON error format
- Interactive API docs with Swagger / OpenAPI
- Multiple Spring profiles for local, S3, RDS and production setups
- Dockerized application (multi-stage build) with a Docker Compose stack that runs the API itself
- Docker image published to Amazon ECR

---

## Tech Stack

- Java 21
- Spring Boot 4.1.0
- Spring Web (Spring MVC)
- Spring Data JPA
- PostgreSQL 16
- Flyway
- AWS SDK v2 — S3 and SQS
- springdoc-openapi (Swagger UI)
- Jakarta Validation
- Docker / Docker Compose
- Maven

---

## Architecture — Current Version

Document upload is asynchronous end to end: the HTTP request only stores the file and enqueues a message — a separate scheduled worker resolves the client match afterward.

```text
Postman / HTTP Client
        |
        v
Spring Boot REST API  (ClientController / DocumentController)
        |
        v
DocumentService
        |
        +--> StorageService  --> local filesystem or Amazon S3
        |
        +--> Document saved with status PENDING
        |
        v
DocumentQueueProducer --> Amazon SQS
                              |
                              v
                    DocumentQueueScheduler (polls every N ms)
                              |
                              v
                    DocumentQueueConsumer --> DocumentProcessingService
                              |                        |
                              |                        +--> match CPF/CNPJ against clients
                              |                        +--> update status: PROCESSED / CLIENT_NOT_FOUND
                              v
                    delete message from SQS

                    PostgreSQL (migrated with Flyway)
```

> Note: today the "worker" runs inside the same process as the API (a `@Scheduled` method), not as a separate ECS service. That extraction is a planned next step — see [Next Steps](#next-steps).

---

## Storage

The application uses a `StorageService` abstraction to store uploaded documents. Only the file's physical location changes between implementations — the metadata (`bucketName`, `s3Key`, `contentType`, `fileSize`) always lives in PostgreSQL, never the binary itself.

Currently supported:

- `local`: stores files on the local filesystem under `storage/local`
- `s3`: stores files in an Amazon S3 bucket, via the AWS SDK v2

The active storage provider is configured by:

```yaml
app:
  storage:
    type: local   # or s3
```

---

## Asynchronous Document Processing with SQS

When a PDF is uploaded, the API stores the file (local or S3), creates a document record with `PENDING` status, and publishes a message to an Amazon SQS queue. A scheduled worker then polls the queue, resolves whether the extracted CPF/CNPJ matches an existing client, and updates the document status.

### Flow

```text
POST /documents/upload
        |
        v
Store PDF (local or Amazon S3)
        |
        v
Save document with status PENDING
        |
        v
DocumentQueueProducer sends message to Amazon SQS
        |
        v
DocumentQueueScheduler polls the queue (default: every 10s)
        |
        v
DocumentQueueConsumer reads and parses the message
        |
        v
DocumentProcessingService resolves the client match
        |
        v
Status updated to PROCESSED or CLIENT_NOT_FOUND
        |
        v
Message deleted from SQS
```

Because SQS provides *at-least-once* delivery, a message may be processed more than once in edge cases (e.g. a crash right before deletion). The current processing logic is safe to repeat (idempotent in effect), but this is worth keeping in mind if side effects are added later (emails, external calls, etc).

You can also trigger a single consumption cycle manually, without waiting for the scheduler:

```http
POST /queue/process-one
```

> This endpoint (and the whole queue package) only exists when `app.aws.sqs.document-queue-url` is configured — it is inactive under the `local` profile today.

---

## Running Locally

### 1. Start PostgreSQL

```bash
docker compose up -d postgres
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

### 3. Run everything with Docker Compose (API included)

```bash
docker compose build
docker compose up -d
```

This builds the application image from the `Dockerfile` (multi-stage: Maven build stage + lightweight JRE runtime stage) and runs it alongside PostgreSQL, using the `prod` profile.

---

## Environment Variables

### S3 storage (profiles `aws`, `rds`, `prod`)

```bash
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=sa-east-1
AWS_S3_BUCKET_NAME=clientdocs-eduardo-dev
```

### SQS queue (profiles `aws`, `rds`)

```bash
AWS_SQS_DOCUMENT_QUEUE_URL=https://sqs.sa-east-1.amazonaws.com/xxxxxxxx/clientdocs-documents
AWS_SQS_POLLING_INTERVAL_MS=10000
```

### External database (profiles `rds`, `prod`)

```bash
DATABASE_URL=jdbc:postgresql://your-rds-endpoint:5432/clientdocs
DATABASE_USERNAME=your-username
DATABASE_PASSWORD=your-password
```

---

## Health Check

```http
GET /health
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

### List Clients

```http
GET /clients
```

### Find Client by ID

```http
GET /clients/{id}
```

---

## Document Endpoints

### Upload and Process (real, asynchronous flow)

```http
POST /documents/upload
Content-Type: multipart/form-data
```

Form field: `file` (a `.pdf`, both by extension and content type).

The document is created with status `PENDING` and enqueued for processing. Poll `GET /documents/{id}` afterward to see it resolve to `PROCESSED` or `CLIENT_NOT_FOUND`.

### Process Mock Document (legacy, synchronous, no real file)

```http
POST /documents/process-mock
```

```json
{
  "fileName": "contrato_12345678000199.pdf"
}
```

This endpoint does not touch `StorageService` or the queue — it resolves the client match synchronously, based only on the file name. Kept for now alongside the real flow; a good candidate to retire or unify once the async flow is fully covered by tests.

### List Documents

```http
GET /documents
```

### Find Document by ID

```http
GET /documents/{id}
```

### Download Document

```http
GET /documents/{id}/download
```

Returns the original file bytes (from local storage or S3) with the correct `Content-Disposition` and `Content-Type` headers.

---

## Queue Endpoints

### Manually Process One Message

```http
POST /queue/process-one
```

Only available when the SQS queue is configured (`app.aws.sqs.document-queue-url`). Useful to trigger processing without waiting for the scheduled worker.

---

## Document Statuses

| Status | Description |
|---|---|
| `PENDING` | Document created via `/documents/upload`, waiting for the queue worker to process it |
| `PROCESSING` | Used by the legacy `/documents/process-mock` synchronous flow |
| `PROCESSED` | Client was found and the document is linked to it |
| `CLIENT_NOT_FOUND` | CPF/CNPJ was extracted but no matching client exists |
| `ERROR` | Reserved for processing failures (not yet set by any flow) |

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

---

## Application Profiles

| Profile | Database | Storage | Purpose |
|---|---|---|---|
| `local` (default) | PostgreSQL via Docker Compose (`localhost`) | `local` (filesystem) | day-to-day development |
| `aws` | PostgreSQL local, `show-sql` enabled | `s3` (real bucket) + SQS | test real S3/SQS integration while keeping the database local |
| `rds` | External PostgreSQL (RDS) via env vars, `show-sql` enabled | `s3` + SQS | test against a real RDS instance while keeping verbose logging |
| `prod` | External PostgreSQL via env vars | `s3` + SQS | production — SQL logging disabled |

---

## Docker & Deploy

The application ships with a multi-stage `Dockerfile`:

1. **Build stage** — `maven:3.9.9-eclipse-temurin-21` runs `mvn clean package -DskipTests`
2. **Runtime stage** — `eclipse-temurin:21-jre` copies only the final `.jar`, keeping the production image lightweight (no build tools, no source code)

```bash
docker compose build
docker compose up -d
```

The image is also published to **Amazon ECR**, under the `clientdocs-api` repository — the natural next step toward running it on ECS.

---

## Next Steps

- [ ] Automated tests (unit + integration), especially around `DocumentQueueConsumer` failure scenarios
- [ ] Extract the queue worker into its own ECS service/task, instead of running inside the API process
- [ ] SNS / Lambda alerts for documents that land in `ERROR` or `CLIENT_NOT_FOUND`
- [ ] CloudWatch logs and metrics
- [ ] Infrastructure as code (Terraform or CDK) for the S3 bucket, SQS queue and related resources