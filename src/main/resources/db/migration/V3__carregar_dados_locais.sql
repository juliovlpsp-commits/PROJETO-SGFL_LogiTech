-- ============================================================
-- SGFL - Dados locais / demonstracao
-- ============================================================
--
-- Esta migration foi escrita para funcionar tanto em:
--
-- PostgreSQL
-- H2 em modo PostgreSQL (usado nos testes)
--
-- Os IDs nao sao informados manualmente.
-- Dessa forma nao precisamos manipular as sequences
-- diretamente e evitamos incompatibilidades entre bancos.
-- ============================================================


-- ============================================================
-- VEICULOS
-- ============================================================

INSERT INTO public.veiculo
(capacidade_carga_kg, modelo, placa)
SELECT
    15000,
    'Volvo FH 540',
    'ABC1D23'
    WHERE NOT EXISTS (
    SELECT 1
    FROM public.veiculo
    WHERE placa = 'ABC1D23'
);

INSERT INTO public.veiculo
(capacidade_carga_kg, modelo, placa)
SELECT
    15000,
    'Volvo FH 460',
    'DEF4E56'
    WHERE NOT EXISTS (
    SELECT 1
    FROM public.veiculo
    WHERE placa = 'DEF4E56'
);

INSERT INTO public.veiculo
(capacidade_carga_kg, modelo, placa)
SELECT
    15000,
    'Mercedes-Benz Actros',
    'GHI7H89'
    WHERE NOT EXISTS (
    SELECT 1
    FROM public.veiculo
    WHERE placa = 'GHI7H89'
);


-- ============================================================
-- CAMINHOES
-- ============================================================
--
-- O ID do caminhao e o mesmo ID do veiculo.
-- Buscamos o ID pela placa.
-- ============================================================

INSERT INTO public.caminhao
(quantidade_eixos, id)
SELECT
    6,
    v.id
FROM public.veiculo v
WHERE v.placa = 'ABC1D23'
  AND NOT EXISTS (
    SELECT 1
    FROM public.caminhao c
    WHERE c.id = v.id
);

INSERT INTO public.caminhao
(quantidade_eixos, id)
SELECT
    6,
    v.id
FROM public.veiculo v
WHERE v.placa = 'DEF4E56'
  AND NOT EXISTS (
    SELECT 1
    FROM public.caminhao c
    WHERE c.id = v.id
);

INSERT INTO public.caminhao
(quantidade_eixos, id)
SELECT
    6,
    v.id
FROM public.veiculo v
WHERE v.placa = 'GHI7H89'
  AND NOT EXISTS (
    SELECT 1
    FROM public.caminhao c
    WHERE c.id = v.id
);


-- ============================================================
-- MOTORISTAS
-- ============================================================

INSERT INTO public.motorista
(cpf, nome, tipocnh)
SELECT
    '12345678900',
    'Carlos Silva',
    'E'
    WHERE NOT EXISTS (
    SELECT 1
    FROM public.motorista
    WHERE cpf = '12345678900'
);

INSERT INTO public.motorista
(cpf, nome, tipocnh)
SELECT
    '11122233344',
    'Lucas Silva',
    'B'
    WHERE NOT EXISTS (
    SELECT 1
    FROM public.motorista
    WHERE cpf = '11122233344'
);

INSERT INTO public.motorista
(cpf, nome, tipocnh)
SELECT
    '99988877766',
    'Lucas CNH B',
    'B'
    WHERE NOT EXISTS (
    SELECT 1
    FROM public.motorista
    WHERE cpf = '99988877766'
);

INSERT INTO public.motorista
(cpf, nome, tipocnh)
SELECT
    '88877766655',
    'Rafael Oliveira',
    'B'
    WHERE NOT EXISTS (
    SELECT 1
    FROM public.motorista
    WHERE cpf = '88877766655'
);

INSERT INTO public.motorista
(cpf, nome, tipocnh)
SELECT
    '12345678901',
    'Carlos Eduardo Silva',
    'E'
    WHERE NOT EXISTS (
    SELECT 1
    FROM public.motorista
    WHERE cpf = '12345678901'
);

INSERT INTO public.motorista
(cpf, nome, tipocnh)
SELECT
    '23456789012',
    'Marcos Antonio Souza',
    'B'
    WHERE NOT EXISTS (
    SELECT 1
    FROM public.motorista
    WHERE cpf = '23456789012'
);


-- ============================================================
-- ENTREGAS
-- ============================================================
--
-- IMPORTANTE:
--
-- PENDENTE:
--   sem veiculo e sem motorista.
--
-- EM_TRANSITO:
--   possui veiculo e motorista.
--
-- ENTREGUE:
--   possui veiculo e motorista.
-- ============================================================


-- ------------------------------------------------------------
-- Entrega PENDENTE
-- ------------------------------------------------------------

INSERT INTO public.entrega
(
    endereco_destino,
    endereco_origem,
    peso_carga_kg,
    status,
    motorista_id,
    veiculo_id,
    descricao
)
SELECT
    'Rio de Janeiro, RJ',
    'Sao Paulo, SP',
    8000,
    'PENDENTE',
    NULL,
    NULL,
    'Carga aguardando alocacao'
    WHERE NOT EXISTS (
    SELECT 1
    FROM public.entrega
    WHERE descricao = 'Carga aguardando alocacao'
);


-- ------------------------------------------------------------
-- Entrega ENTREGUE - Porto Alegre
-- ------------------------------------------------------------

INSERT INTO public.entrega
(
    endereco_destino,
    endereco_origem,
    peso_carga_kg,
    status,
    motorista_id,
    veiculo_id,
    descricao
)
SELECT
    'Porto Alegre, RS',
    'Curitiba, PR',
    12000,
    'ENTREGUE',
    m.id,
    v.id,
    'Carga entregue com sucesso'
FROM public.motorista m
         JOIN public.veiculo v
              ON v.placa = 'ABC1D23'
WHERE m.cpf = '12345678900'
  AND NOT EXISTS (
    SELECT 1
    FROM public.entrega
    WHERE descricao = 'Carga entregue com sucesso'
);


-- ------------------------------------------------------------
-- Entrega ENTREGUE - Florianopolis
-- ------------------------------------------------------------

INSERT INTO public.entrega
(
    endereco_destino,
    endereco_origem,
    peso_carga_kg,
    status,
    motorista_id,
    veiculo_id,
    descricao
)
SELECT
    'Florianopolis, SC',
    'Curitiba, PR',
    5000,
    'ENTREGUE',
    m.id,
    v.id,
    'Carga entregue em Santa Catarina'
FROM public.motorista m
         JOIN public.veiculo v
              ON v.placa = 'DEF4E56'
WHERE m.cpf = '12345678901'
  AND NOT EXISTS (
    SELECT 1
    FROM public.entrega
    WHERE descricao = 'Carga entregue em Santa Catarina'
);


-- ------------------------------------------------------------
-- Entrega EM_TRANSITO
-- ------------------------------------------------------------

INSERT INTO public.entrega
(
    endereco_destino,
    endereco_origem,
    peso_carga_kg,
    status,
    motorista_id,
    veiculo_id,
    descricao
)
SELECT
    'Sao Paulo, SP',
    'Belo Horizonte, MG',
    7000,
    'EM_TRANSITO',
    m.id,
    v.id,
    'Carga atualmente em transporte'
FROM public.motorista m
         JOIN public.veiculo v
              ON v.placa = 'GHI7H89'
WHERE m.cpf = '12345678900'
  AND NOT EXISTS (
    SELECT 1
    FROM public.entrega
    WHERE descricao = 'Carga atualmente em transporte'
);


-- ------------------------------------------------------------
-- Entrega PENDENTE
-- ------------------------------------------------------------

INSERT INTO public.entrega
(
    endereco_destino,
    endereco_origem,
    peso_carga_kg,
    status,
    motorista_id,
    veiculo_id,
    descricao
)
SELECT
    'Santa Catarina, SC',
    'Curitiba, PR',
    4000,
    'PENDENTE',
    NULL,
    NULL,
    'Aguardando motorista e veiculo'
    WHERE NOT EXISTS (
    SELECT 1
    FROM public.entrega
    WHERE descricao = 'Aguardando motorista e veiculo'
);


-- ------------------------------------------------------------
-- Entrega ENTREGUE - Gramado
-- ------------------------------------------------------------

INSERT INTO public.entrega
(
    endereco_destino,
    endereco_origem,
    peso_carga_kg,
    status,
    motorista_id,
    veiculo_id,
    descricao
)
SELECT
    'Gramado, RS',
    'Sao Paulo, SP',
    3000,
    'ENTREGUE',
    m.id,
    v.id,
    'Produto T5 entregue'
FROM public.motorista m
         JOIN public.veiculo v
              ON v.placa = 'DEF4E56'
WHERE m.cpf = '12345678901'
  AND NOT EXISTS (
    SELECT 1
    FROM public.entrega
    WHERE descricao = 'Produto T5 entregue'
);


-- ============================================================
-- USUARIO ADMINISTRADOR
-- ============================================================
--
-- O ID tambem e gerado automaticamente.
-- ============================================================

INSERT INTO public.usuarios
(email, password, perfil, username)
SELECT
    'admin@gmail.com',
    '$2a$10$2rl1FHAShiTOvNbYgyqYwuudmhsqN33C9de1l43YE8yeDtQZsmhJq',
    'ROLE_ADMIN',
    'admin'
    WHERE NOT EXISTS (
    SELECT 1
    FROM public.usuarios
    WHERE username = 'admin'
);