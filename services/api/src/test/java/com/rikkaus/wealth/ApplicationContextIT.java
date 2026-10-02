package com.rikkaus.wealth;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkaus.wealth.support.AbstractPostgresIntegrationTest;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
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
    void theMigrationsCreateExactlyTheTablesTheSlicesDeclare() throws Exception {
        // This replaces an assertion that the wealth schema held no tables at all, which was true only
        // while V1__baseline.sql was the single migration. V2__identity.sql adds the identity slice's
        // two tables, so the check becomes an exact table set rather than a count of zero: it still
        // fails on a table nobody declared, including one Hibernate created behind Flyway's back, which
        // is the regression the original assertion existed to catch.
        try (var connection = dataSource.getConnection()) {
            try (var statement = connection.createStatement();
                    ResultSet schemas =
                            statement.executeQuery(
                                    "SELECT schema_name FROM information_schema.schemata "
                                            + "WHERE schema_name = 'wealth'")) {
                assertThat(schemas.next()).as("the wealth schema does not exist").isTrue();
            }
            List<String> tableNames = new ArrayList<>();
            try (var statement = connection.createStatement();
                    ResultSet tables =
                            statement.executeQuery(
                                    "SELECT table_name FROM information_schema.tables "
                                            + "WHERE table_schema = 'wealth' ORDER BY table_name")) {
                while (tables.next()) {
                    tableNames.add(tables.getString("table_name"));
                }
            }
            assertThat(tableNames)
                    .as("the wealth schema must hold exactly the tables the migrations declare")
                    .containsExactly("refresh_tokens", "users");
        }
    }
}
