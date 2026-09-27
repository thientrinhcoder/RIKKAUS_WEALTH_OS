import type { ReactNode } from 'react';
import { StyleSheet } from 'react-native';
import { Surface, TouchableRipple } from 'react-native-paper';

import { elevation } from '@/ui/elevation';
import { radius, sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * The shared surface every card variant composes: padding, radius, elevation and press
 * semantics from docs/design-guidelines.md section 5.3.
 *
 * A card that does nothing when pressed exposes no button role, so a screen reader never offers
 * an action that does not exist.
 */
export interface CardShellProps {
  children: ReactNode;
  /** Supplying this makes the card tappable and requires an accessible name. */
  onPress?: () => void;
  accessibilityLabel?: string;
  testID?: string;
}

export function CardShell({ children, onPress, accessibilityLabel, testID }: CardShellProps) {
  const theme = useAppTheme();

  const surface = (
    <Surface
      elevation={onPress === undefined ? elevation.flat.level : elevation.raisedCard.level}
      style={[
        styles.card,
        {
          backgroundColor: theme.colors.surface,
          borderColor: theme.colors.outlineVariant,
          borderWidth: onPress === undefined ? StyleSheet.hairlineWidth : 0,
        },
      ]}
      testID={onPress === undefined ? testID : undefined}
    >
      {children}
    </Surface>
  );

  if (onPress === undefined) {
    return surface;
  }

  return (
    <TouchableRipple
      accessibilityLabel={accessibilityLabel}
      accessibilityRole="button"
      borderless
      onPress={onPress}
      style={styles.pressable}
      testID={testID}
    >
      {surface}
    </TouchableRipple>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: radius.card,
    gap: spacing.sm,
    padding: spacing.md,
  },
  pressable: {
    borderRadius: radius.card,
    minHeight: sizing.minTouchTarget.android,
  },
});
