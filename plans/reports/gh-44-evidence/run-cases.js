// Executes the F-route cases from plans/261122-1629-GH-44-money-time-primitives/test-cases.md.
const m = require('./primitives/money');
const t = require('./primitives/time');

let pass = 0;
let fail = 0;
function check(id, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected);
  ok ? pass++ : fail++;
  console.log(`${ok ? 'PASS' : 'FAIL'} ${id.padEnd(5)} ${JSON.stringify(actual)}${ok ? '' : `  expected ${JSON.stringify(expected)}`}`);
}
const D = m.decimal;
const parse = (s, o) => { const r = m.parseVietnameseDecimal(s, o); return r.error ?? r.value; };

console.log('-- 1. Decimal intake');
check('D1', m.fromApiValue(12002000000), '12002000000');
check('D2', m.fromApiValue(JSON.parse('45000000.50')), '45000000.5');
check('D3', m.fromApiValue('25100.000000'), '25100.000000');
check('D4', m.fromApiValue(-1245000000), '-1245000000');
check('D5', [m.fromApiValue(0), m.fromApiValue(-0)], ['0', '0']);
check('D6', [m.fromApiValue(null), m.fromApiValue(undefined)], [null, null]);
check('D7', m.fromApiValue(1e-7), '0.0000001');
check('D8', [m.fromApiValue(NaN), m.fromApiValue(Infinity)], [null, null]);
check('D9', [m.isPrecisionAtRisk(JSON.parse('9007199254740993')), m.isPrecisionAtRisk(12002000000)], [true, false]);
check('D10', [m.fromApiValue('12,5'), m.fromApiValue('abc')], [null, null]);

console.log('-- 2. Money display');
const show = (a, c) => [m.formatMoney(D(a), c), m.moneyAccessibilityLabel(D(a), c)];
check('M1', show('12002000000', 'VND'), ['12.002.000.000 ₫', '12.002.000.000 đồng']);
check('M2', show('380000000.00', 'VND'), ['380.000.000 ₫', '380.000.000 đồng']);
check('M3', show('-1245000000', 'VND'), ['-1.245.000.000 ₫', 'âm 1.245.000.000 đồng']);
check('M4', show('0', 'VND'), ['0 ₫', '0 đồng']);
check('M6', show('20000', 'USD'), ['20.000 USD', '20.000 đô la Mỹ']);
check('M7', show('1250.5', 'USD'), ['1.250,50 USD', '1.250,50 đô la Mỹ']);
check('M8', m.formatMoney(D('1250.05'), 'USD'), '1.250,05 USD');
check('M9', m.formatMoney(D('0.005'), 'USD'), '0,01 USD');
check('M10', m.formatMoney(D('1234567.5'), 'VND'), '1.234.568 ₫');
check('M11', show('-0.4', 'VND'), ['0 ₫', '0 đồng']);
check('M12', m.formatMoney(D('99999999999999999.99'), 'VND'), '100.000.000.000.000.000 ₫');

console.log('-- 3. Input parsing');
check('P1', parse('12.002.000.000'), '12002000000');
check('P2', parse('1.250,50'), '1250.50');
check('P3', parse('1250,5'), '1250.5');
check('P4', parse(' 380 000 000 ₫'), '380000000');
check('P5', parse('-1.245.000.000'), 'negative-not-allowed');
check('P6', parse('-1.245.000.000', { allowNegative: true }), '-1245000000');
check('P7', parse('1.25.000'), 'invalid-format');
check('P8', parse('1,2,3'), 'invalid-format');
check('P9', parse('1.250,505'), 'too-many-decimals');
check('P10', [parse('abc'), parse('')], ['invalid-format', 'empty']);
check('P11', parse('123.456.789.012.345.678'), 'too-large');
check('P12', parse('25.100,5', m.RATE_LIMITS), '25100.5');
check('P13', parse('1.250'), '1250');
check('P14', [m.fromFormValue('1250,50'), m.fromFormValue('1250,'), m.fromFormValue('-'), m.toFormValue(D('1250.50'))], ['1250.50', '1250', null, '1250,50']);

console.log('-- 4. Exchange rate & conversion');
const rate = { usdToVndRate: D('25100.000000'), effectiveDate: '2026-09-12' };
check('R1', [m.formatExchangeRate(rate), m.formatRateEffective(rate)], ['1 USD = 25.100 VND', 'hiệu lực 12/09/2026']);
check('R2', m.formatExchangeRate({ ...rate, usdToVndRate: D('25100.5') }), '1 USD = 25.100,5 VND');
check('R3', m.readExchangeRateSettings({ baseCurrency: 'VND', usdToVndRate: null, effectiveDate: null, updatedAt: null }), { rate: null, updatedAt: null });
const r4 = m.convertToReporting(D('20000'), 'USD', rate);
check('R4', [r4.status, r4.amount, m.formatMoney(r4.amount, 'VND'), r4.rate.usdToVndRate, r4.rate.effectiveDate], ['converted', '502000000.00', '502.000.000 ₫', '25100.000000', '2026-09-12']);
check('R5', m.convertToReporting(D('1250.50'), 'USD', { ...rate, usdToVndRate: D('25100.5') }).amount, '31388175.25');
check('R6', m.convertToReporting(D('0.01'), 'USD', { ...rate, usdToVndRate: D('25100.123456') }).amount, '251.00');
check('R7', m.convertToReporting(D('20000'), 'USD', null), { status: 'unavailable', original: { amount: '20000', currency: 'USD' }, reason: 'Chưa có tỷ giá USD/VND' });
check('R8', m.convertToReporting(D('380000000'), 'VND', rate), { status: 'reporting', amount: '380000000' });

console.log('-- 5. Percentages');
check('Q1', [m.formatPercent(D('18.4')), m.percentAccessibilityLabel(D('18.4'))], ['18,4%', '18,4 phần trăm']);
check('Q2', m.formatPercent(D('18.36')), '18,4%');
check('Q3', m.formatPercent(D('0')), '0%');
check('Q4', [m.formatPercent(D('-2.5')), m.percentAccessibilityLabel(D('-2.5'))], ['-2,5%', 'âm 2,5 phần trăm']);
check('Q6', m.formatPercent(D('100')), '100%');

console.log('-- 6. Dates and timestamps (process TZ=' + (process.env.TZ ?? 'system') + ')');
check('T1', t.formatDate(t.parseIsoDate('2026-09-12')), '12/09/2026');
check('T2', t.formatTimestamp(t.parseInstant('2026-09-13T02:30:00Z')), '09:30, 13/09/2026 (GMT+7)');
check('T3', t.civilDateOf(t.parseInstant('2026-09-12T16:59:59Z')), '2026-09-12');
check('T4', t.civilDateOf(t.parseInstant('2026-09-12T17:00:00Z')), '2026-09-13');
check('T5', t.formatTimestamp(t.parseInstant('2026-12-31T17:30:00Z')), '00:30, 01/01/2027 (GMT+7)');
check('T6', t.formatTimestamp(t.parseInstant('2026-11-22T09:38:02.123456Z')), '16:38, 22/11/2026 (GMT+7)');
const now = t.parseInstant('2026-09-13T04:30:00Z');
check('T7', t.formatRelative(t.parseInstant('2026-09-13T02:30:00Z'), now), 'Khoảng 2 giờ trước');
check('T8', [
  t.formatRelative(new Date(now.getTime() - 30e3), now),
  t.formatRelative(new Date(now.getTime() - 45 * 60e3), now),
  t.formatRelative(t.parseInstant('2026-09-12T10:00:00Z'), now),
  t.formatRelative(t.parseInstant('2026-09-03T04:30:00Z'), now),
], ['Vừa xong', '45 phút trước', 'Hôm qua', '10 ngày trước']);
check('T9', [t.parseIsoDate('2026-02-30'), t.parseIsoDate('not-a-date'), t.parseInstant('not-a-date')], [null, null, null]);

console.log('-- 7. Freshness');
check('S1', t.assessFreshness('2026-09-12', 30, '2026-09-13'), { status: 'fresh', ageDays: 1, asOf: '2026-09-12' });
check('S2', t.assessFreshness('2026-09-12', 30, '2026-11-22'), { status: 'stale', ageDays: 71, asOf: '2026-09-12' });
check('S3', [t.assessFreshness('2026-09-12', 30, '2026-10-12').status, t.assessFreshness('2026-09-12', 30, '2026-10-13').status], ['fresh', 'stale']);
check('S4', t.todayInProductZone(t.parseInstant('2026-09-12T16:30:00Z')), '2026-09-12');
check('S4b', t.todayInProductZone(t.parseInstant('2026-09-12T17:30:00Z')), '2026-09-13');
check('S5', t.assessFreshness(null, 30, '2026-11-22'), { status: 'missing' });

console.log(`\n${pass} passed, ${fail} failed`);
process.exitCode = fail === 0 ? 0 : 1;
