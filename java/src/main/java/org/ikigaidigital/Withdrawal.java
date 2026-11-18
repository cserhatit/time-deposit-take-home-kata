package org.ikigaidigital;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A withdrawal from a time deposit. Withdrawals are informational and do not affect interest.
 *
 * @param id     the withdrawal identifier
 * @param amount the withdrawn amount
 * @param date   the date of the withdrawal
 */
public record Withdrawal(int id, BigDecimal amount, LocalDate date) {
}
