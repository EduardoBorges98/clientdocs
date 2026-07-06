ALTER TABLE documents
    ADD COLUMN bucket_name VARCHAR(100), --nome do bucket S3
ADD COLUMN s3_key VARCHAR(500), --caminho do arquivo no S3
ADD COLUMN content_type VARCHAR(100), --tipo do arquivo, exemplo application/pdf
ADD COLUMN file_size BIGINT; --tamanho do arquivo em bytes