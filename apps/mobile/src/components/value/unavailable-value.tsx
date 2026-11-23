import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/** Section 9: an incalculable value says so in words; it never falls back to zero. */
export const UNAVAILABLE_VALUE_LABEL = 'Không khả dụng';

export interface UnavailableValueProps {
  /** Why the value cannot be shown, for example "Tổng tài sản bằng 0 nên không thể tính tỷ lệ". */
  reason: string;
  /** The type scale of the value this stands in for, so the layout does not jump. */
  variant?: 'displaySmall' | 'titleLarge' | 'bodyLarge';
  testID?: string;
}

export function UnavailableValue({
  reason,
  variant = 'bodyLarge',
  testID = 'unavailable-value',
}: UnavailableValueProps) {
  const theme = useAppTheme();

  return (
    <View
      accessibilityLabel={`${UNAVAILABLE_VALUE_LABEL}. ${reason}`}
      accessibilityRole="text"
      accessible
      style={styles.container}
      testID={testID}
    >
      {/* The dash is the shape; the label carries the meaning, so colour is never alone. */}
      <Text style={{ color: theme.colors.onSurfaceVariant }} variant={variant}>
        {`— ${UNAVAILABLE_VALUE_LABEL}`}
      </Text>
      <Text
        style={{ color: theme.colors.onSurfaceVariant }}
        testID={`${testID}-reason`}
        variant="bodyMedium"
      >
        {reason}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: spacing.xs,
  },
});
