import type { DecimalString } from './decimal';
import { displaysNegative, formatDecimal } from './format-number';

/**
 * Percentages arrive already computed by the backend (`sharePercent` at one decimal, for
 * example); the client formats them and never derives a ratio itself. A ratio the backend could
 * not compute is `null` and renders as unavailable, never as `0%`.
 */

/** `18,4%`, `100%`, `-2,5%`. */
export function formatPercent(value: DecimalString, maxFractionDigits = 1): string {
  return `${formatDecimal(value, { maxFractionDigits, fractionStyle: 'trim' })}%`;
}

/** `18,4 phần trăm`, `âm 2,5 phần trăm`. */
export function percentAccessibilityLabel(value: DecimalString, maxFractionDigits = 1): string {
  const text = formatDecimal(value, { maxFractionDigits, fractionStyle: 'trim' });
  const negative = displaysNegative(value, maxFractionDigits);

  return `${negative ? `âm ${text.slice(1)}` : text} phần trăm`;
}
