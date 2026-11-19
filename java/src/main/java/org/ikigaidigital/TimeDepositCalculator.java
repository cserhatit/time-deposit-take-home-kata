package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.ikigaidigital.calculation.InterestCalculationStrategy;
import org.ikigaidigital.calculation.standard.StandardInterestCalculation;

/**
 * Applies interest to time deposits.
 *
 * <p>
 * The interest rule comes from an {@link InterestCalculationStrategy}; by default the {@link StandardInterestCalculation} (one month at the plan's
 * annual rate). This class rounds the interest (to two decimal places, {@link RoundingMode#HALF_UP}) and adds it to the balance; the balance itself
 * is never rounded.
 */
public class TimeDepositCalculator {

    private static final int DECIMAL_PLACES = 2;

    private final InterestCalculationStrategy interestCalculation;

    /**
     * Creates a calculator with the {@link StandardInterestCalculation}.
     */
    public TimeDepositCalculator() {
        this(new StandardInterestCalculation());
    }

    /**
     * Creates a calculator with a custom interest rule.
     *
     * @param interestCalculation
     *            the interest rule to apply
     */
    public TimeDepositCalculator(InterestCalculationStrategy interestCalculation) {
        this.interestCalculation = Objects.requireNonNull(interestCalculation, "interestCalculation");
    }

    /**
     * Adds interest to the balance of every given deposit, in place.
     *
     * <p>
     * Not idempotent: every call adds interest again, and {@code days} is not changed. With the standard rule, every call adds one month of
     * interest and unknown plan types (matched case-sensitively) earn none.
     *
     * @param deposits
     *            the deposits to update; each one's balance is replaced
     */
    public void updateBalance(List<TimeDeposit> deposits) {
        for (TimeDeposit deposit : deposits) {
            double interest = roundToTwoDecimals(interestCalculation.calculateInterest(deposit));
            deposit.setBalance(deposit.getBalance() + interest);
        }
    }

    // new BigDecimal(double) on purpose: it rounds the exact binary value (0.015 -> 0.01), unlike BigDecimal.valueOf.
    private static double roundToTwoDecimals(double amount) {
        return new BigDecimal(amount).setScale(DECIMAL_PLACES, RoundingMode.HALF_UP).doubleValue();
    }
}
