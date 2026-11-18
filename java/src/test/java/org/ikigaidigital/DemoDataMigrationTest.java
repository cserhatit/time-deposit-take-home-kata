package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the schema migration and the dev-only demo data apply cleanly to PostgreSQL.
 *
 * <p>Uses its own database instead of the shared {@link TestcontainersConfiguration} container: its Flyway
 * locations differ from the other tests, and sharing one database would make the results depend on test order.
 */
@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/demo")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DemoDataMigrationTest.DedicatedPostgres.class)
class DemoDataMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loadsDemoDepositsAndWithdrawals() {
        assertThat(jdbcTemplate.queryForList("SELECT \"planType\" FROM \"timeDeposits\" ORDER BY \"id\"", String.class))
                .containsExactly("basic", "basic", "student", "student", "premium", "premium");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"withdrawals\"", Integer.class)).isEqualTo(4);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DedicatedPostgres {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> demoPostgresContainer() {
            return new PostgreSQLContainer<>("postgres:17-alpine");
        }
    }
}
