package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.Withdrawal;

import java.util.List;

/**
 * Read model: a time deposit together with its withdrawals.
 *
 * @param deposit     the time deposit
 * @param withdrawals its withdrawals ordered by id; empty if there are none
 */
public record TimeDepositWithWithdrawals(TimeDeposit deposit, List<Withdrawal> withdrawals) {
}
