package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Applies one month of interest to time deposits according to their {@link PlanType}.
 *
 * <p>
 * Annual rates are applied monthly ({@code rate / 12}) once a plan's interest-free days have passed. Only the interest is rounded (to two decimal
 * places, {@link RoundingMode#HALF_UP}); the balance itself is never rounded.
 */
public class TimeDepositCalculator {

    private static final int MONTHS_PER_YEAR = 12;
    private static final int DECIMAL_PLACES = 2;

    /**
     * Adds one month of interest to the balance of every given deposit, in place.
     *
     * <p>
     * Not idempotent: every call adds another month of interest, and {@code days} is not changed. Unknown plan types (matched case-sensitively) earn
     * no interest.
     *
     * @param deposits
     *            the deposits to update; each one's balance is replaced
     */
    public void updateBalance(List<TimeDeposit> deposits) {
        for (TimeDeposit deposit : deposits) {
            double interest = roundToTwoDecimals(monthlyInterest(deposit));
            deposit.setBalance(deposit.getBalance() + interest);
        }
    }

    private static double monthlyInterest(TimeDeposit deposit) {
        return PlanType.fromCode(deposit.getPlanType())
                .filter(planType -> planType.earnsInterestOn(deposit.getDays()))
                .map(planType -> monthlyInterest(deposit.getBalance(), planType.annualRate()))
                .orElse(0.0);
    }

    /**
     * Calculates one month's interest using the annual interest rate.
     *
     * <p>
     * The original operation order (balance * rate, then / 12) is preserved because floating-point arithmetic is not associative. Reordering the
     * operations may introduce different rounding errors, potentially affecting the final interest amount after rounding to two decimals.
     */
    private static double monthlyInterest(double balance, double annualRate) {
        return balance * annualRate / MONTHS_PER_YEAR;
    }

    // new BigDecimal(double) on purpose: it rounds the exact binary value (0.015 -> 0.01), unlike BigDecimal.valueOf.
    private static double roundToTwoDecimals(double amount) {
        return new BigDecimal(amount).setScale(DECIMAL_PLACES, RoundingMode.HALF_UP).doubleValue();
    }
}
