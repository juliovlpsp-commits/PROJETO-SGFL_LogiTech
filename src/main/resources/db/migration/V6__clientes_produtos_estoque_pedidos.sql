-- V6
-- Cadastro de clientes, produtos, estoque e pedidos.

CREATE TABLE IF NOT EXISTS cliente (
                                       id BIGSERIAL PRIMARY KEY,
                                       nome VARCHAR(255) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    telefone VARCHAR(30),
    cep VARCHAR(20),
    logradouro VARCHAR(255),
    numero VARCHAR(30),
    complemento VARCHAR(255),
    bairro VARCHAR(255),
    cidade VARCHAR(255),
    uf VARCHAR(2),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS produto (
                                       id BIGSERIAL PRIMARY KEY,
                                       codigo VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL,
    descricao TEXT,
    preco NUMERIC(12, 2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS estoque (
                                       id BIGSERIAL PRIMARY KEY,
                                       produto_id BIGINT NOT NULL UNIQUE,
                                       quantidade_disponivel INT NOT NULL DEFAULT 0,
                                       atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_estoque_produto
                                       FOREIGN KEY (produto_id)
    REFERENCES produto(id)
    ON DELETE CASCADE,

    CONSTRAINT ck_estoque_quantidade
    CHECK (quantidade_disponivel >= 0)
    );

CREATE TABLE IF NOT EXISTS pedido (
                                      id BIGSERIAL PRIMARY KEY,
                                      cliente_id BIGINT NOT NULL,
                                      status VARCHAR(30) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pedido_cliente
    FOREIGN KEY (cliente_id)
    REFERENCES cliente(id),

    CONSTRAINT ck_pedido_status
    CHECK (status IN ('ABERTO', 'CANCELADO', 'CONCLUIDO'))
    );

CREATE TABLE IF NOT EXISTS item_pedido (
                                           id BIGSERIAL PRIMARY KEY,
                                           pedido_id BIGINT NOT NULL,
                                           produto_id BIGINT NOT NULL,
                                           quantidade INT NOT NULL,
                                           preco_unitario NUMERIC(12, 2) NOT NULL,

    CONSTRAINT fk_item_pedido_pedido
    FOREIGN KEY (pedido_id)
    REFERENCES pedido(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_item_pedido_produto
    FOREIGN KEY (produto_id)
    REFERENCES produto(id),

    CONSTRAINT ck_item_pedido_quantidade
    CHECK (quantidade > 0),

    CONSTRAINT ck_item_pedido_preco
    CHECK (preco_unitario >= 0),

    CONSTRAINT uk_item_pedido_produto
    UNIQUE (pedido_id, produto_id)
    );

CREATE INDEX IF NOT EXISTS idx_pedido_cliente
    ON pedido(cliente_id);

CREATE INDEX IF NOT EXISTS idx_pedido_status
    ON pedido(status);

CREATE INDEX IF NOT EXISTS idx_item_pedido_produto
    ON item_pedido(produto_id);