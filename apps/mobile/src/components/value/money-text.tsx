import { Text } from 'react-native-paper';

import {
  formatMoney,
  formatPercent,
  moneyAccessibilityLabel,
  percentAccessibilityLabel,
  type CurrencyCode,
  type DecimalString,
} from '@/money';
import { useAppTheme } from '@/ui/theme';
import { UnavailableValue } from './unavailable-value';

/** Paper variants approved for figures; each already carries tabular digits. */
export type FigureVariant = 'displaySmall' | 'titleLarge' | 'bodyLarge' | 'bodyMedium';

export interface MoneyTextProps {
  /** `null` is a value that could not be determined, which renders as unavailable. */
  amount: DecimalString | null;
  currency: CurrencyCode;
  /** Required with a `null` amount: the user is told why, not shown a blank. */
  unavailableReason?: string;
  variant?: FigureVariant;
  /** Secondary figures (the USD original under a VND value) use the supporting colour. */
  tone?: 'primary' | 'secondary';
  testID?: string;
}

export function MoneyText({
  amount,
  currency,
  unavailableReason = 'Chưa có dữ liệu để hiển thị giá trị này.',
  variant = 'bodyLarge',
  tone = 'primary',
  testID = 'money-text',
}: MoneyTextProps) {
  const theme = useAppTheme();

  if (amount === null) {
    return (
      <UnavailableValue
        reason={unavailableReason}
        testID={testID}
        variant={variant === 'bodyMedium' ? 'bodyLarge' : variant}
      />
    );
  }

  return (
    <Text
      accessibilityLabel={moneyAccessibilityLabel(amount, currency)}
      style={{ color: tone === 'primary' ? theme.colors.onSurface : theme.colors.onSurfaceVariant }}
      testID={testID}
      variant={variant}
    >
      {formatMoney(amount, currency)}
    </Text>
  );
}

export interface PercentTextProps {
  /** A percentage the backend computed. `null` means it could not be computed. */
  value: DecimalString | null;
  unavailableReason?: string;
  maxFractionDigits?: number;
  variant?: FigureVariant;
  testID?: string;
}

export function PercentText({
  value,
  unavailableReason = 'Chưa đủ dữ liệu để tính tỷ lệ này.',
  maxFractionDigits = 1,
  variant = 'bodyLarge',
  testID = 'percent-text',
}: PercentTextProps) {
  const theme = useAppTheme();

  if (value === null) {
    return (
      <UnavailableValue
        reason={unavailableReason}
        testID={testID}
        variant={variant === 'bodyMedium' ? 'bodyLarge' : variant}
      />
    );
  }

  return (
    <Text
      accessibilityLabel={percentAccessibilityLabel(value, maxFractionDigits)}
      style={{ color: theme.colors.onSurface }}
      testID={testID}
      variant={variant}
    >
      {formatPercent(value, maxFractionDigits)}
    </Text>
  );
}
