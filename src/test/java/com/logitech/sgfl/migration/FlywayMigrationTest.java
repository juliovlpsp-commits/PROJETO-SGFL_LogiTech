package com.logitech.sgfl.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

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
    void deveExecutarMigracoesFlywayComSucesso() {

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
        ).isGreaterThanOrEqualTo(1);
    }
}