package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Characterization tests for {@link TimeDepositCalculator#updateBalance(List)}.
 *
 * <p>These tests pin the legacy behavior exactly as it is (eligibility boundaries, rounding,
 * repeated updates) so that refactoring cannot silently change it. The expected values were
 * recorded from the original implementation. Balances are compared as exact {@code double}s on purpose.
 */
class TimeDepositCalculatorTest {

    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @ParameterizedTest(name = "{0}, {1} days: 10000.00 -> {2}")
    @CsvSource({
            "basic,   0,    10000.00",
            "basic,   29,   10000.00",
            "basic,   30,   10000.00",
            "basic,   31,   10008.33",
            "basic,   3650, 10008.33",
            "student, 30,   10000.00",
            "student, 31,   10025.00",
            "student, 365,  10025.00",
            "student, 366,  10000.00",
            "premium, 30,   10000.00",
            "premium, 31,   10000.00",
            "premium, 44,   10000.00",
            "premium, 45,   10000.00",
            "premium, 46,   10041.67"
    })
    void appliesOneMonthOfInterestOnlyWithinEligibleDays(String planType, int days, double expectedBalance) {
        assertThat(updatedBalance(planType, 10000.00, days)).isEqualTo(expectedBalance);
    }

    @ParameterizedTest(name = "{0} ({1}/12 per month): {2} -> {3}")
    @CsvSource({
            "basic,   1%, 50000.00,   50041.67",
            "basic,   1%, 999.99,     1000.82",
            "basic,   1%, 1234567.89, 1235596.70",
            "student, 3%, 50000.00,   50125.00",
            "student, 3%, 999.99,     1002.49",
            // interest 3086.42 is rounded correctly, but the double sum is not exactly 1237654.31
            "student, 3%, 1234567.89, 1237654.3099999998",
            "premium, 5%, 50000.00,   50208.33",
            "premium, 5%, 999.99,     1004.16",
            "premium, 5%, 1234567.89, 1239711.92"
    })
    void appliesTheAnnualRateDividedByTwelve(String planType, String annualRate, double balance, double expectedBalance) {
        assertThat(updatedBalance(planType, balance, 100)).isEqualTo(expectedBalance);
    }

    @ParameterizedTest(name = "\"{0}\" earns no interest")
    @CsvSource({"gold", "Basic", "PREMIUM", "' student'", "''"})
    void unknownOrDifferentlyCasedPlanTypeEarnsNoInterest(String planType) {
        assertThat(updatedBalance(planType, 10000.00, 100)).isEqualTo(10000.00);
    }

    @ParameterizedTest(name = "{0}, {1}: -> {2}")
    @CsvSource({
            "basic,   1.00,   1.00",    // 0.000833... rounds to 0.00
            "basic,   6.00,   6.01",    // 0.005 rounds HALF_UP to 0.01
            "basic,   18.00,  18.01",   // 0.015 is 0.01499999... in binary -> 0.01 (BigDecimal.valueOf would give 0.02)
            "premium, 25.20,  25.30",   // 0.105 is 0.10499999... in binary -> 0.10
            "basic,   0.00,   0.00",
            "basic,   -6.00,  -6.01",   // HALF_UP rounds away from zero
            "basic,   -1000.00, -1000.83"
    })
    void roundsOnlyTheInterestHalfUpToTwoDecimalsFromItsExactBinaryValue(String planType, double balance, double expectedBalance) {
        assertThat(updatedBalance(planType, balance, 100)).isEqualTo(expectedBalance);
    }

    @Test
    void doesNotRoundTheBalanceItself() {
        // interest 0.0833375 -> 0.08; the third decimal of the balance is kept
        assertThat(updatedBalance("basic", 100.005, 31)).isEqualTo(100.005 + 0.08);
    }

    @Test
    void keepsDoublePrecisionForVeryLargeBalances() {
        assertThat(updatedBalance("basic", 1e15, 31)).isEqualTo(1.0008333333333334E15);
    }

    @Test
    void updatesTheOriginalExampleDeposit() {
        assertThat(updatedBalance("basic", 1234567.00, 45)).isEqualTo(1235595.81);
    }

    @Test
    void repeatedUpdatesCompoundInterestWithoutChangingDays() {
        TimeDeposit deposit = new TimeDeposit(1, "basic", 10000.00, 31);

        calculator.updateBalance(List.of(deposit));
        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(10016.67);
        assertThat(deposit.getDays()).isEqualTo(31);
    }

    @Test
    void repeatedUpdatesLeaveIneligibleDepositUnchanged() {
        TimeDeposit deposit = new TimeDeposit(1, "premium", 10000.00, 45);

        for (int i = 0; i < 3; i++) {
            calculator.updateBalance(List.of(deposit));
        }

        assertThat(deposit.getBalance()).isEqualTo(10000.00);
    }

    @Test
    void updatesEachDepositInTheListIndependently() {
        TimeDeposit basic = new TimeDeposit(1, "basic", 10000.00, 31);
        TimeDeposit student = new TimeDeposit(2, "student", 10000.00, 366);
        TimeDeposit premium = new TimeDeposit(3, "premium", 10000.00, 46);

        calculator.updateBalance(List.of(basic, student, premium));

        assertThat(basic.getBalance()).isEqualTo(10008.33);
        assertThat(student.getBalance()).isEqualTo(10000.00);
        assertThat(premium.getBalance()).isEqualTo(10041.67);
    }

    @Test
    void changesOnlyTheBalance() {
        TimeDeposit deposit = new TimeDeposit(7, "student", 10000.00, 100);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getId()).isEqualTo(7);
        assertThat(deposit.getPlanType()).isEqualTo("student");
        assertThat(deposit.getDays()).isEqualTo(100);
    }

    @Test
    void acceptsAnEmptyList() {
        assertThatCode(() -> calculator.updateBalance(List.of())).doesNotThrowAnyException();
    }

    private double updatedBalance(String planType, double balance, int days) {
        TimeDeposit deposit = new TimeDeposit(1, planType, balance, days);
        calculator.updateBalance(List.of(deposit));
        return deposit.getBalance();
    }
}
