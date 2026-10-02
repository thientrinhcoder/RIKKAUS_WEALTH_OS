import { View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { StateView } from './state-view';
import { StatusIndicator } from './status-indicator';

/**
 * Section 11.1 requires a stale state to combine text with an icon or shape, carry an absolute
 * date, name the affected value, and offer an update path. All four are required props.
 */
export interface StaleStateProps {
  /** Which value is out of date. */
  affectedValue: string;
  /**
   * An absolute date, for example "12/09/2026". Section 9 allows relative time to supplement an
   * absolute date but never to replace it, so this is the absolute one.
   */
  asOfDate: string;
  /** The caller's short label for the state, for example "Cần cập nhật". */
  statusLabel: string;
  updateLabel: string;
  onUpdate: () => void;
  testID?: string;
}

export function StaleState({
  affectedValue,
  asOfDate,
  statusLabel,
  updateLabel,
  onUpdate,
  testID = 'stale-state',
}: StaleStateProps) {
  const theme = useAppTheme();

  return (
    <StateView
      action={
        <ActionButton onPress={onUpdate} testID={`${testID}-action-button`} variant="secondary">
          {updateLabel}
        </ActionButton>
      }
      detail={
        <View style={{ gap: spacing.xs }}>
          <StatusIndicator label={statusLabel} testID={`${testID}-indicator`} tone="stale" />
          <Text
            style={{ color: theme.colors.onSurfaceVariant }}
            testID={`${testID}-as-of`}
            variant="bodyLarge"
          >
            {asOfDate}
          </Text>
        </View>
      }
      icon="clock-alert-outline"
      message={affectedValue}
      testID={testID}
      tone="warning"
    />
  );
}
