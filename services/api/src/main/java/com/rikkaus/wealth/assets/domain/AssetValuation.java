package com.rikkaus.wealth.assets.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What an asset is worth, in which currency, as of when.
 *
 * <p>The three travel together because none of them means anything alone: a value without its
 * currency cannot be summed and a value without its date cannot be judged stale. Grouping them is
 * also what makes the boundary with issue #57 a type rather than a comment — an asset takes one of
 * these when it is created and has no method that accepts another, so overwriting a valuation is not
 * something a later edit could do by accident.
 *
 * <p>{@link BigDecimal} and never a floating-point type: this value is summed across a portfolio and
 * multiplied by a user-declared exchange rate.
 */
public record AssetValuation(
        BigDecimal currentValue, AssetCurrency currency, LocalDate valuationDate) {}
