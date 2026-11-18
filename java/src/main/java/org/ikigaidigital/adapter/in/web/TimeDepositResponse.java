package org.ikigaidigital.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.TimeDepositWithWithdrawals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * A time deposit as returned by {@code GET /time-deposits}.
 *
 * @param id          the deposit identifier
 * @param planType    the plan type, e.g. {@code basic}, {@code student} or {@code premium}
 * @param balance     the balance with two decimal places
 * @param days        the age of the deposit in days
 * @param withdrawals the withdrawals ordered by id; empty if there are none
 */
public record TimeDepositResponse(
        @Schema(example = "1") int id,
        @Schema(example = "basic") String planType,
        @Schema(example = "10008.33") BigDecimal balance,
        @Schema(example = "31") int days,
        List<WithdrawalResponse> withdrawals) {

    private static final int DECIMAL_PLACES = 2;

    static TimeDepositResponse from(TimeDepositWithWithdrawals source) {
        TimeDeposit deposit = source.deposit();
        return new TimeDepositResponse(
                deposit.getId(),
                deposit.getPlanType(),
                BigDecimal.valueOf(deposit.getBalance()).setScale(DECIMAL_PLACES, RoundingMode.HALF_UP),
                deposit.getDays(),
                source.withdrawals().stream().map(WithdrawalResponse::from).toList());
    }
}
