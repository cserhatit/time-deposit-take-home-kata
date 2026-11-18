package org.ikigaidigital;

/**
 * The standard (legacy) interest rule: one month of interest at the plan's annual rate ({@code rate / 12}),
 * once the plan's interest-free days have passed and up to its last interest day, as configured in {@link PlanType}.
 *
 * <p>Unknown plan types (matched case-sensitively) earn no interest.
 */
public class StandardInterestCalculation implements InterestCalculationStrategy {

    private static final int MONTHS_PER_YEAR = 12;

    /**
     * {@inheritDoc}
     *
     * @return one month of interest, or {@code 0} for unknown plan types and deposits outside the plan's interest window
     */
    @Override
    public double calculateInterest(TimeDeposit deposit) {
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
}
