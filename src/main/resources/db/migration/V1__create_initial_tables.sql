CREATE TABLE clients (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(150) NOT NULL,
                         cpf_cnpj VARCHAR(20) NOT NULL UNIQUE,
                         email VARCHAR(150),
                         active BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP
);

CREATE TABLE documents (
                           id BIGSERIAL PRIMARY KEY,
                           file_name VARCHAR(255) NOT NULL,
                           cpf_cnpj_extracted VARCHAR(20),
                           status VARCHAR(50) NOT NULL,
                           client_id BIGINT,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           processed_at TIMESTAMP,

                           CONSTRAINT fk_documents_clients
                               FOREIGN KEY (client_id)
                                   REFERENCES clients(id)
);