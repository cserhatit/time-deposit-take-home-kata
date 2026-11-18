package org.ikigaidigital.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Spring Data repository for {@link TimeDepositEntity}.
 */
interface TimeDepositJpaRepository extends JpaRepository<TimeDepositEntity, Integer> {

    List<TimeDepositEntity> findAllByOrderByIdAsc();

    // One query for deposits and withdrawals (no N+1); withdrawals ordered by @OrderBy("id").
    @Query("select d from TimeDepositEntity d left join fetch d.withdrawals order by d.id")
    List<TimeDepositEntity> findAllWithWithdrawals();
}
