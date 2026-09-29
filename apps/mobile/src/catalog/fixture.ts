/**
 * The section 16.3 mock data baseline, as display fixture data for the component catalog.
 *
 * Every aggregate reconciles with its source records. A catalog whose numbers do not add up
 * teaches feature teams that they need not either, which is the habit section 16.3 exists to
 * prevent. The reconciliation is asserted in __tests__/catalog/fixture.test.ts.
 *
 * This is fixture data for examples. It is not business logic and nothing in src/features/
 * imports it.
 *
 * It sits outside src/app/ because Expo Router publishes every file under that tree as a route,
 * including ones that export no component. A leading underscore does not exempt it; only moving
 * it out does.
 */

export const EXCHANGE_RATE = {
  pair: '1 USD = 25.100 VND',
  rate: 25_100,
  updatedOn: '12/09/2026',
} as const;

export interface FixtureRecord {
  id: string;
  title: string;
  category: string;
  /** In VND. */
  amount: number;
  /** The original-currency line, where the record was entered in another currency. */
  originalAmount?: string;
  date: string;
}

export const ASSETS: readonly FixtureRecord[] = [
  {
    id: 'cash',
    title: 'Tiền mặt và tài khoản ngân hàng',
    category: 'Tiền mặt',
    amount: 650_000_000,
    date: 'Cập nhật 12/09/2026',
  },
  {
    id: 'term-deposit',
    title: 'Tiền gửi có kỳ hạn Vietcombank',
    category: 'Tiền gửi tiết kiệm',
    amount: 1_200_000_000,
    date: 'Đáo hạn 15/03/2027',
  },
  {
    id: 'securities',
    title: 'Danh mục cổ phiếu niêm yết',
    category: 'Chứng khoán',
    amount: 2_150_000_000,
    date: 'Cập nhật 12/09/2026',
  },
  {
    id: 'real-estate',
    title: 'Căn hộ Thảo Điền, Thành phố Thủ Đức',
    category: 'Bất động sản',
    amount: 7_500_000_000,
    date: 'Định giá 15/03/2026',
  },
  {
    id: 'usd-deposit',
    title: 'Tiền gửi ngoại tệ',
    category: 'Tiền gửi tiết kiệm',
    amount: 502_000_000,
    originalAmount: '20.000,00 USD',
    date: 'Cập nhật 12/09/2026',
  },
];

export const LIABILITIES: readonly FixtureRecord[] = [
  {
    id: 'mortgage',
    title: 'Vay mua nhà Techcombank',
    category: 'Vay thế chấp',
    amount: 1_500_000_000,
    date: 'Kỳ tới 05/10/2026',
  },
  {
    id: 'credit-card',
    title: 'Dư nợ thẻ tín dụng',
    category: 'Tín dụng tiêu dùng',
    amount: 32_000_000,
    date: 'Đến hạn 20/10/2026',
  },
];

export const MONTHLY_INCOME = 180_000_000;
export const MONTHLY_EXPENSES = 92_000_000;

function sum(records: readonly FixtureRecord[]): number {
  return records.reduce((total, record) => total + record.amount, 0);
}

export const TOTAL_ASSETS = sum(ASSETS);
export const TOTAL_LIABILITIES = sum(LIABILITIES);
export const NET_WORTH = TOTAL_ASSETS - TOTAL_LIABILITIES;
export const NET_CASH_FLOW = MONTHLY_INCOME - MONTHLY_EXPENSES;

/** The record whose valuation is out of date, used by the stale-state example. */
export const STALE_ASSET = ASSETS[3];

/** The next payment due, used by the upcoming-obligation example. */
export const UPCOMING_PAYMENT = LIABILITIES[0];

/**
 * Formats an amount for display using the same grouping the form kit applies on entry, so the
 * catalog reads the way the product will.
 */
export function formatVnd(amount: number): string {
  return `${amount.toLocaleString('de-DE')} ₫`;
}
