import { StyleSheet, View } from 'react-native';

import { radius, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

export interface LoadingStateProps {
  /**
   * The height the loaded content will occupy. Section 5.5 requires loading to reserve stable
   * space, so the caller states the shape rather than the skeleton guessing and the layout
   * jumping when real content arrives.
   */
  reservedHeight: number;
  /** How many placeholder lines to draw inside the reserved space. */
  lines?: number;
  /** What is loading, so a screen reader hears more than "busy". */
  accessibilityLabel: string;
  testID?: string;
}

/**
 * A skeleton that reserves the same height as the content it stands in for, and renders no
 * value of any kind: section 5.5 forbids animating financial values, and a placeholder number
 * would be a fake value on screen.
 */
export function LoadingState({
  reservedHeight,
  lines = 3,
  accessibilityLabel,
  testID = 'loading-state',
}: LoadingStateProps) {
  const theme = useAppTheme();

  return (
    <View
      accessibilityLabel={accessibilityLabel}
      accessibilityState={{ busy: true }}
      aria-busy
      style={[styles.skeleton, { height: reservedHeight }]}
      testID={testID}
    >
      {Array.from({ length: lines }, (_, index) => (
        <View
          key={index}
          style={[
            styles.line,
            {
              backgroundColor: theme.colors.surfaceVariant,
              width: index === lines - 1 ? '60%' : '100%',
            },
          ]}
          testID={`${testID}-line-${index}`}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  skeleton: {
    gap: spacing.sm,
    justifyContent: 'flex-start',
  },
  line: {
    borderRadius: radius.input,
    height: 16,
  },
});
