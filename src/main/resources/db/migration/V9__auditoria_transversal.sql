-- Auditoria transversal: histórico de criação/alteração/exclusão de cliente,
-- produto e pedido (a linha do tempo de entrega continua em entrega_evento).
--
-- dados_antes / dados_depois guardam um resumo JSON dos campos simples da
-- entidade (sem relações), o suficiente para responder "quem mudou o quê".
CREATE TABLE auditoria_registro (
    id            BIGSERIAL PRIMARY KEY,
    entidade      VARCHAR(40)  NOT NULL,
    entidade_id   BIGINT,
    acao          VARCHAR(30)  NOT NULL,
    descricao     VARCHAR(255),
    dados_antes   TEXT,
    dados_depois  TEXT,
    usuario       VARCHAR(120),
    criado_em     TIMESTAMP    NOT NULL
);

CREATE INDEX idx_auditoria_entidade_id
    ON auditoria_registro (entidade, entidade_id);

CREATE INDEX idx_auditoria_criado_em
    ON auditoria_registro (criado_em DESC);
