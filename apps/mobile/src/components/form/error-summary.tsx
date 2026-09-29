import { StyleSheet, View } from 'react-native';
import { Surface, Text, TouchableRipple } from 'react-native-paper';

import { radius, sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

export interface FieldError {
  name: string;
  label: string;
  message: string;
}

interface ErrorSummaryProps {
  errors: readonly FieldError[];
  onFocusField: (name: string) => void;
  testID?: string;
}

/**
 * Section 8 requires an accessible error summary when several fields fail on submit. Each entry
 * names the field and its correction, and activating it moves focus to that field so the user
 * is not left to hunt for it.
 */
export function ErrorSummary({ errors, onFocusField, testID = 'error-summary' }: ErrorSummaryProps) {
  const theme = useAppTheme();

  if (errors.length === 0) {
    return null;
  }

  return (
    <Surface
      /**
       * Assertive: the user has just submitted and is waiting for the result, so this is the
       * answer to an action they took rather than an ambient change.
       */
      accessibilityLiveRegion="assertive"
      accessibilityRole="alert"
      elevation={0}
      style={[
        styles.summary,
        { backgroundColor: theme.colors.surface, borderColor: theme.colors.error },
      ]}
      testID={testID}
    >
      <Text style={{ color: theme.colors.error }} variant="titleMedium">
        {errors.length === 1
          ? 'Còn 1 trường cần sửa'
          : `Còn ${errors.length} trường cần sửa`}
      </Text>

      <View>
        {errors.map((error) => (
          <TouchableRipple
            accessibilityLabel={`${error.label}: ${error.message}`}
            accessibilityRole="button"
            key={error.name}
            onPress={() => onFocusField(error.name)}
            style={styles.entry}
            testID={`${testID}-${error.name}`}
          >
            <Text style={{ color: theme.colors.error }} variant="bodyMedium">
              {error.label}: {error.message}
            </Text>
          </TouchableRipple>
        ))}
      </View>
    </Surface>
  );
}

const styles = StyleSheet.create({
  summary: {
    borderRadius: radius.card,
    borderWidth: 1,
    gap: spacing.xs,
    padding: spacing.md,
  },
  entry: {
    justifyContent: 'center',
    minHeight: sizing.minTouchTarget.android,
    paddingVertical: spacing.xs,
  },
});
