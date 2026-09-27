package com.rikkaus.wealth;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkaus.wealth.support.AbstractPostgresIntegrationTest;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Proves the integration harness itself: the container wiring, Flyway, and the baseline schema. */
class ApplicationContextIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void isConnectedToTheTestcontainersDatabaseAndNotALocalOne() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL())
                    .as("@ServiceConnection did not apply; the suite is pointed at another database")
                    .contains(String.valueOf(POSTGRES.getMappedPort(5432)));
        }
    }

    @Test
    void flywayAppliedTheBaselineMigration() throws Exception {
        try (var connection = dataSource.getConnection();
                var statement = connection.createStatement();
                ResultSet rows =
                        statement.executeQuery(
                                """
                                SELECT version, success
                                  FROM flyway_schema_history
                                 WHERE version = '1'
                                """)) {
            assertThat(rows.next())
                    .as("flyway_schema_history has no row for version 1, so V1__baseline.sql "
                            + "did not run inside the container")
                    .isTrue();
            assertThat(rows.getBoolean("success"))
                    .as("V1__baseline.sql was recorded as failed")
                    .isTrue();
        }
    }

    @Test
    void baselineCreatesTheWealthSchemaAndNoDomainTables() throws Exception {
        try (var connection = dataSource.getConnection()) {
            try (var statement = connection.createStatement();
                    ResultSet schemas =
                            statement.executeQuery(
                                    "SELECT schema_name FROM information_schema.schemata "
                                            + "WHERE schema_name = 'wealth'")) {
                assertThat(schemas.next()).as("the wealth schema does not exist").isTrue();
            }
            try (var statement = connection.createStatement();
                    ResultSet tables =
                            statement.executeQuery(
                                    "SELECT count(*) AS table_count FROM information_schema.tables "
                                            + "WHERE table_schema = 'wealth'")) {
                assertThat(tables.next()).isTrue();
                assertThat(tables.getInt("table_count"))
                        .as("the baseline must create the wealth schema and no domain tables")
                        .isZero();
            }
        }
    }
}
