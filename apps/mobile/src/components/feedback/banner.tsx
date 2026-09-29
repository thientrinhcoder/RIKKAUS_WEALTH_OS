import { StyleSheet, View } from 'react-native';
import { Icon, Surface, Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { radius, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import type { StateTone } from './state-view';

export interface BannerProps {
  message: string;
  tone?: StateTone;
  icon?: string;
  /** Persistent until dismissed, which is what separates a banner from a snackbar. */
  dismissLabel?: string;
  onDismiss: () => void;
  actionLabel?: string;
  onAction?: () => void;
  testID?: string;
}

export function Banner({
  message,
  tone = 'info',
  icon = 'information-outline',
  dismissLabel = 'Đóng thông báo',
  onDismiss,
  actionLabel,
  onAction,
  testID = 'banner',
}: BannerProps) {
  const theme = useAppTheme();

  const toneColor = {
    neutral: theme.colors.onSurfaceVariant,
    success: theme.colors.success,
    warning: theme.colors.warning,
    error: theme.colors.error,
    info: theme.colors.info,
  }[tone];

  return (
    <Surface
      accessibilityLiveRegion="polite"
      elevation={0}
      style={[
        styles.banner,
        { backgroundColor: theme.colors.surface, borderColor: toneColor },
      ]}
      testID={testID}
    >
      <View style={styles.headline}>
        <Icon color={toneColor} size={20} source={icon} />
        <Text style={styles.message} testID={`${testID}-message`} variant="bodyLarge">
          {message}
        </Text>
      </View>

      <View style={styles.actions}>
        {actionLabel === undefined || onAction === undefined ? null : (
          <ActionButton onPress={onAction} testID={`${testID}-action`} variant="text">
            {actionLabel}
          </ActionButton>
        )}

        <ActionButton
          accessibilityLabel={dismissLabel}
          icon="close"
          onPress={onDismiss}
          testID={`${testID}-dismiss`}
          variant="text"
        />
      </View>
    </Surface>
  );
}

const styles = StyleSheet.create({
  banner: {
    borderRadius: radius.card,
    borderWidth: 1,
    gap: spacing.sm,
    padding: spacing.md,
  },
  headline: {
    alignItems: 'flex-start',
    flexDirection: 'row',
    gap: spacing.sm,
  },
  message: {
    flex: 1,
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
  },
});
