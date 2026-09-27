import { View } from 'react-native';
import { Text } from 'react-native-paper';

import { useAppTheme } from '@/ui/theme';
import { CardShell } from './card-shell';

/**
 * Section 9 forbids showing zero as a fallback for missing or incalculable data, so an absent
 * value renders this explicitly rather than a number the user would read as real.
 */
export const UNAVAILABLE_LABEL = 'Không khả dụng';

export interface KpiCardProps {
  label: string;
  /** Already formatted by the caller. Absent means the value could not be calculated. */
  value?: string;
  /** For example the as-of date or the source of the figure. */
  caption?: string;
  onPress?: () => void;
  testID?: string;
}

export function KpiCard({ label, value, caption, onPress, testID }: KpiCardProps) {
  const theme = useAppTheme();
  const available = value !== undefined && value !== '';

  return (
    <CardShell
      accessibilityLabel={`${label}: ${available ? value : UNAVAILABLE_LABEL}`}
      onPress={onPress}
      testID={testID}
    >
      <Text style={{ color: theme.colors.onSurfaceVariant }} variant="labelLarge">
        {label}
      </Text>

      <View>
        <Text
          style={{ color: available ? theme.colors.onSurface : theme.colors.onSurfaceVariant }}
          testID={testID === undefined ? undefined : `${testID}-value`}
          variant="displaySmall"
        >
          {available ? value : UNAVAILABLE_LABEL}
        </Text>
      </View>

      {caption === undefined ? null : (
        <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyMedium">
          {caption}
        </Text>
      )}
    </CardShell>
  );
}
