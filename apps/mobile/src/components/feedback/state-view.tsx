import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { Icon, Text } from 'react-native-paper';

import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * The shared anatomy behind every async and data-quality state, from
 * docs/design-guidelines.md section 11.1.
 *
 * Section 11.1 describes one anatomy for all of them, so the implementation shares one too: a
 * state that forgot its next action would otherwise only be caught in review.
 */
export type StateTone = 'neutral' | 'success' | 'warning' | 'error' | 'info';

export interface StateViewProps {
  /** What happened, in the caller's words. */
  message: string;
  /** The supporting detail: what was preserved, what is missing, what is affected. */
  detail?: ReactNode;
  /** The recovery or next step. */
  action?: ReactNode;
  tone?: StateTone;
  /** Section 12 forbids colour as the only carrier, so a tone always brings an icon. */
  icon?: string;
  /**
   * An error the user must act on is assertive; everything else is polite so it does not
   * interrupt, per the announcement table in section 11.1.
   */
  live?: 'polite' | 'assertive' | 'none';
  busy?: boolean;
  testID?: string;
}

export function StateView({
  message,
  detail,
  action,
  tone = 'neutral',
  icon,
  live = 'polite',
  busy,
  testID = 'state',
}: StateViewProps) {
  const theme = useAppTheme();

  const toneColor = {
    neutral: theme.colors.onSurfaceVariant,
    success: theme.colors.success,
    warning: theme.colors.warning,
    error: theme.colors.error,
    info: theme.colors.info,
  }[tone];

  return (
    <View
      accessibilityLiveRegion={live}
      accessibilityRole={live === 'assertive' ? 'alert' : undefined}
      accessibilityState={busy === undefined ? undefined : { busy }}
      aria-busy={busy}
      style={styles.state}
      testID={testID}
    >
      <View style={styles.headline}>
        {icon === undefined ? null : (
          /** Paper's Icon does not forward testID, so the wrapper carries it. */
          <View testID={`${testID}-icon`}>
            <Icon color={toneColor} size={20} source={icon} />
          </View>
        )}

        <Text
          style={{ color: tone === 'neutral' ? theme.colors.onSurface : toneColor }}
          testID={`${testID}-message`}
          variant="titleMedium"
        >
          {message}
        </Text>
      </View>

      {detail === undefined ? null : <View testID={`${testID}-detail`}>{detail}</View>}

      {action === undefined ? null : <View testID={`${testID}-action`}>{action}</View>}
    </View>
  );
}

const styles = StyleSheet.create({
  state: {
    gap: spacing.sm,
  },
  headline: {
    alignItems: 'center',
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.xs,
  },
});
