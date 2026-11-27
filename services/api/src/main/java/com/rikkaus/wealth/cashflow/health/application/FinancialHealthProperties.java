package com.rikkaus.wealth.cashflow.health.application;

import com.rikkaus.wealth.cashflow.domain.CashFlowCategory;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryType;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The system reference values the five financial-health indicators are banded by and the six insight
 * rules are judged against, and the expense categories counted as essential.
 *
 * <p>The accepted design for issue #81 shows these values as a review fixture, not as an approved
 * business configuration, which is why they live in configuration in one place: changing one is a
 * deployment decision, not a code change, and every response states the values it was judged with, so
 * a result can be reproduced after the configuration changes.
 *
 * <p>There is no default in this record. The values are in {@code application.yml}, and a missing or
 * meaningless value stops the application at start-up with the property named, because an indicator
 * banded by a threshold nobody chose would be a silent product decision. A value equal to a limit is not
 * beyond it.
 *
 * @param reserveOkMinMonths the reserve in months at and above which the reserve is {@code OK}
 * @param minReserveMonths the reserve in months below which it is {@code ATTENTION} and the reserve rule
 *     fires; between the two it is {@code WATCH}
 * @param debtBurdenOkMax the debt-payment burden at and below which the burden is {@code OK}
 * @param maxDebtPaymentBurden the burden above which it is {@code ATTENTION} and the burden rule fires
 * @param passiveIncomeHighMin the passive-income share at and above which it is {@code HIGH}
 * @param passiveIncomeMediumMin the share at and above which it is {@code MEDIUM}; below it, {@code LOW}
 * @param essentialShareOkMax the essential-expense share at and below which it is {@code OK}
 * @param essentialShareWatchMax the share at and below which it is {@code WATCH}; above it, {@code
 *     ATTENTION}
 * @param dueWindowDays how many days after today an obligation still counts as upcoming
 * @param maxIncomeSourceShare the largest income category's share above which the concentration rule
 *     fires
 * @param essentialCategories the expense categories counted as essential
 */
@ConfigurationProperties("rikkaus.cashflow.health")
public record FinancialHealthProperties(
        BigDecimal reserveOkMinMonths,
        BigDecimal minReserveMonths,
        BigDecimal debtBurdenOkMax,
        BigDecimal maxDebtPaymentBurden,
        BigDecimal passiveIncomeHighMin,
        BigDecimal passiveIncomeMediumMin,
        BigDecimal essentialShareOkMax,
        BigDecimal essentialShareWatchMax,
        Integer dueWindowDays,
        BigDecimal maxIncomeSourceShare,
        List<CashFlowCategory> essentialCategories) {

    private static final String PREFIX = "rikkaus.cashflow.health.";

    /** Fractions carry six decimals; a threshold with more could not be told apart from them. */
    static final int FRACTION_SCALE = 6;

    /** The reserve is shown in months at one decimal, so a threshold carries at most one. */
    static final int MONTHS_SCALE = 1;

    /** Ten years. A longer window is not a threshold anyone would choose, only a typo. */
    private static final int MAX_DAYS = 3650;

    /** A hundred years of expenses. A larger reserve threshold is a typo, not a reference value. */
    private static final BigDecimal MAX_MONTHS = BigDecimal.valueOf(1200);

    public FinancialHealthProperties {
        months(reserveOkMinMonths, "reserve-ok-min-months");
        months(minReserveMonths, "min-reserve-months");
        notAbove(minReserveMonths, reserveOkMinMonths, "min-reserve-months", "reserve-ok-min-months");
        positiveFraction(debtBurdenOkMax, "debt-burden-ok-max");
        positiveFraction(maxDebtPaymentBurden, "max-debt-payment-burden");
        notAbove(debtBurdenOkMax, maxDebtPaymentBurden, "debt-burden-ok-max", "max-debt-payment-burden");
        share(passiveIncomeHighMin, "passive-income-high-min");
        share(passiveIncomeMediumMin, "passive-income-medium-min");
        notAbove(passiveIncomeMediumMin, passiveIncomeHighMin, "passive-income-medium-min", "passive-income-high-min");
        share(essentialShareOkMax, "essential-share-ok-max");
        share(essentialShareWatchMax, "essential-share-watch-max");
        notAbove(essentialShareOkMax, essentialShareWatchMax, "essential-share-ok-max", "essential-share-watch-max");
        days(dueWindowDays, "due-window-days");
        share(maxIncomeSourceShare, "max-income-source-share");
        essentialCategories = expenseCategories(essentialCategories, "essential-categories");
    }

    /** A share of a total can never exceed one, so a limit above one would never be reached. */
    private static void share(BigDecimal value, String name) {
        positiveFraction(value, name);
        if (value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException(PREFIX + name + " must not be greater than 1.");
        }
    }

    /** Debt payments may exceed income, so the burden has no upper bound; it is still a positive fraction. */
    private static void positiveFraction(BigDecimal value, String name) {
        positive(value, name);
        if (value.stripTrailingZeros().scale() > FRACTION_SCALE) {
            throw new IllegalStateException(
                    PREFIX + name + " must have at most " + FRACTION_SCALE + " decimal places.");
        }
    }

    private static void months(BigDecimal value, String name) {
        positive(value, name);
        if (value.stripTrailingZeros().scale() > MONTHS_SCALE) {
            throw new IllegalStateException(PREFIX + name + " must have at most " + MONTHS_SCALE + " decimal place.");
        }
        if (value.compareTo(MAX_MONTHS) > 0) {
            throw new IllegalStateException(PREFIX + name + " must not be greater than " + MAX_MONTHS + ".");
        }
    }

    private static void positive(BigDecimal value, String name) {
        if (value == null) {
            throw new IllegalStateException(PREFIX + name + " is not set.");
        }
        if (value.signum() <= 0) {
            throw new IllegalStateException(PREFIX + name + " must be greater than zero.");
        }
    }

    /** The two limits of one band rail must not cross, or a band would be empty and another overlap. */
    private static void notAbove(BigDecimal lower, BigDecimal upper, String lowerName, String upperName) {
        if (lower.compareTo(upper) > 0) {
            throw new IllegalStateException(
                    PREFIX + lowerName + " must not be greater than " + PREFIX + upperName + ".");
        }
    }

    private static void days(Integer value, String name) {
        if (value == null) {
            throw new IllegalStateException(PREFIX + name + " is not set.");
        }
        if (value < 0 || value > MAX_DAYS) {
            throw new IllegalStateException(PREFIX + name + " must be between 0 and " + MAX_DAYS + " days.");
        }
    }

    /**
     * At least one category, each an expense category and none twice; returned in the order {@link
     * CashFlowCategory} declares, so the response lists them the same way whatever order the
     * configuration used.
     */
    private static List<CashFlowCategory> expenseCategories(List<CashFlowCategory> value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(PREFIX + name + " is not set.");
        }
        for (CashFlowCategory category : value) {
            if (category == null) {
                throw new IllegalStateException(PREFIX + name + " must not contain an empty entry.");
            }
            if (category.entryType() != CashFlowEntryType.EXPENSE) {
                throw new IllegalStateException(
                        PREFIX + name + " must list expense categories only; " + category + " is an income category.");
            }
        }
        if (EnumSet.copyOf(value).size() != value.size()) {
            throw new IllegalStateException(PREFIX + name + " must not list a category twice.");
        }
        return value.stream().sorted().toList();
    }
}
