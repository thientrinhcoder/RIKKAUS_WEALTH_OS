import {
  ASSETS,
  EXCHANGE_RATE,
  LIABILITIES,
  MONTHLY_EXPENSES,
  MONTHLY_INCOME,
  NET_CASH_FLOW,
  NET_WORTH,
  STALE_ASSET,
  TOTAL_ASSETS,
  TOTAL_LIABILITIES,
  UPCOMING_PAYMENT,
  formatVnd,
} from '@/catalog/fixture';

describe('the catalog fixture reconciles', () => {
  it('sums its asset records to the published total', () => {
    expect(TOTAL_ASSETS).toBe(12_002_000_000);
  });

  it('sums its liability records to the published total', () => {
    expect(TOTAL_LIABILITIES).toBe(1_532_000_000);
  });

  it('derives net worth from assets minus liabilities', () => {
    expect(NET_WORTH).toBe(10_470_000_000);
    expect(NET_WORTH).toBe(TOTAL_ASSETS - TOTAL_LIABILITIES);
  });

  it('derives net cash flow from income minus expenses', () => {
    expect(MONTHLY_INCOME).toBe(180_000_000);
    expect(MONTHLY_EXPENSES).toBe(92_000_000);
    expect(NET_CASH_FLOW).toBe(88_000_000);
  });

  it('converts the foreign-currency deposit at the stated rate', () => {
    const usdDeposit = ASSETS.find((asset) => asset.id === 'usd-deposit');

    expect(usdDeposit?.originalAmount).toBe('20.000,00 USD');
    expect(usdDeposit?.amount).toBe(20_000 * EXCHANGE_RATE.rate);
  });

  it('carries the exchange rate with the date it was entered', () => {
    expect(EXCHANGE_RATE.pair).toBe('1 USD = 25.100 VND');
    expect(EXCHANGE_RATE.updatedOn).toBe('12/09/2026');
  });
});

describe('the fixture covers the states the catalog must show', () => {
  it('includes a valuation that is out of date', () => {
    expect(STALE_ASSET.date).toBe('Định giá 15/03/2026');
    expect(STALE_ASSET.date).not.toContain('12/09/2026');
  });

  it('includes an upcoming payment', () => {
    expect(UPCOMING_PAYMENT.date).toMatch(/Kỳ tới|Đến hạn/);
  });

  it('gives every record a real Vietnamese title rather than a placeholder', () => {
    for (const record of [...ASSETS, ...LIABILITIES]) {
      expect(record.title).not.toMatch(/lorem|ipsum|foo|bar|test/i);
      expect(record.title.length).toBeGreaterThan(5);
    }
  });

  it('uses no round-number-only amounts across the whole fixture', () => {
    const amounts = [...ASSETS, ...LIABILITIES].map((record) => record.amount);

    expect(amounts.some((amount) => amount % 1_000_000_000 !== 0)).toBe(true);
  });
});

describe('display formatting', () => {
  it('groups amounts the Vietnamese way with the unit beside the value', () => {
    expect(formatVnd(12_002_000_000)).toBe('12.002.000.000 ₫');
    expect(formatVnd(88_000_000)).toBe('88.000.000 ₫');
  });
});
