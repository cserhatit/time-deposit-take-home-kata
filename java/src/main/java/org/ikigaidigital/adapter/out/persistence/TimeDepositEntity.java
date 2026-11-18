package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.List;

/**
 * JPA mapping of the {@code timeDeposits} table. Never leaves the persistence adapter.
 */
@Entity
@Table(name = "timeDeposits")
class TimeDepositEntity {

    @Id
    private Integer id;

    private String planType;

    private int days;

    private BigDecimal balance;

    @OneToMany(mappedBy = "timeDeposit")
    @OrderBy("id")
    private List<WithdrawalEntity> withdrawals;

    protected TimeDepositEntity() {
        // for JPA
    }

    Integer getId() {
        return id;
    }

    String getPlanType() {
        return planType;
    }

    int getDays() {
        return days;
    }

    BigDecimal getBalance() {
        return balance;
    }

    void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    List<WithdrawalEntity> getWithdrawals() {
        return withdrawals;
    }
}
