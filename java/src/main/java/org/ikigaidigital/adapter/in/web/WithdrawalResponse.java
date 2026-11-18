package org.ikigaidigital.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import org.ikigaidigital.Withdrawal;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A withdrawal as returned by the API.
 *
 * @param id     the withdrawal identifier
 * @param amount the withdrawn amount
 * @param date   the date of the withdrawal
 */
public record WithdrawalResponse(
        @Schema(example = "1") int id,
        @Schema(example = "100.00") BigDecimal amount,
        @Schema(example = "2026-01-10") LocalDate date) {

    static WithdrawalResponse from(Withdrawal withdrawal) {
        return new WithdrawalResponse(withdrawal.id(), withdrawal.amount(), withdrawal.date());
    }
}
