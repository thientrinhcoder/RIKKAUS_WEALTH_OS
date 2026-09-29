import { View } from 'react-native';
import { ProgressBar, Text } from 'react-native-paper';

import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { CardShell } from './card-shell';

export interface GoalCardProps {
  title: string;
  /** 0 to 1. The caller computes it; the card only presents it. */
  progress: number;
  /** Already formatted, for example the amount saved against the target. */
  progressLabel: string;
  /** Supplied by the domain: the shared kit never hard-codes goal vocabulary. */
  statusLabel?: string;
  onPress?: () => void;
  testID?: string;
}

/**
 * Section 12 requires progress to be readable without seeing the bar, so the percentage is
 * exposed as an accessible value and the caller's label is rendered as text beside it.
 */
export function GoalCard({
  title,
  progress,
  progressLabel,
  statusLabel,
  onPress,
  testID,
}: GoalCardProps) {
  const theme = useAppTheme();
  const clamped = Math.min(1, Math.max(0, progress));
  const percent = Math.round(clamped * 100);

  return (
    <CardShell
      accessibilityLabel={`${title}. ${progressLabel}.`}
      onPress={onPress}
      testID={testID}
    >
      <Text variant="titleMedium">{title}</Text>

      <View
        accessibilityRole="progressbar"
        accessibilityValue={{ min: 0, max: 100, now: percent, text: `${percent}%` }}
        aria-valuemax={100}
        aria-valuemin={0}
        aria-valuenow={percent}
        style={{ gap: spacing.xs }}
        testID={testID === undefined ? undefined : `${testID}-progress`}
      >
        <ProgressBar color={theme.colors.primary} progress={clamped} />
        <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyMedium">
          {progressLabel}
        </Text>
      </View>

      {statusLabel === undefined ? null : (
        <Text
          testID={testID === undefined ? undefined : `${testID}-status`}
          variant="labelLarge"
        >
          {statusLabel}
        </Text>
      )}
    </CardShell>
  );
}
