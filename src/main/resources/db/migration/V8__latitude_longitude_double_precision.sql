-- As entidades mapeiam latitude/longitude como Double (float8), mas a V7 criou
-- as colunas como NUMERIC(9,6). Com spring.jpa.hibernate.ddl-auto=validate, o
-- Hibernate falhava na subida do backend com:
--   Schema-validation: wrong column type encountered in column [latitude_destino]
--   in table [entrega]; found [numeric (Types#NUMERIC)], but expecting [float(53)]
-- Converte as quatro colunas para DOUBLE PRECISION, que é o tipo derivado do
-- mapeamento Java Double. Os valores existentes são preservados via cast.
ALTER TABLE entrega
    ALTER COLUMN latitude_origem TYPE DOUBLE PRECISION
        USING latitude_origem::DOUBLE PRECISION,
    ALTER COLUMN longitude_origem TYPE DOUBLE PRECISION
        USING longitude_origem::DOUBLE PRECISION,
    ALTER COLUMN latitude_destino TYPE DOUBLE PRECISION
        USING latitude_destino::DOUBLE PRECISION,
    ALTER COLUMN longitude_destino TYPE DOUBLE PRECISION
        USING longitude_destino::DOUBLE PRECISION;
