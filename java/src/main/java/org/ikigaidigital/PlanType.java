package org.ikigaidigital;

import java.util.Arrays;
import java.util.Optional;

/**
 * The supported time deposit plan types, the exact codes used for them in {@link TimeDeposit#getPlanType()},
 * and their interest terms.
 */
public enum PlanType {
    BASIC("basic", 0.01, 30),
    STUDENT("student", 0.03, 30, 365),
    PREMIUM("premium", 0.05, 45);

    private final String code;
    private final double annualRate;
    private final int interestFreeDays;
    private final int lastInterestDay;

    // Plans without an end to their interest period.
    PlanType(String code, double annualRate, int interestFreeDays) {
        this(code, annualRate, interestFreeDays, Integer.MAX_VALUE);
    }

    PlanType(String code, double annualRate, int interestFreeDays, int lastInterestDay) {
        this.code = code;
        this.annualRate = annualRate;
        this.interestFreeDays = interestFreeDays;
        this.lastInterestDay = lastInterestDay;
    }

    /**
     * Returns the annual interest rate, e.g. {@code 0.01} for 1%.
     *
     * @return the annual interest rate as a fraction
     */
    public double annualRate() {
        return annualRate;
    }

    /**
     * Tells whether a deposit of the given age earns interest under this plan: after its interest-free days
     * and up to and including its last interest day.
     *
     * @param days the age of the deposit in days
     * @return {@code true} if the deposit earns interest on that day
     */
    public boolean earnsInterestOn(int days) {
        return days > interestFreeDays && days <= lastInterestDay;
    }

    /**
     * Returns the plan type for an exact, case-sensitive code.
     *
     * <p>Unknown codes give an empty result instead of an exception, because legacy behavior is that
     * unknown plan types simply earn no interest.
     *
     * @param code the plan type code, e.g. {@code "basic"}; must not be {@code null}
     * @return the matching plan type, or empty if the code is unknown
     * @throws NullPointerException if {@code code} is {@code null}
     */
    public static Optional<PlanType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(planType -> code.equals(planType.code))
                .findFirst();
    }
}
