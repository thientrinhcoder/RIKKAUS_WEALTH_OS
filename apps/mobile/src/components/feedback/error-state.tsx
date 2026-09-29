import { View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { StateView } from './state-view';

/**
 * Section 11.1 requires a request or offline error to state what failed, what was preserved, and
 * the retry or safe-return path. All three are required props.
 *
 * The two variants differ in what the user can do about it, which is why they are distinguished
 * rather than collapsed: a request failure is worth retrying now, and an offline one is not.
 */
export type ErrorVariant = 'request' | 'offline';

export interface ErrorStateProps {
  variant: ErrorVariant;
  /** What failed, in the caller's words. */
  message: string;
  /** What was preserved, so the user knows whether their input survived. */
  preserved: string;
  retryLabel: string;
  onRetry: () => void;
  /** The safe way back, offered alongside retry. */
  safeReturnLabel?: string;
  onSafeReturn?: () => void;
  testID?: string;
}

const VARIANT_ICON: Record<ErrorVariant, string> = {
  request: 'alert-circle-outline',
  offline: 'wifi-off',
};

export function ErrorState({
  variant,
  message,
  preserved,
  retryLabel,
  onRetry,
  safeReturnLabel,
  onSafeReturn,
  testID = 'error-state',
}: ErrorStateProps) {
  const theme = useAppTheme();

  return (
    <StateView
      action={
        <View style={styles.actions}>
          <ActionButton onPress={onRetry} testID={`${testID}-retry`} variant="primary">
            {retryLabel}
          </ActionButton>

          {safeReturnLabel === undefined || onSafeReturn === undefined ? null : (
            <ActionButton
              onPress={onSafeReturn}
              testID={`${testID}-safe-return`}
              variant="text"
            >
              {safeReturnLabel}
            </ActionButton>
          )}
        </View>
      }
      detail={
        <Text
          style={{ color: theme.colors.onSurfaceVariant }}
          testID={`${testID}-preserved`}
          variant="bodyLarge"
        >
          {preserved}
        </Text>
      }
      icon={VARIANT_ICON[variant]}
      /** Assertive: the user is waiting on an action that just failed. */
      live="assertive"
      message={message}
      testID={testID}
      tone="error"
    />
  );
}

const styles = {
  actions: {
    flexDirection: 'row' as const,
    gap: spacing.sm,
  },
};
