-- V5: saneamento de dados e garantias de integridade no próprio banco.
--
-- Por que existe (e por que NÃO alteramos a V4):
--   Migrations já aplicadas não podem ser editadas (o Flyway valida o checksum e recusa
--   subir). A V4 é um dump do banco local e deixou lixo: veículos com a mesma placa,
--   motoristas com o mesmo CPF (em formatos diferentes) e um administrador com senha
--   conhecida. Esta migration corrige isso e impede que volte a acontecer.

-- ---------------------------------------------------------------------------
-- 1) Normaliza placas (7 caracteres, maiúsculas) e CPFs (somente dígitos)
-- ---------------------------------------------------------------------------
UPDATE veiculo
   SET placa = NULLIF(upper(regexp_replace(placa, '[^a-zA-Z0-9]', '', 'g')), '')
 WHERE placa IS NOT NULL;

UPDATE motorista
   SET cpf = NULLIF(regexp_replace(cpf, '\D', '', 'g'), '')
 WHERE cpf IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 2) Remove duplicatas de veículo (mantém o menor id) e reaponta as entregas
-- ---------------------------------------------------------------------------
UPDATE entrega e
   SET veiculo_id = d.keep_id
  FROM (SELECT id, MIN(id) OVER (PARTITION BY placa) AS keep_id
          FROM veiculo
         WHERE placa IS NOT NULL) d
 WHERE e.veiculo_id = d.id
   AND d.id <> d.keep_id;

DELETE FROM caminhao
 WHERE id IN (SELECT id FROM (SELECT id, MIN(id) OVER (PARTITION BY placa) AS keep_id
                                FROM veiculo WHERE placa IS NOT NULL) d
               WHERE d.id <> d.keep_id);

DELETE FROM furgao
 WHERE id IN (SELECT id FROM (SELECT id, MIN(id) OVER (PARTITION BY placa) AS keep_id
                                FROM veiculo WHERE placa IS NOT NULL) d
               WHERE d.id <> d.keep_id);

DELETE FROM veiculo
 WHERE id IN (SELECT id FROM (SELECT id, MIN(id) OVER (PARTITION BY placa) AS keep_id
                                FROM veiculo WHERE placa IS NOT NULL) d
               WHERE d.id <> d.keep_id);

-- ---------------------------------------------------------------------------
-- 3) Remove duplicatas de motorista (mantém o menor id) e reaponta as entregas
-- ---------------------------------------------------------------------------
UPDATE entrega e
   SET motorista_id = d.keep_id
  FROM (SELECT id, MIN(id) OVER (PARTITION BY cpf) AS keep_id
          FROM motorista
         WHERE cpf IS NOT NULL) d
 WHERE e.motorista_id = d.id
   AND d.id <> d.keep_id;

DELETE FROM motorista
 WHERE id IN (SELECT id FROM (SELECT id, MIN(id) OVER (PARTITION BY cpf) AS keep_id
                                FROM motorista WHERE cpf IS NOT NULL) d
               WHERE d.id <> d.keep_id);

-- ---------------------------------------------------------------------------
-- 4) Unicidade garantida pelo banco (a checagem no código sozinha tem corrida)
-- ---------------------------------------------------------------------------
ALTER TABLE veiculo   ADD CONSTRAINT uk_veiculo_placa UNIQUE (placa);
ALTER TABLE motorista ADD CONSTRAINT uk_motorista_cpf UNIQUE (cpf);

-- ---------------------------------------------------------------------------
-- 5) Um veículo / motorista não pode estar em duas entregas EM_TRANSITO
--    Duas requisições simultâneas de alocação passavam pela checagem do serviço ao
--    mesmo tempo; com estes índices a segunda é barrada pelo banco.
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM entrega
                WHERE status = 'EM_TRANSITO' AND veiculo_id IS NOT NULL
                GROUP BY veiculo_id HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Migracao V5: existe veiculo alocado em mais de uma entrega EM_TRANSITO. Corrija os dados e tente novamente.';
    END IF;

    IF EXISTS (SELECT 1 FROM entrega
                WHERE status = 'EM_TRANSITO' AND motorista_id IS NOT NULL
                GROUP BY motorista_id HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Migracao V5: existe motorista alocado em mais de uma entrega EM_TRANSITO. Corrija os dados e tente novamente.';
    END IF;
END $$;

CREATE UNIQUE INDEX uq_entrega_veiculo_em_transito
    ON entrega (veiculo_id) WHERE status = 'EM_TRANSITO';

CREATE UNIQUE INDEX uq_entrega_motorista_em_transito
    ON entrega (motorista_id) WHERE status = 'EM_TRANSITO';

-- ---------------------------------------------------------------------------
-- 6) Remove o administrador com senha conhecida inserido pela V4
--    Só apaga se a senha ainda for exatamente a do dump; se alguém já trocou,
--    o usuário é preservado. O admin agora é criado por variáveis de ambiente
--    (BOOTSTRAP_ADMIN_*), ver README.
-- ---------------------------------------------------------------------------
DELETE FROM usuarios
 WHERE email = 'admin@gmail.com'
   AND password = '$2a$10$2rl1FHAShiTOvNbYgyqYwuudmhsqN33C9de1l43YE8yeDtQZsmhJq';
