-- Migração inicial Flyway: Criação das tabelas do SGFL compatíveis com as entidades JPA

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    perfil VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS veiculo (
    id BIGSERIAL PRIMARY KEY,
    placa VARCHAR(255),
    modelo VARCHAR(255),
    capacidade_carga_kg DOUBLE PRECISION NOT NULL
);

CREATE TABLE IF NOT EXISTS caminhao (
    id BIGINT PRIMARY KEY,
    quantidade_eixos INT NOT NULL,
    CONSTRAINT fk_caminhao_veiculo FOREIGN KEY (id) REFERENCES veiculo (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS furgao (
    id BIGINT PRIMARY KEY,
    volumem3 DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_furgao_veiculo FOREIGN KEY (id) REFERENCES veiculo (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS motorista (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255),
    cpf VARCHAR(255),
    tipocnh VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS entrega (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(255),
    endereco_origem VARCHAR(255),
    endereco_destino VARCHAR(255),
    peso_carga_kg DOUBLE PRECISION NOT NULL,
    status VARCHAR(50),
    veiculo_id BIGINT,
    motorista_id BIGINT,
    CONSTRAINT fk_entrega_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculo (id) ON DELETE SET NULL,
    CONSTRAINT fk_entrega_motorista FOREIGN KEY (motorista_id) REFERENCES motorista (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_entrega_status ON entrega(status);
