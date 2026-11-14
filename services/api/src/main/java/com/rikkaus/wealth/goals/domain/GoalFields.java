package com.rikkaus.wealth.goals.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Everything about a goal the user enters.
 *
 * <p>One type for both create and replace, so the editable set is defined once and the two paths
 * cannot drift into accepting different fields. Unlike an asset, every user-entered field of a goal
 * is editable, including the type: the accepted design makes a template nothing but a set of
 * suggested values, and no stored field depends on which template was chosen, so freezing it would
 * protect nothing and would force a user who picked the wrong template to delete and start over.
 *
 * <p>{@code currentAllocatedAmount} is never {@code null} once normalized — a goal with nothing set
 * aside has {@code 0.00} allocated — while {@code plannedMonthlyContribution} and {@code
 * expectedAnnualReturnPercent} may be, because the design treats "not stated" and "zero" as different
 * answers for both.
 */
public record GoalFields(
        String name,
        GoalType type,
        BigDecimal targetAmount,
        BigDecimal currentAllocatedAmount,
        LocalDate targetDate,
        BigDecimal plannedMonthlyContribution,
        BigDecimal expectedAnnualReturnPercent) {}
