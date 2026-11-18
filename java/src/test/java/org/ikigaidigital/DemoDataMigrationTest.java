package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the schema migration and the dev-only demo data apply cleanly to PostgreSQL.
 */
@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/demo")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class DemoDataMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loadsDemoDepositsAndWithdrawals() {
        assertThat(jdbcTemplate.queryForList("SELECT \"planType\" FROM \"timeDeposits\" ORDER BY \"id\"", String.class))
                .containsExactly("basic", "basic", "student", "student", "premium", "premium");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"withdrawals\"", Integer.class)).isEqualTo(4);
    }
}
