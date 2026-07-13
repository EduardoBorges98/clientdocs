# ClientDocs Processor

ClientDocs Processor é uma API backend desenvolvida em Java com Spring Boot para upload, armazenamento e processamento assíncrono de documentos PDF de clientes.

O projeto simula um fluxo real de backend cloud, usando banco relacional, armazenamento de arquivos, fila de mensagens, processamento assíncrono, containerização e deploy em AWS ECS Fargate.

---

## Objetivo do projeto

O objetivo deste projeto é demonstrar uma arquitetura backend moderna e escalável usando Java e AWS.

A aplicação permite:

- cadastrar clientes;
- fazer upload de documentos PDF;
- armazenar arquivos no Amazon S3;
- salvar metadados no PostgreSQL;
- enviar eventos para uma fila SQS;
- processar documentos de forma assíncrona;
- atualizar o status do documento após o processamento.

---

## Tecnologias utilizadas

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Spring Validation
- PostgreSQL
- Flyway
- Maven
- Docker
- Swagger/OpenAPI
- AWS SDK for Java
- Amazon ECR
- Amazon ECS Fargate
- Amazon RDS PostgreSQL
- Amazon S3
- Amazon SQS
- AWS IAM
- Application Load Balancer

---

## Arquitetura geral

A aplicação roda em um container Docker publicado no Amazon ECR e executado no Amazon ECS Fargate.

O tráfego externo entra pelo Application Load Balancer, que encaminha as requisições para a aplicação Spring Boot rodando no ECS.

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
Scheduler interno
    ↓
RDS atualizado
```

---

## Fluxo principal

1. O usuário envia um PDF para a API.
2. A aplicação valida o arquivo.
3. O PDF é salvo no Amazon S3.
4. Um registro do documento é criado no PostgreSQL com status `PENDING`.
5. A aplicação envia uma mensagem para uma fila SQS.
6. Um scheduler interno da aplicação consulta a fila periodicamente.
7. O consumer processa a mensagem.
8. A aplicação busca o cliente pelo CPF/CNPJ extraído do nome do arquivo.
9. O documento é atualizado para `PROCESSED` ou `CLIENT_NOT_FOUND`.
10. A mensagem é removida da fila SQS.

Exemplo de status inicial:

```json
{
  "id": 4,
  "status": "PENDING",
  "clientId": null,
  "processedAt": null
}
```

Exemplo após o processamento:

```json
{
  "id": 4,
  "status": "PROCESSED",
  "clientId": 1,
  "processedAt": "2026-07-13T23:22:01.04786"
}
```

---

## Principais entidades

### Client

Representa um cliente cadastrado na base.

Campos principais:

- `id`
- `name`
- `cpfCnpj`
- `email`
- `active`
- `createdAt`
- `updatedAt`

### Document

Representa um documento enviado para processamento.

Campos principais:

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

Possíveis status do documento:

```text
PENDING
PROCESSING
PROCESSED
CLIENT_NOT_FOUND
ERROR
```

---

## Endpoints principais

### Health check

```http
GET /health
```

Exemplo:

```http
GET http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/health
```

Resposta esperada:

```text
ClientDocs API is running
```

---

### Criar cliente

```http
POST /clients
```

Exemplo de body:

```json
{
  "name": "Cliente Teste",
  "cpfCnpj": "12345678000199",
  "email": "cliente@teste.com"
}
```

---

### Listar clientes

```http
GET /clients
```

---

### Buscar cliente por ID

```http
GET /clients/{id}
```

---

### Upload de documento

```http
POST /documents/upload
```

Tipo de requisição:

```text
multipart/form-data
```

Campo esperado:

```text
file
```

Exemplo usando a URL do Load Balancer:

```http
POST http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/documents/upload
```

Exemplo de resposta inicial:

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

### Consultar documento

```http
GET /documents/{id}
```

Exemplo:

```http
GET http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/documents/4
```

Exemplo de resposta após processamento:

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

### Listar documentos

```http
GET /documents
```

---

### Download do documento

```http
GET /documents/{id}/download
```

---

### Processar uma mensagem manualmente

```http
POST /queue/process-one
```

Esse endpoint pode ser usado para testar manualmente o consumo de uma mensagem da fila.

---

## Swagger/OpenAPI

Com a aplicação rodando, a documentação Swagger pode ser acessada em:

```http
GET /swagger-ui/index.html
```

Exemplo local:

```http
http://localhost:8080/swagger-ui/index.html
```

---

## Estrutura de pacotes

Estrutura conceitual do projeto:

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

Responsabilidades principais:

- `controller`: endpoints REST;
- `dto`: objetos de entrada e saída da API;
- `entity`: entidades JPA;
- `repository`: interfaces Spring Data JPA;
- `service`: regras de negócio;
- `storage`: armazenamento local ou S3;
- `queue`: integração com SQS;
- `config`: configurações da aplicação;
- `exception`: tratamento global de erros.

---

## Profiles da aplicação

A aplicação usa profiles para separar ambientes.

### local

Usado para desenvolvimento local com PostgreSQL local ou Docker Compose.

```text
SPRING_PROFILES_ACTIVE=local
```

### aws

Usado para testes locais consumindo serviços reais da AWS, como S3 e SQS.

```text
SPRING_PROFILES_ACTIVE=aws
```

### rds

Usado para testar localmente a aplicação conectando no RDS.

```text
SPRING_PROFILES_ACTIVE=rds
```

### prod

Usado no ECS Fargate.

```text
SPRING_PROFILES_ACTIVE=prod
```

---

## Banco de dados e migrations

O projeto usa PostgreSQL como banco relacional e Flyway para versionamento do schema.

Migrations principais:

```text
V1__create_initial_tables.sql
V2__add_storage_fields_to_documents.sql
```

As migrations criam e evoluem as tabelas:

- `clients`
- `documents`

---

## Deploy na AWS

O deploy foi feito usando os seguintes serviços:

- Amazon ECR
- Amazon ECS Fargate
- Amazon RDS PostgreSQL
- Amazon S3
- Amazon SQS
- AWS IAM
- Application Load Balancer

---

## Amazon ECR

A imagem Docker da aplicação foi publicada no Amazon ECR.

Repository:

```text
clientdocs-api
```

Image URI utilizada no deploy:

```text
766577288490.dkr.ecr.sa-east-1.amazonaws.com/clientdocs-api:v2
```

Fluxo usado:

```text
Projeto Spring Boot
    ↓
Docker build
    ↓
Docker tag
    ↓
Docker push
    ↓
Amazon ECR
```

Comandos utilizados:

```bash
mvn clean package -DskipTests

docker build -t clientdocs-api .

docker tag clientdocs-api:latest 766577288490.dkr.ecr.sa-east-1.amazonaws.com/clientdocs-api:v2

aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin 766577288490.dkr.ecr.sa-east-1.amazonaws.com

docker push 766577288490.dkr.ecr.sa-east-1.amazonaws.com/clientdocs-api:v2
```

---

## Amazon ECS Fargate

A aplicação foi executada no ECS Fargate, sem necessidade de criar instância EC2 manualmente.

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

Revision validada:

```text
clientdocs-api-task:4
```

Container port:

```text
8080
```

O ECS Service mantém uma task rodando com a aplicação Spring Boot.

---

## Amazon RDS PostgreSQL

O banco de dados da aplicação roda no Amazon RDS PostgreSQL.

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

A aplicação usa o RDS para armazenar:

- clientes;
- documentos;
- status de processamento;
- metadados dos arquivos.

As migrations são executadas com Flyway.

---

## Amazon S3

Os PDFs enviados para a API são armazenados no Amazon S3.

Bucket:

```text
clientdocs-eduardo-dev
```

Exemplo de chave gerada:

```text
documents/2026/07/13720c7d-8353-4232-93e9-a07f76b89100-contrato_cliente_12345678000199_final.pdf
```

A aplicação salva no banco apenas os metadados do arquivo, como:

- bucket;
- chave S3;
- nome original;
- content type;
- tamanho do arquivo.

---

## Amazon SQS

A aplicação usa SQS para processamento assíncrono de documentos.

Queue:

```text
clientdocs-document-processing-dev
```

Queue URL:

```text
https://sqs.sa-east-1.amazonaws.com/766577288490/clientdocs-document-processing-dev
```

Após o upload, a API envia uma mensagem para a fila contendo os dados necessários para processar o documento.

Exemplo conceitual da mensagem:

```json
{
  "documentId": 4,
  "bucketName": "clientdocs-eduardo-dev",
  "storageKey": "documents/2026/07/arquivo.pdf",
  "cpfCnpjExtracted": "12345678000199"
}
```

---

## Scheduler interno

A aplicação possui um scheduler usando `@Scheduled`.

Ele executa periodicamente a leitura da fila SQS.

Responsabilidades:

- consultar a fila SQS;
- receber mensagens;
- processar documentos pendentes;
- atualizar status no RDS;
- remover mensagens processadas da fila.

Fluxo:

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

Inicialmente, a task usava credenciais via environment variables:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

Depois, o projeto foi ajustado para usar IAM Task Role, que é o modelo correto para aplicações rodando no ECS.

Role criada:

```text
clientdocs-ecs-task-role
```

Policy criada:

```text
ClientDocsEcsTaskPolicy
```

Permissões concedidas:

- acesso ao bucket S3 `clientdocs-eduardo-dev`;
- envio e consumo de mensagens na fila SQS `clientdocs-document-processing-dev`.

Com isso, as access keys foram removidas da Task Definition.

A aplicação passou a acessar S3 e SQS usando permissões da própria task ECS.

Arquitetura atual de permissão:

```text
ECS Task
    ↓ assume role
clientdocs-ecs-task-role
    ↓
S3 + SQS
```

---

## Application Load Balancer

Foi criado um Application Load Balancer para expor a API de forma mais estável.

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

Fluxo com Load Balancer:

```text
Internet
    ↓ HTTP :80
Application Load Balancer
    ↓ HTTP :8080
ECS Fargate / Spring Boot
```

Antes, a aplicação era acessada diretamente pelo IP público da task ECS:

```text
http://TASK_PUBLIC_IP:8080
```

Depois do ALB, passou a ser acessada por:

```text
http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com
```

---

## Security Groups

Foram configurados security groups separados para ALB, ECS e RDS.

### ALB Security Group

Nome:

```text
clientdocs-alb-sg
```

Inbound:

```text
HTTP 80 from 0.0.0.0/0
```

Responsabilidade:

```text
Permitir acesso público HTTP ao Load Balancer
```

### ECS Security Group

Nome:

```text
clientdocs-ecs-sg
```

Inbound final:

```text
Custom TCP 8080 from clientdocs-alb-sg
```

A regra pública direta foi removida:

```text
Custom TCP 8080 from 0.0.0.0/0
```

Responsabilidade:

```text
Permitir acesso à aplicação somente através do Load Balancer
```

### RDS Security Group

Nome:

```text
clientdocs-rds-sg
```

Inbound:

```text
PostgreSQL 5432 from clientdocs-ecs-sg
```

Responsabilidade:

```text
Permitir que somente a aplicação no ECS acesse o banco de dados
```

---

## Variáveis de ambiente da aplicação em produção

A Task Definition usa variáveis de ambiente para configurar a aplicação.

Exemplo:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://clientdocs-postgres-dev.clieq46eogak.sa-east-1.rds.amazonaws.com:5432/clientdocs
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=********
AWS_REGION=sa-east-1
AWS_S3_BUCKET_NAME=clientdocs-eduardo-dev
AWS_SQS_DOCUMENT_QUEUE_URL=https://sqs.sa-east-1.amazonaws.com/766577288490/clientdocs-document-processing-dev
AWS_SQS_POLLING_INTERVAL_MS=10000
```

Observação:

```text
AWS_ACCESS_KEY_ID e AWS_SECRET_ACCESS_KEY não são mais usados na task.
O acesso a S3 e SQS é feito via IAM Task Role.
```

---

## Validação do fluxo em cloud

O fluxo completo foi validado na AWS.

Teste realizado:

```http
POST http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/documents/upload
```

Resultado inicial:

```json
{
  "id": 4,
  "status": "PENDING",
  "clientId": null,
  "processedAt": null
}
```

Consulta posterior:

```http
GET http://clientdocs-alb-1532624692.sa-east-1.elb.amazonaws.com/documents/4
```

Resultado processado:

```json
{
  "id": 4,
  "status": "PROCESSED",
  "clientId": 1,
  "processedAt": "2026-07-13T23:22:01.04786"
}
```

Isso confirma que os seguintes componentes estão funcionando integrados:

```text
Application Load Balancer
ECS Fargate
Spring Boot
RDS PostgreSQL
S3
SQS
IAM Task Role
Scheduler interno
```

---

## Como rodar localmente

Subir dependências locais:

```bash
docker compose up -d
```

Rodar a aplicação com profile local:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Health check local:

```http
GET http://localhost:8080/health
```

---

## Como testar localmente

Criar cliente:

```http
POST http://localhost:8080/clients
```

Upload de documento:

```http
POST http://localhost:8080/documents/upload
```

Consultar documento:

```http
GET http://localhost:8080/documents/{id}
```

Swagger:

```http
http://localhost:8080/swagger-ui/index.html
```

---

## Como fazer novo deploy

Após alterar o código:

```bash
mvn clean package -DskipTests

docker build -t clientdocs-api .

docker tag clientdocs-api:latest 766577288490.dkr.ecr.sa-east-1.amazonaws.com/clientdocs-api:v3

aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin 766577288490.dkr.ecr.sa-east-1.amazonaws.com

docker push 766577288490.dkr.ecr.sa-east-1.amazonaws.com/clientdocs-api:v3
```

Depois:

1. Criar nova revision da Task Definition.
2. Atualizar a imagem para a nova tag.
3. Manter a Task Role.
4. Atualizar o ECS Service.
5. Forçar novo deployment.
6. Testar `/health`.
7. Testar upload de documento.

---

## Custos e desligamento

Para reduzir custos quando não estiver usando o projeto:

- alterar o ECS Service para `desired tasks = 0`;
- parar temporariamente o RDS;
- manter S3 e SQS, que tendem a ter custo baixo nesse volume de testes.

Atenção: o Application Load Balancer e o RDS podem gerar cobrança mesmo com pouco uso.

---

## Melhorias futuras

- Configurar HTTPS no Application Load Balancer
- Adicionar domínio próprio
- Usar AWS Secrets Manager para credenciais do banco
- Criar pipeline CI/CD com GitHub Actions
- Adicionar testes automatizados
- Melhorar endpoint `/health` com status detalhado
- Criar endpoint `/` com informações da API
- Separar worker em serviço dedicado
- Adicionar Dead Letter Queue para mensagens com erro
- Adicionar métricas e alarmes no CloudWatch
- Implementar autenticação/autorização
- Criar frontend para upload e consulta de documentos

---

## Status do projeto

Fluxo cloud validado com sucesso:

```text
Upload PDF
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

Componentes validados:

```text
Spring Boot
Docker
ECR
ECS Fargate
RDS PostgreSQL
S3
SQS
IAM Task Role
Application Load Balancer
Security Groups
```
