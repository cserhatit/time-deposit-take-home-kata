package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;

import java.util.List;

/**
 * Output port: the storage operations the application needs for time deposits.
 */
public interface TimeDepositRepository {

    /**
     * Loads all time deposits, without withdrawals, ordered by id.
     *
     * @return all time deposits
     */
    List<TimeDeposit> findAll();

    /**
     * Stores the current balance of each given deposit, rounded to two decimal places.
     *
     * <p>Intended to run in the same transaction as the preceding {@link #findAll()}.
     *
     * @param deposits deposits previously returned by {@link #findAll()}
     */
    void saveBalances(List<TimeDeposit> deposits);

    /**
     * Loads all time deposits with their withdrawals, ordered by id.
     *
     * @return all time deposits with their withdrawals
     */
    List<TimeDepositWithWithdrawals> findAllWithWithdrawals();
}
