-- Execute somente em um banco de desenvolvimento/staging descartável.
-- Cria até 1.000 registros identificáveis e não duplica os que já existem.
INSERT INTO entrega (descricao, endereco_origem, endereco_destino, peso_carga_kg, status)
SELECT
    'PERF_SEED_' || serie,
    'Origem benchmark ' || (serie % 20),
    'Destino benchmark ' || (serie % 100),
    10 + (serie % 5000),
    CASE serie % 4
        WHEN 0 THEN 'PENDENTE'
        WHEN 1 THEN 'EM_TRANSITO'
        WHEN 2 THEN 'ENTREGUE'
        ELSE 'CANCELADA'
    END
FROM generate_series(1, 1000) AS serie
WHERE NOT EXISTS (
    SELECT 1 FROM entrega existente
    WHERE existente.descricao = 'PERF_SEED_' || serie
);

-- Para remover somente os dados criados por esta carga de teste:
-- DELETE FROM entrega WHERE descricao ~ '^PERF_SEED_[0-9]+$';
