import { useState } from 'react';
import { View } from 'react-native';
import { Text } from 'react-native-paper';

import { CardShell } from '@/components/data';
import { TextField } from '@/components/form';
import { ScreenHeading } from '@/components/shell/screen-heading';
import {
  ConvertedMoney,
  DateText,
  ExchangeRateText,
  FreshnessBadge,
  MoneyText,
  PercentText,
  TimestampText,
  UnavailableValue,
} from '@/components/value';
import { LiveMoneyPanel } from '@/catalog/live-money-panel';
import {
  DECIMAL_PARSE_MESSAGES,
  RATE_LIMITS,
  convertToReporting,
  decimal,
  parseVietnameseDecimal,
  type DecimalParseOptions,
  type ExchangeRateSnapshot,
} from '@/money';
import { parseInstant, parseIsoDate, type IsoDate } from '@/time';
import { spacing } from '@/ui/tokens';

/**
 * Money, currency, exchange-rate and time primitives in every state the approved design
 * (docs/money-currency-time-primitives-review.html) names. The figures are the design's own; the
 * clock is pinned so the relative and freshness examples read the same on every run.
 */
const RATE: ExchangeRateSnapshot = {
  usdToVndRate: decimal('25100.000000'),
  effectiveDate: '2026-09-12' as IsoDate,
};

const UPDATED_AT = parseInstant('2026-09-13T02:30:00Z');
const NOW = new Date('2026-09-13T04:30:00Z');

function Section({
  title,
  children,
  testID,
}: {
  title: string;
  children: React.ReactNode;
  testID: string;
}) {
  return (
    <CardShell testID={testID}>
      <Text variant="titleMedium">{title}</Text>
      <View style={{ gap: spacing.md }}>{children}</View>
    </CardShell>
  );
}

function ParseProbe({
  label,
  helper,
  options,
  testID,
  initial,
}: {
  label: string;
  helper: string;
  options: DecimalParseOptions;
  testID: string;
  initial: string;
}) {
  const [text, setText] = useState(initial);
  const result = parseVietnameseDecimal(text, options);

  return (
    <View style={{ gap: spacing.xs }}>
      <TextField
        autoCapitalize="none"
        errorText={result.error === undefined ? undefined : DECIMAL_PARSE_MESSAGES[result.error]}
        helperText={helper}
        keyboardType="default"
        label={label}
        onChangeValue={setText}
        testID={testID}
        value={text}
      />
      <Text testID={`${testID}-result`} variant="bodyMedium">
        {result.error === undefined ? `Giá trị gửi API: ${result.value}` : `Lỗi: ${result.error}`}
      </Text>
    </View>
  );
}

export default function MoneyCatalog() {
  return (
    <View style={{ gap: spacing.lg }} testID="catalog-money">
      <ScreenHeading>Tiền tệ, tỷ giá và thời gian</ScreenHeading>

      <Section testID="money-display" title="Hiển thị tiền">
        <MoneyText
          amount={decimal('12002000000')}
          currency="VND"
          testID="m-vnd"
          variant="displaySmall"
        />
        <MoneyText amount={decimal('380000000.00')} currency="VND" testID="m-vnd-decimals" />
        <MoneyText amount={decimal('-1245000000')} currency="VND" testID="m-negative" />
        <MoneyText amount={decimal('0')} currency="VND" testID="m-zero" />
        <MoneyText amount={decimal('20000')} currency="USD" testID="m-usd" />
        <MoneyText amount={decimal('1250.5')} currency="USD" testID="m-usd-cents" />
        <MoneyText
          amount={null}
          currency="VND"
          testID="m-null"
          unavailableReason="Tài sản này chưa có định giá."
        />
      </Section>

      <Section testID="money-rate" title="Tỷ giá và quy đổi">
        <ExchangeRateText rate={RATE} testID="r-rate" />
        <ConvertedMoney
          conversion={convertToReporting(decimal('20000'), 'USD', RATE)}
          testID="r-converted"
        />
        <ConvertedMoney
          conversion={convertToReporting(decimal('20000'), 'USD', null)}
          testID="r-no-rate"
        />
        <ExchangeRateText
          onSetup={() => {}}
          rate={null}
          setupLabel="Thiết lập tỷ giá"
          testID="r-missing"
        />
      </Section>

      <Section testID="money-percent" title="Tỷ lệ">
        <PercentText testID="q-ratio" value={decimal('18.4')} variant="titleLarge" />
        <PercentText
          testID="q-unavailable"
          unavailableReason="Tổng tài sản bằng 0 nên không thể tính tỷ lệ."
          value={null}
        />
      </Section>

      <Section testID="money-time" title="Ngày và giờ">
        <DateText date={parseIsoDate('2026-09-12')} prefix="Ngày định giá" testID="t-date" />
        <TimestampText
          instant={UPDATED_AT}
          now={NOW}
          prefix="Cập nhật lúc"
          showRelative
          showSource
          testID="t-timestamp"
        />
        <DateText date={parseIsoDate('2026-02-30')} prefix="Ngày định giá" testID="t-invalid" />
      </Section>

      <Section testID="money-freshness" title="Độ mới của dữ liệu (ngưỡng ví dụ 30 ngày)">
        <FreshnessBadge
          asOf={RATE.effectiveDate}
          datePrefix="hiệu lực"
          maxAgeDays={30}
          testID="s-fresh"
          today={'2026-09-13' as IsoDate}
        />
        <FreshnessBadge
          asOf={RATE.effectiveDate}
          datePrefix="hiệu lực"
          maxAgeDays={30}
          onUpdate={() => {}}
          testID="s-stale"
          today={'2026-11-22' as IsoDate}
          updateLabel="Cập nhật tỷ giá"
        />
        <FreshnessBadge
          asOf={null}
          maxAgeDays={30}
          onUpdate={() => {}}
          testID="s-missing"
          updateLabel="Thiết lập tỷ giá"
        />
      </Section>

      <Section testID="money-unavailable" title="Không khả dụng">
        <UnavailableValue
          reason="Tổng tài sản bằng 0 nên không thể tính tỷ lệ. Thêm tài sản hoặc cập nhật định giá để tính lại."
          testID="u-unavailable"
          variant="titleLarge"
        />
      </Section>

      <Section testID="money-parse" title="Nhập số theo định dạng Việt Nam">
        <ParseProbe
          helper="Ví dụ 12.002.000.000"
          initial="12.002.000.000"
          label="Số tiền VND"
          options={{}}
          testID="p-vnd"
        />
        <ParseProbe
          helper="Ví dụ 1.250,50"
          initial="1.250,50"
          label="Số tiền USD"
          options={{}}
          testID="p-usd"
        />
        <ParseProbe
          helper="Tối đa 6 chữ số thập phân"
          initial="25.100,5"
          label="Tỷ giá VND/USD"
          options={RATE_LIMITS}
          testID="p-rate"
        />
      </Section>

      <Section testID="money-live" title="Dữ liệu thật từ API (kiểm thử cục bộ)">
        <LiveMoneyPanel />
      </Section>
    </View>
  );
}
