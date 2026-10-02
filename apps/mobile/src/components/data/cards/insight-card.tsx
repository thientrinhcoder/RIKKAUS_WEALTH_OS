import { View } from 'react-native';
import { Icon, Text } from 'react-native-paper';

import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { CardShell } from './card-shell';

/**
 * Section 12 forbids conveying a distinction by colour alone, so a status carries both an icon
 * and a text label. The label comes from the caller: the shared kit holds no domain vocabulary.
 */
export interface InsightStatus {
  label: string;
  icon: string;
  tone: 'success' | 'warning' | 'error' | 'info';
}

export interface InsightCardProps {
  title: string;
  status: InsightStatus;
  /** What was measured, in the caller's words. */
  observation: string;
  /** The suggested next step. Optional: not every insight implies an action. */
  action?: string;
  onPress?: () => void;
  testID?: string;
}

export function InsightCard({
  title,
  status,
  observation,
  action,
  onPress,
  testID,
}: InsightCardProps) {
  const theme = useAppTheme();

  const toneColor = {
    success: theme.colors.success,
    warning: theme.colors.warning,
    error: theme.colors.error,
    info: theme.colors.info,
  }[status.tone];

  return (
    <CardShell
      accessibilityLabel={`${title}. ${status.label}. ${observation}`}
      onPress={onPress}
      testID={testID}
    >
      <View style={styles.statusRow}>
        <Icon color={toneColor} size={20} source={status.icon} />
        <Text
          style={{ color: toneColor }}
          testID={testID === undefined ? undefined : `${testID}-status`}
          variant="labelLarge"
        >
          {status.label}
        </Text>
      </View>

      <Text variant="titleMedium">{title}</Text>

      <Text
        style={{ color: theme.colors.onSurfaceVariant }}
        testID={testID === undefined ? undefined : `${testID}-observation`}
        variant="bodyLarge"
      >
        {observation}
      </Text>

      {action === undefined ? null : (
        <Text testID={testID === undefined ? undefined : `${testID}-action`} variant="labelLarge">
          {action}
        </Text>
      )}
    </CardShell>
  );
}

const styles = {
  statusRow: {
    alignItems: 'center' as const,
    flexDirection: 'row' as const,
    gap: spacing.xs,
  },
};
