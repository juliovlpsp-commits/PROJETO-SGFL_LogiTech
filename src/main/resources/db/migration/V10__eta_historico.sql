-- Histórico de previsões de ETA por entrega.
--
-- Guarda cada estimativa (distância, duração, chegada prevista e a fonte
-- do cálculo) para comparar a previsão inicial com o tempo real de
-- entrega. O serviço grava no máximo um registro por entrega a cada
-- 5 minutos.
CREATE TABLE entrega_eta (
    id                BIGSERIAL PRIMARY KEY,
    entrega_id        BIGINT NOT NULL
        REFERENCES entrega(id) ON DELETE CASCADE,
    distancia_km      DOUBLE PRECISION,
    duracao_minutos   INTEGER,
    previsao_chegada  TIMESTAMP,
    fonte             VARCHAR(20) NOT NULL,
    criado_em         TIMESTAMP NOT NULL
);

CREATE INDEX idx_entrega_eta_entrega
    ON entrega_eta (entrega_id, criado_em DESC);
