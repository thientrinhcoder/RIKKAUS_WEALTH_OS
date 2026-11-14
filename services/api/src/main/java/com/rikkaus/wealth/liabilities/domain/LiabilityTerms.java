package com.rikkaus.wealth.liabilities.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The terms a lender set: the nominal annual rate and the two dates that bound the loan.
 *
 * <p>Every member may be {@code null}. A margin loan has no maturity, a debt to a relative has no
 * rate, and a card balance has neither. The accepted design records what the user states and does
 * not infer a missing term from the category.
 *
 * <p>The rate is a percentage per year held as {@link BigDecimal}, never a {@code double}: {@code
 * ArchitectureRulesTest} fails the build on a floating-point field whose name contains "rate", and
 * for the right reason — a rate quoted as {@code 8.5} must read back as {@code 8.5}.
 */
public record LiabilityTerms(BigDecimal interestRate, LocalDate startDate, LocalDate maturityDate) {}
