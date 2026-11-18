package org.ikigaidigital;

/**
 * A time deposit whose balance grows with monthly interest.
 *
 * <p>Shared class: its public constructor and methods must stay backward compatible.
 * Only the balance can change after construction.
 */
public class TimeDeposit {
    private final int id;
    private final String planType;
    private Double balance;
    private final int days;

    /**
     * Creates a time deposit.
     *
     * @param id       the deposit identifier
     * @param planType the plan type, e.g. {@code "basic"}, {@code "student"} or {@code "premium"}
     * @param balance  the current balance
     * @param days     the age of the deposit in days
     */
    public TimeDeposit(int id, String planType, Double balance, int days) {
        this.id = id;
        this.planType = planType;
        this.balance = balance;
        this.days = days;
    }

    /**
     * Returns the deposit identifier.
     *
     * @return the deposit identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the plan type.
     *
     * @return the plan type
     */
    public String getPlanType() {
        return planType;
    }

    /**
     * Returns the current balance.
     *
     * @return the current balance
     */
    public Double getBalance() {
        return balance;
    }

    /**
     * Returns the age of the deposit in days.
     *
     * @return the age of the deposit in days
     */
    public int getDays() {
        return days;
    }

    /**
     * Replaces the balance.
     *
     * @param balance the new balance
     */
    public void setBalance(Double balance) {
        this.balance = balance;
    }
}
