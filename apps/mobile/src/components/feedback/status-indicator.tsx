import { StyleSheet, View } from 'react-native';
import { Icon, Text } from 'react-native-paper';

import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * One generic indicator for the five shared tones, per the Product Owner decision of
 * 2026-09-27.
 *
 * On-track, at-risk and behind are goal vocabulary owned by the goals feature, which passes its
 * own label in. The shared kit never hard-codes goal language and never maps a goal state to a
 * tone on the domain's behalf.
 */
export const STATUS_TONES = ['success', 'warning', 'error', 'info', 'stale'] as const;

export type StatusTone = (typeof STATUS_TONES)[number];

/** Section 12 requires a distinction to survive without colour, so each tone brings a shape. */
const TONE_ICON: Record<StatusTone, string> = {
  success: 'check-circle-outline',
  warning: 'alert-outline',
  error: 'close-circle-outline',
  info: 'information-outline',
  stale: 'clock-alert-outline',
};

export interface StatusIndicatorProps {
  tone: StatusTone;
  /** Supplied by the caller. This component ships no default label text. */
  label: string;
  testID?: string;
}

export function StatusIndicator({ tone, label, testID = 'status' }: StatusIndicatorProps) {
  const theme = useAppTheme();

  const color = {
    success: theme.colors.success,
    warning: theme.colors.warning,
    error: theme.colors.error,
    info: theme.colors.info,
    stale: theme.colors.warning,
  }[tone];

  return (
    <View
      accessibilityLabel={label}
      accessibilityRole="text"
      style={styles.indicator}
      testID={testID}
    >
      {/** Paper's Icon does not forward testID, so the wrapper carries it. */}
      <View testID={`${testID}-icon`}>
        <Icon color={color} size={16} source={TONE_ICON[tone]} />
      </View>
      <Text style={{ color }} testID={`${testID}-label`} variant="labelLarge">
        {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  indicator: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: spacing.xs,
  },
});
