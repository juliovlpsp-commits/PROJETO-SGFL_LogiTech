-- Execute em staging depois de carregar dados representativos:
-- psql "$DATABASE_URL" -f performance/explain-deliveries.sql
-- Compare custo, linhas reais/estimadas, buffers e tempo antes de criar índices.

EXPLAIN (ANALYZE, BUFFERS)
SELECT e.id, e.descricao, e.endereco_destino, e.status, v.placa, m.nome
FROM entrega e
LEFT JOIN veiculo v ON v.id = e.veiculo_id
LEFT JOIN motorista m ON m.id = e.motorista_id
WHERE e.status = 'PENDENTE'
ORDER BY e.id ASC
LIMIT 20 OFFSET 0;

EXPLAIN (ANALYZE, BUFFERS)
SELECT e.id, e.descricao, e.endereco_destino, e.status, v.placa, m.nome
FROM entrega e
LEFT JOIN veiculo v ON v.id = e.veiculo_id
LEFT JOIN motorista m ON m.id = e.motorista_id
WHERE (
    lower(e.descricao) LIKE '%perf\_seed\_999%' ESCAPE E'\\'
    OR lower(e.endereco_origem) LIKE '%perf\_seed\_999%' ESCAPE E'\\'
    OR lower(e.endereco_destino) LIKE '%perf\_seed\_999%' ESCAPE E'\\'
    OR lower(m.nome) LIKE '%perf\_seed\_999%' ESCAPE E'\\'
    OR lower(v.placa) LIKE '%perf\_seed\_999%' ESCAPE E'\\'
    OR lower(v.modelo) LIKE '%perf\_seed\_999%' ESCAPE E'\\'
    OR e.id = 999
)
ORDER BY e.id ASC
LIMIT 20 OFFSET 0;
