package org.ikigaidigital;

import org.ikigaidigital.calculation.InterestCalculationStrategy;
import org.ikigaidigital.calculation.standard.StandardInterestCalculation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shows that {@link TimeDepositCalculator} accepts a custom {@link InterestCalculationStrategy}: new interest rules
 * can be added without changing the calculator or the {@link StandardInterestCalculation}.
 */
class TimeDepositCalculatorStrategyTest {

    @Test
    void usesTheGivenStrategyAndRoundsItsResultLikeTheStandardRule() {
        TimeDepositCalculator calculator = new TimeDepositCalculator(deposit -> 1.005);
        TimeDeposit deposit = new TimeDeposit(1, "basic", 100.00, 10);

        calculator.updateBalance(List.of(deposit));

        // 1.005 is 1.00499... in binary, so the legacy HALF_UP rounding gives 1.00
        assertThat(deposit.getBalance()).isEqualTo(101.00);
    }

    @Test
    void aNewRuleCanExtendTheStandardCalculationWithoutChangingIt() {
        // Hypothetical "gold" plan, defined only in this test: 2% a year; every other plan keeps the standard rule.
        StandardInterestCalculation standard = new StandardInterestCalculation();
        InterestCalculationStrategy withGoldPlan = deposit -> "gold".equals(deposit.getPlanType())
                ? deposit.getBalance() * 0.02 / 12
                : standard.calculateInterest(deposit);
        TimeDeposit gold = new TimeDeposit(1, "gold", 10000.00, 31);
        TimeDeposit basic = new TimeDeposit(2, "basic", 10000.00, 31);

        new TimeDepositCalculator(withGoldPlan).updateBalance(List.of(gold, basic));

        assertThat(gold.getBalance()).isEqualTo(10016.67);
        assertThat(basic.getBalance()).isEqualTo(10008.33);
    }
}
