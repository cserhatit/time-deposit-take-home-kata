package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.Withdrawal;
import org.ikigaidigital.application.TimeDepositRepository;
import org.ikigaidigital.application.TimeDepositWithWithdrawals;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Implements {@link TimeDepositRepository} with Spring Data JPA.
 *
 * <p>This is the money boundary: the domain calculates with {@code double}, the database stores
 * {@code NUMERIC(19,2)}. Entities are mapped to domain objects here and never leave this package.
 */
@Component
class TimeDepositPersistenceAdapter implements TimeDepositRepository {

    private static final int STORED_DECIMAL_PLACES = 2;

    private final TimeDepositJpaRepository jpaRepository;

    TimeDepositPersistenceAdapter(TimeDepositJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<TimeDeposit> findAll() {
        return jpaRepository.findAllByOrderByIdAsc().stream()
                .map(TimeDepositPersistenceAdapter::toDomain)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Joins the caller's transaction. There, {@code getReferenceById} returns the entity already loaded by
     * {@link #findAll()} without another query, and the changes are written as UPDATEs on commit.
     */
    @Override
    @Transactional
    public void saveBalances(List<TimeDeposit> deposits) {
        for (TimeDeposit deposit : deposits) {
            jpaRepository.getReferenceById(deposit.getId()).setBalance(toStoredBalance(deposit.getBalance()));
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Deposits and withdrawals are fetched in one query and mapped before returning, so nothing lazy is
     * accessed after the session closes.
     */
    @Override
    public List<TimeDepositWithWithdrawals> findAllWithWithdrawals() {
        return jpaRepository.findAllWithWithdrawals().stream()
                .map(entity -> new TimeDepositWithWithdrawals(
                        toDomain(entity),
                        entity.getWithdrawals().stream().map(TimeDepositPersistenceAdapter::toDomain).toList()))
                .toList();
    }

    private static TimeDeposit toDomain(TimeDepositEntity entity) {
        return new TimeDeposit(entity.getId(), entity.getPlanType(), entity.getBalance().doubleValue(), entity.getDays());
    }

    private static Withdrawal toDomain(WithdrawalEntity entity) {
        return new Withdrawal(entity.getId(), entity.getAmount(), entity.getDate());
    }

    // The calculator's raw double can carry binary noise (e.g. 1237654.3099999998); the NUMERIC(19,2) column
    // stores it rounded explicitly to two decimals (1237654.31). The next update calculates from the stored value.
    private static BigDecimal toStoredBalance(double balance) {
        return BigDecimal.valueOf(balance).setScale(STORED_DECIMAL_PLACES, RoundingMode.HALF_UP);
    }
}
