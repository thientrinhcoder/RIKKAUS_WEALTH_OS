package com.rikkaus.wealth.liabilities.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The recurring payment and the next date one is due.
 *
 * <p>The amount and the frequency are a pair: both present or both absent, which {@code
 * LiabilityService} enforces with a named field and the table enforces with a check constraint. The
 * next payment date stands alone, because a credit card has a due date without a fixed recurring
 * amount, and may be in the past, because an overdue payment is a state the design shows rather than
 * an input error.
 *
 * <p>The amount is the total due each period. It is deliberately not split into principal and
 * interest and nothing here expands it into dated cash flows — that is issues #74 and #77.
 */
public record PaymentSchedule(
        BigDecimal recurringPaymentAmount,
        PaymentFrequency recurringPaymentFrequency,
        LocalDate nextPaymentDate) {}
