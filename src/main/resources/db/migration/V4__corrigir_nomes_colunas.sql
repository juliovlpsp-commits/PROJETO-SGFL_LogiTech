-- Corrige os nomes das colunas para o padrão esperado pelo Hibernate.

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