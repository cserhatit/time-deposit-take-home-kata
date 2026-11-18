package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TestcontainersConfiguration;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.Withdrawal;
import org.ikigaidigital.application.TimeDepositWithWithdrawals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests of {@link TimeDepositPersistenceAdapter} against PostgreSQL with the Flyway schema.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, TimeDepositPersistenceAdapter.class})
@Sql("/time-deposits.sql")
class TimeDepositPersistenceAdapterTest {

    @Autowired
    private TimeDepositPersistenceAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findAllMapsEveryColumnOrderedById() {
        List<TimeDeposit> deposits = adapter.findAll();

        assertThat(deposits).extracting(TimeDeposit::getId).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(deposits.get(1))
                .extracting(TimeDeposit::getPlanType, TimeDeposit::getBalance, TimeDeposit::getDays)
                .containsExactly("student", 1234567.89, 100);
    }

    @Test
    void findAllWithWithdrawalsReturnsWithdrawalsOrderedByIdAndEmptyListWhenNone() {
        List<TimeDepositWithWithdrawals> deposits = adapter.findAllWithWithdrawals();

        assertThat(deposits).extracting(d -> d.deposit().getId()).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(deposits.get(0).withdrawals()).containsExactly(
                new Withdrawal(1, new BigDecimal("100.00"), LocalDate.of(2026, 1, 10)),
                new Withdrawal(2, new BigDecimal("50.25"), LocalDate.of(2026, 2, 10)));
        assertThat(deposits.get(1).withdrawals()).isEmpty();
        assertThat(deposits.get(2).withdrawals()).extracting(Withdrawal::id).containsExactly(3);
    }

    @Test
    void storesTheCalculatorsRawDoubleRoundedToTwoDecimals() {
        List<TimeDeposit> deposits = adapter.findAll();
        TimeDeposit student = deposits.get(1);
        student.setBalance(1237654.3099999998); // raw calculator result for this deposit, see TimeDepositCalculatorTest

        adapter.saveBalances(deposits);
        entityManager.flush();
        entityManager.clear();

        assertThat(storedBalance(2)).isEqualByComparingTo("1237654.31");
        assertThat(adapter.findAll().get(1).getBalance()).isEqualTo(1237654.31);
        assertThat(storedBalance(1)).isEqualByComparingTo("10000.00");
    }

    private BigDecimal storedBalance(int id) {
        return jdbcTemplate.queryForObject("SELECT \"balance\" FROM \"timeDeposits\" WHERE \"id\" = ?", BigDecimal.class, id);
    }
}
