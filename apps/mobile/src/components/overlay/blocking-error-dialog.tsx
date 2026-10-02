import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { useAppTheme } from '@/ui/theme';
import { OverlayShell } from './overlay-shell';

export interface BlockingErrorDialogProps {
  visible: boolean;
  title: string;
  /** What failed and what was preserved, in the caller's words. */
  message: string;
  /** The one safe way forward. */
  safeReturnLabel: string;
  onSafeReturn: () => void;
  testID?: string;
}

/**
 * An error the user cannot work around from here.
 *
 * Section 12 gives it exactly one safe way forward and no scrim dismissal: leaving it by tapping
 * outside would drop the user somewhere ambiguous with the failure unacknowledged.
 */
export function BlockingErrorDialog({
  visible,
  title,
  message,
  safeReturnLabel,
  onSafeReturn,
  testID = 'blocking-error-dialog',
}: BlockingErrorDialogProps) {
  const theme = useAppTheme();

  return (
    <OverlayShell
      accessibilityLabel={title}
      dismissPolicy="explicit-only"
      onDismiss={onSafeReturn}
      testID={testID}
      visible={visible}
    >
      <Text
        accessibilityRole="header"
        style={{ color: theme.colors.error }}
        testID={`${testID}-title`}
        variant="titleMedium"
      >
        {title}
      </Text>

      <Text testID={`${testID}-message`} variant="bodyLarge">
        {message}
      </Text>

      <ActionButton
        onPress={onSafeReturn}
        testID={`${testID}-safe-return`}
        variant="primary"
      >
        {safeReturnLabel}
      </ActionButton>
    </OverlayShell>
  );
}
