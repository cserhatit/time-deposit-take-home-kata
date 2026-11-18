package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use cases for time deposits: applying monthly interest to all deposits and listing them.
 */
@Service
public class TimeDepositService {

    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    /**
     * Creates the service.
     *
     * @param repository the time deposit storage
     * @param calculator the legacy interest calculator, used unchanged
     */
    public TimeDepositService(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    /**
     * Applies one month of interest to every stored time deposit.
     *
     * <p>Load, calculation and save run in one short transaction, so the update is all or nothing.
     * Not idempotent: every call applies another month. No locking: two concurrent calls can lose one
     * month (lost update); concurrency control is outside the README requirements.
     */
    @Transactional
    public void updateAllBalances() {
        List<TimeDeposit> deposits = repository.findAll();
        calculator.updateBalance(deposits);
        repository.saveBalances(deposits);
    }

    /**
     * Returns all time deposits with their withdrawals, ordered by id.
     *
     * <p>Deliberately not {@code @Transactional}: the persistence adapter loads everything in one query.
     *
     * @return all time deposits with their withdrawals
     */
    public List<TimeDepositWithWithdrawals> getAllTimeDeposits() {
        return repository.findAllWithWithdrawals();
    }
}
