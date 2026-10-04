-- V7 - Rastreamento, auditoria, comprovante, agenda, ETA, custos e portal publico.

ALTER TABLE entrega
    ADD COLUMN IF NOT EXISTS codigo_rastreio VARCHAR(40),
    ADD COLUMN IF NOT EXISTS agendada_inicio TIMESTAMP,
    ADD COLUMN IF NOT EXISTS agendada_fim TIMESTAMP,
    ADD COLUMN IF NOT EXISTS iniciada_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS entregue_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS latitude_origem NUMERIC(9, 6),
    ADD COLUMN IF NOT EXISTS longitude_origem NUMERIC(9, 6),
    ADD COLUMN IF NOT EXISTS latitude_destino NUMERIC(9, 6),
    ADD COLUMN IF NOT EXISTS longitude_destino NUMERIC(9, 6),
    ADD COLUMN IF NOT EXISTS valor_frete NUMERIC(12, 2) NOT NULL DEFAULT 0;

UPDATE entrega
SET codigo_rastreio = 'SGFL-' || UPPER(SUBSTRING(md5(random()::text || clock_timestamp()::text) FROM 1 FOR 12))
WHERE codigo_rastreio IS NULL;

ALTER TABLE entrega
    ALTER COLUMN codigo_rastreio SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_entrega_codigo_rastreio
    ON entrega(codigo_rastreio);

CREATE INDEX IF NOT EXISTS idx_entrega_agendada_inicio
    ON entrega(agendada_inicio);

CREATE INDEX IF NOT EXISTS idx_entrega_agendada_fim
    ON entrega(agendada_fim);

CREATE TABLE IF NOT EXISTS entrega_evento (
    id BIGSERIAL PRIMARY KEY,
    entrega_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    status_anterior VARCHAR(30),
    status_novo VARCHAR(30),
    ocorrido_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responsavel VARCHAR(150),
    observacao TEXT,

    CONSTRAINT fk_entrega_evento_entrega
        FOREIGN KEY (entrega_id)
        REFERENCES entrega(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_entrega_evento_entrega
    ON entrega_evento(entrega_id, ocorrido_em);

CREATE TABLE IF NOT EXISTS comprovante_entrega (
    id BIGSERIAL PRIMARY KEY,
    entrega_id BIGINT NOT NULL UNIQUE,
    foto_path VARCHAR(500),
    assinatura TEXT,
    nome_recebedor VARCHAR(255) NOT NULL,
    recebido_em TIMESTAMP NOT NULL,
    observacao TEXT,

    CONSTRAINT fk_comprovante_entrega_entrega
        FOREIGN KEY (entrega_id)
        REFERENCES entrega(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS custo_entrega (
    id BIGSERIAL PRIMARY KEY,
    entrega_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    descricao VARCHAR(255),
    valor NUMERIC(12, 2) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_custo_entrega_entrega
        FOREIGN KEY (entrega_id)
        REFERENCES entrega(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_custo_entrega_valor
        CHECK (valor >= 0)
);

CREATE INDEX IF NOT EXISTS idx_custo_entrega_entrega
    ON custo_entrega(entrega_id);
