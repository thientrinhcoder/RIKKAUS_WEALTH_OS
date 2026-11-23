import type { DecimalString } from './decimal';
import { displaysNegative, formatDecimal } from './format-number';

/** The two currencies the MVP accepts. VND is the reporting currency; USD is entry only. */
export const CURRENCIES = ['VND', 'USD'] as const;

export type CurrencyCode = (typeof CURRENCIES)[number];

export const REPORTING_CURRENCY: CurrencyCode = 'VND';

interface CurrencyDisplay {
  /** Shown after the amount. USD is written as its code, never an ambiguous `$`. */
  unit: string;
  /** What a screen reader says for the unit. */
  spokenUnit: string;
  maxFractionDigits: number;
  fractionStyle: 'trim' | 'all-or-none';
}

/**
 * VND is shown in whole dong: a manual valuation does not support more precision, and the stored
 * value keeps its two decimals untouched. USD shows cents only when there are some.
 */
const CURRENCY_DISPLAY: Record<CurrencyCode, CurrencyDisplay> = {
  VND: { unit: '₫', spokenUnit: 'đồng', maxFractionDigits: 0, fractionStyle: 'trim' },
  USD: { unit: 'USD', spokenUnit: 'đô la Mỹ', maxFractionDigits: 2, fractionStyle: 'all-or-none' },
};

export function isCurrencyCode(value: unknown): value is CurrencyCode {
  return typeof value === 'string' && (CURRENCIES as readonly string[]).includes(value);
}

export function currencyUnit(currency: CurrencyCode): string {
  return CURRENCY_DISPLAY[currency].unit;
}

/** `12.002.000.000`, `1.250,50` — the number alone, without its unit. */
export function formatAmount(amount: DecimalString, currency: CurrencyCode): string {
  return formatDecimal(amount, CURRENCY_DISPLAY[currency]);
}

/** `12.002.000.000 ₫`, `20.000 USD`, `-1.245.000.000 ₫`. */
export function formatMoney(amount: DecimalString, currency: CurrencyCode): string {
  return `${formatAmount(amount, currency)} ${CURRENCY_DISPLAY[currency].unit}`;
}

/**
 * What assistive technology reads: the value and its unit in words, with the sign spoken
 * (`âm 1.245.000.000 đồng`) because a leading hyphen is often skipped.
 */
export function moneyAccessibilityLabel(amount: DecimalString, currency: CurrencyCode): string {
  const display = CURRENCY_DISPLAY[currency];
  const text = formatAmount(amount, currency);
  const negative = displaysNegative(amount, display.maxFractionDigits);

  return `${negative ? `âm ${text.slice(1)}` : text} ${display.spokenUnit}`;
}
