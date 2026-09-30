-- Corrige nomes de coluna que ficaram divergentes do que o Hibernate espera.
--
-- O Hibernate converte camelCase para snake_case automaticamente:
--   Furgao.volumeM3   -> volume_m3
--   Motorista.tipoCNH -> tipo_cnh
--
-- A V1 histórica criou:
--   furgao.volumem3
--   motorista.tipocnh
--
-- Esta migration corrige esses nomes depois da V3.
--
-- A operação é defensiva:
-- se a coluna antiga existir, ela será renomeada.
-- se já estiver correta, não faz nada.

DO $$
BEGIN

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'furgao'
          AND column_name = 'volumem3'
    ) THEN

ALTER TABLE furgao
    RENAME COLUMN volumem3 TO volume_m3;

END IF;


    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'motorista'
          AND column_name = 'tipocnh'
    ) THEN

ALTER TABLE motorista
    RENAME COLUMN tipocnh TO tipo_cnh;

END IF;

END $$;