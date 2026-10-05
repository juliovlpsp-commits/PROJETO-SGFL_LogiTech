-- Modo de estoque configurável (SGFL_STOCK_MODE).
--
-- quantidade_reservada guarda o que está bloqueado para pedidos abertos
-- quando o modo é RESERVA. No modo IMEDIATA (padrão) a coluna permanece 0.
ALTER TABLE estoque
    ADD COLUMN quantidade_reservada INT NOT NULL DEFAULT 0;

ALTER TABLE estoque
    ADD CONSTRAINT ck_estoque_reservado
        CHECK (quantidade_reservada >= 0);
