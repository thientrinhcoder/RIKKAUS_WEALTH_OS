import { formatDate, parseIsoDate, parseInstant, type IsoDate } from '../time';

import { fromApiValue, multiplyDecimal, type DecimalString } from './decimal';
import { formatDecimal } from './format-number';
import { REPORTING_CURRENCY, type CurrencyCode } from './format-money';

/**
 * The USD→VND rate and the conversion it drives.
 *
 * The feature criterion is "Conversion giữ rate và effective date": a converted figure is never
 * shown or passed on without the rate and the date it was converted at, so the conversion result
 * carries both and the display helpers take the whole snapshot rather than a bare number.
 */
export interface ExchangeRateSnapshot {
  usdToVndRate: DecimalString;
  effectiveDate: IsoDate;
}

/** `GET /api/v1/settings/exchange-rate` as the API sends it. */
export interface ExchangeRateSettingsPayload {
  baseCurrency: unknown;
  usdToVndRate: unknown;
  effectiveDate: unknown;
  updatedAt: unknown;
}

export interface ExchangeRateSettings {
  /** `null` until the user declares a rate: the designed rate-needed state, not an error. */
  rate: ExchangeRateSnapshot | null;
  /** When the server stored the declaration — a system timestamp, distinct from the date. */
  updatedAt: Date | null;
}

/** VND results are written at two decimals, rounded half-up, as the backend's reports are. */
const CONVERSION_FRACTION_DIGITS = 2;

export const RATE_MISSING_REASON = 'Chưa có tỷ giá USD/VND';

export function readExchangeRateSettings(payload: ExchangeRateSettingsPayload): ExchangeRateSettings {
  const usdToVndRate = fromApiValue(payload.usdToVndRate);
  const effectiveDate = parseIsoDate(payload.effectiveDate);

  return {
    rate:
      usdToVndRate === null || effectiveDate === null ? null : { usdToVndRate, effectiveDate },
    updatedAt: parseInstant(payload.updatedAt),
  };
}

/** `25.100`, `25.100,5` — a rate keeps up to its six stored decimals, trailing zeros dropped. */
export function formatRateValue(rate: DecimalString): string {
  return formatDecimal(rate, { maxFractionDigits: 6, fractionStyle: 'trim' });
}

/** `1 USD = 25.100 VND`. */
export function formatExchangeRate(snapshot: ExchangeRateSnapshot): string {
  return `1 USD = ${formatRateValue(snapshot.usdToVndRate)} VND`;
}

/** `25.100 VND/USD`, the unit form used inside a conversion trace. */
export function formatRatePerUnit(snapshot: ExchangeRateSnapshot): string {
  return `${formatRateValue(snapshot.usdToVndRate)} VND/USD`;
}

/** `hiệu lực 12/09/2026`. */
export function formatRateEffective(snapshot: ExchangeRateSnapshot): string {
  return `hiệu lực ${formatDate(snapshot.effectiveDate)}`;
}

export type Conversion =
  | {
      status: 'reporting';
      /** Already in VND; nothing was converted and no rate applies. */
      amount: DecimalString;
    }
  | {
      status: 'converted';
      original: { amount: DecimalString; currency: CurrencyCode };
      rate: ExchangeRateSnapshot;
      amount: DecimalString;
    }
  | {
      status: 'unavailable';
      original: { amount: DecimalString; currency: CurrencyCode };
      reason: string;
    };

/**
 * Converts an amount into the reporting currency with exact decimal arithmetic. A USD amount
 * without a declared rate is `unavailable` with its reason — never converted at zero.
 */
export function convertToReporting(
  amount: DecimalString,
  currency: CurrencyCode,
  rate: ExchangeRateSnapshot | null,
): Conversion {
  if (currency === REPORTING_CURRENCY) {
    return { status: 'reporting', amount };
  }

  const original = { amount, currency };

  if (rate === null) {
    return { status: 'unavailable', original, reason: RATE_MISSING_REASON };
  }

  return {
    status: 'converted',
    original,
    rate,
    amount: multiplyDecimal(amount, rate.usdToVndRate, CONVERSION_FRACTION_DIGITS),
  };
}
