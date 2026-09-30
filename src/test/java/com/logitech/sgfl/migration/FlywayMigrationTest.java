package com.logitech.sgfl.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class FlywayMigrationTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("flyway_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Test
    void deveExecutarMigracoesFlywayComSucesso() throws SQLException {

        Flyway flyway =
                Flyway.configure()
                        .dataSource(
                                postgres.getJdbcUrl(),
                                postgres.getUsername(),
                                postgres.getPassword()
                        )
                        .locations(
                                "classpath:db/migration"
                        )
                        .baselineOnMigrate(true)
                        .load();

        var resultado =
                flyway.migrate();

        assertThat(
                resultado.migrationsExecuted
        ).isGreaterThanOrEqualTo(5);

        try (Connection conexao = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conexao.createStatement()) {

            // V5 removeu os veículos duplicados deixados pela V4
            assertThat(contar(stmt,
                    "SELECT COUNT(*) FROM (SELECT placa FROM veiculo GROUP BY placa HAVING COUNT(*) > 1) d"))
                    .isZero();

            // ...e os motoristas duplicados (inclusive os com CPF em formatos diferentes)
            assertThat(contar(stmt,
                    "SELECT COUNT(*) FROM (SELECT cpf FROM motorista GROUP BY cpf HAVING COUNT(*) > 1) d"))
                    .isZero();

            // CPFs ficaram só com dígitos
            assertThat(contar(stmt, "SELECT COUNT(*) FROM motorista WHERE cpf ~ '\\D'")).isZero();

            // Nenhuma entrega ficou apontando para um veículo/motorista removido
            assertThat(contar(stmt,
                    "SELECT COUNT(*) FROM entrega e LEFT JOIN veiculo v ON v.id = e.veiculo_id "
                            + "WHERE e.veiculo_id IS NOT NULL AND v.id IS NULL")).isZero();
            assertThat(contar(stmt,
                    "SELECT COUNT(*) FROM entrega e LEFT JOIN motorista m ON m.id = e.motorista_id "
                            + "WHERE e.motorista_id IS NOT NULL AND m.id IS NULL")).isZero();

            // Garantias de integridade criadas pela V5
            assertThat(contar(stmt,
                    "SELECT COUNT(*) FROM pg_indexes WHERE indexname IN "
                            + "('uq_entrega_veiculo_em_transito', 'uq_entrega_motorista_em_transito')"))
                    .isEqualTo(2);
            assertThat(contar(stmt,
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname IN ('uk_veiculo_placa', 'uk_motorista_cpf')"))
                    .isEqualTo(2);

            // O admin com senha conhecida não pode sobreviver a uma instalação nova
            assertThat(contar(stmt, "SELECT COUNT(*) FROM usuarios WHERE email = 'admin@gmail.com'")).isZero();
        }
    }

    private long contar(Statement stmt, String sql) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
