package com.rikkaus.wealth.cashflow.forecast.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Expands a liability's stated schedule into the payment dates inside a window.
 *
 * <p>The same rules {@code CashFlowSchedule} applies to a recurring entry, with the next payment date
 * as the anchor: payment {@code n} is the anchor plus {@code n} steps counted from the anchor, a day of
 * month the month lacks moves back to that month's last day and the next month returns to the anchor's
 * day, and the maturity date is the last day a payment may fall on. A pure function of calendar dates,
 * so the result does not depend on the server's zone or clock.
 *
 * <p>An anchor in the past is still the anchor. Payments it produces before the window are not added
 * to the forecast: an overdue payment is a fact about today's balance, not a projection.
 */
public final class LiabilityPaymentSchedule {

    private LiabilityPaymentSchedule() {}

    /** One payment date, with its position counted from the anchor and whether it was moved. */
    public record Payment(LocalDate date, int sequence, boolean shiftedToMonthEnd) {}

    /** The calendar months between two payments for each frequency {@code V7__liabilities.sql} allows. */
    public static int monthsBetweenPayments(String frequency) {
        return switch (frequency) {
            case "MONTHLY" -> 1;
            case "QUARTERLY" -> 3;
            case "SEMIANNUAL" -> 6;
            case "YEARLY" -> 12;
            default -> throw new IllegalStateException("unknown payment frequency " + frequency);
        };
    }

    public static List<Payment> paymentsOf(
            LocalDate anchor, int step, LocalDate maturityDate, LocalDate from, LocalDate to) {
        LocalDate last = maturityDate == null || maturityDate.isAfter(to) ? to : maturityDate;
        List<Payment> payments = new ArrayList<>();
        // Starts one step before the first payment that can be in the window, for the reason
        // CashFlowSchedule.firstCandidate gives: MONTHS.between never overstates the distance.
        long first = Math.max(0, ChronoUnit.MONTHS.between(anchor, from) / step - 1);
        for (long n = first; ; n++) {
            LocalDate date = anchor.plusMonths(n * step);
            if (date.isAfter(last)) {
                return payments;
            }
            if (!date.isBefore(from)) {
                payments.add(new Payment(
                        date, Math.toIntExact(n + 1), date.getDayOfMonth() != anchor.getDayOfMonth()));
            }
        }
    }
}
