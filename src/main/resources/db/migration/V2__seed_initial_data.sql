-- Migração Flyway: Ajustes em tabelas preexistentes e carga inicial de motoristas

-- Garante que a coluna descricao exista na tabela entrega (caso venha de banco preexistente)
ALTER TABLE entrega ADD COLUMN IF NOT EXISTS descricao VARCHAR(255);

-- Motoristas iniciais de demonstração
INSERT INTO motorista (nome, cpf, tipocnh)
SELECT 'Carlos Eduardo Silva', '123.456.789-01', 'E'
WHERE NOT EXISTS (SELECT 1 FROM motorista WHERE cpf = '123.456.789-01');

INSERT INTO motorista (nome, cpf, tipocnh)
SELECT 'Marcos Antonio Souza', '234.567.890-12', 'B'
WHERE NOT EXISTS (SELECT 1 FROM motorista WHERE cpf = '234.567.890-12');
