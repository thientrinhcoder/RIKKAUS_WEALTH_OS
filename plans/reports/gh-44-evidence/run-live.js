const fs = require('fs'); const m = require('./primitives/money'); const t = require('./primitives/time');
const rateJson = fs.readFileSync(__dirname + '/live-rate.json', 'utf8').split('\nHTTP')[0];
const assetsJson = fs.readFileSync(__dirname + '/live-assets.raw', 'utf8').split('\nHTTP')[0];
const settings = m.readExchangeRateSettings(JSON.parse(rateJson));
console.log('R9/R10', m.formatExchangeRate(settings.rate), '·', m.formatRateEffective(settings.rate));
console.log('T6    ', 'Cập nhật lúc', t.formatTimestamp(settings.updatedAt), '| UTC', t.formatUtcSource(settings.updatedAt));
const today = t.todayInProductZone();
console.log('S6    ', 'today(VN)=' + today, JSON.stringify(t.assessFreshness(settings.rate.effectiveDate, 30, today)));
for (const a of JSON.parse(assetsJson)) {
  const amount = m.fromApiValue(a.currentValue);
  const c = m.convertToReporting(amount, a.currency, settings.rate);
  const shown = c.status === 'converted'
    ? `${m.formatMoney(c.amount, 'VND')} ← ${m.formatMoney(c.original.amount, 'USD')} × ${m.formatRatePerUnit(c.rate)} · ${m.formatRateEffective(c.rate)}`
    : m.formatMoney(c.amount, 'VND');
  console.log('L1/L2 ', `${a.name}: json=${JSON.stringify(a.currentValue)} (${typeof a.currentValue}) decimal=${amount} precisionAtRisk=${m.isPrecisionAtRisk(a.currentValue)} → ${shown} · ngày định giá ${t.formatDate(a.valuationDate)}`);
}
