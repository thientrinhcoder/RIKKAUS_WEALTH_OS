import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import {
  RATE_MISSING_REASON,
  formatExchangeRate,
  formatMoney,
  formatRateEffective,
  formatRatePerUnit,
  type Conversion,
  type ExchangeRateSnapshot,
} from '@/money';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { MoneyText, type FigureVariant } from './money-text';
import { UnavailableValue } from './unavailable-value';

export interface ExchangeRateTextProps {
  /** `null` until the user declares a rate. */
  rate: ExchangeRateSnapshot | null;
  /** The rate-needed state's path: "Thiết lập tỷ giá". */
  setupLabel?: string;
  onSetup?: () => void;
  testID?: string;
}

/** The rate and its effective date, always together: `1 USD = 25.100 VND · hiệu lực 12/09/2026`. */
export function ExchangeRateText({
  rate,
  setupLabel,
  onSetup,
  testID = 'exchange-rate-text',
}: ExchangeRateTextProps) {
  const theme = useAppTheme();

  if (rate === null) {
    return (
      <View style={styles.stack} testID={testID}>
        <UnavailableValue
          reason={`${RATE_MISSING_REASON}. Giá trị USD chưa được quy đổi sang VND.`}
          testID={`${testID}-missing`}
        />
        {setupLabel !== undefined && onSetup !== undefined ? (
          <ActionButton onPress={onSetup} testID={`${testID}-setup`} variant="secondary">
            {setupLabel}
          </ActionButton>
        ) : null}
      </View>
    );
  }

  return (
    <View
      accessibilityLabel={`Tỷ giá ${formatExchangeRate(rate)}, ${formatRateEffective(rate)}`}
      accessible
      style={styles.stack}
      testID={testID}
    >
      <Text
        style={{ color: theme.colors.onSurface }}
        testID={`${testID}-rate`}
        variant="titleLarge"
      >
        {formatExchangeRate(rate)}
      </Text>
      <Text
        style={{ color: theme.colors.onSurfaceVariant }}
        testID={`${testID}-effective`}
        variant="bodyMedium"
      >
        {formatRateEffective(rate)}
      </Text>
    </View>
  );
}

export interface ConvertedMoneyProps {
  conversion: Conversion;
  variant?: FigureVariant;
  testID?: string;
}

/**
 * A value in the reporting currency with its trace: VND first as the answer, then the USD
 * original, the rate and its effective date. A USD value without a rate keeps its original and
 * says why there is no VND figure.
 */
export function ConvertedMoney({
  conversion,
  variant = 'titleLarge',
  testID = 'converted-money',
}: ConvertedMoneyProps) {
  const theme = useAppTheme();

  if (conversion.status === 'reporting') {
    return (
      <MoneyText amount={conversion.amount} currency="VND" testID={testID} variant={variant} />
    );
  }

  if (conversion.status === 'unavailable') {
    return (
      <View style={styles.stack} testID={testID}>
        <UnavailableValue
          reason={`${conversion.reason}. Giá trị gốc vẫn được giữ.`}
          testID={`${testID}-reporting`}
          variant={variant === 'bodyMedium' ? 'bodyLarge' : variant}
        />
        <MoneyText
          amount={conversion.original.amount}
          currency={conversion.original.currency}
          testID={`${testID}-original`}
          tone="secondary"
          variant="bodyLarge"
        />
      </View>
    );
  }

  const trace = `${formatMoney(conversion.original.amount, conversion.original.currency)} × ${formatRatePerUnit(
    conversion.rate,
  )} = ${formatMoney(conversion.amount, 'VND')}`;

  return (
    <View style={styles.stack} testID={testID}>
      <MoneyText
        amount={conversion.amount}
        currency="VND"
        testID={`${testID}-reporting`}
        variant={variant}
      />
      <MoneyText
        amount={conversion.original.amount}
        currency={conversion.original.currency}
        testID={`${testID}-original`}
        tone="secondary"
        variant="bodyLarge"
      />
      <Text
        style={{ color: theme.colors.onSurfaceVariant }}
        testID={`${testID}-rate`}
        variant="bodyMedium"
      >
        {`Tỷ giá ${formatRatePerUnit(conversion.rate)} · ${formatRateEffective(conversion.rate)}`}
      </Text>
      <Text
        style={{ color: theme.colors.onSurfaceVariant }}
        testID={`${testID}-trace`}
        variant="bodyMedium"
      >
        {trace}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  stack: {
    alignItems: 'flex-start',
    gap: spacing.xs,
  },
});
