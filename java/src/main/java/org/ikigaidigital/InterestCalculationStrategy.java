package org.ikigaidigital;

/**
 * Extension point for interest calculation rules.
 *
 * <p>A new rule (e.g. tiered rates or a different accrual method) is added as a new implementation, without
 * changing {@link TimeDepositCalculator} or {@link StandardInterestCalculation}. An implementation may delegate
 * to {@link StandardInterestCalculation} for the deposits it does not handle itself.
 *
 * <p>{@link TimeDepositCalculator} rounds the result and applies it to the balance; implementations return the
 * unrounded amount.
 */
@FunctionalInterface
public interface InterestCalculationStrategy {

    /**
     * Calculates the interest to add to the deposit for one balance update.
     *
     * @param deposit the deposit to calculate interest for; must not be modified
     * @return the unrounded interest, or {@code 0} if the deposit earns none
     */
    double calculateInterest(TimeDeposit deposit);
}
