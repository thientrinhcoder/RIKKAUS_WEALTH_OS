import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { OverlayShell } from './overlay-shell';

export interface ConfirmDialogProps {
  visible: boolean;
  title: string;
  /** What the user is agreeing to, in the caller's words. */
  body: string;
  confirmLabel: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  testID?: string;
}

/**
 * An ordinary confirmation. Nothing here is destructive, so it accepts every dismissal route
 * and cancel simply makes no change.
 */
export function ConfirmDialog({
  visible,
  title,
  body,
  confirmLabel,
  cancelLabel = 'Huỷ',
  onConfirm,
  onCancel,
  testID = 'confirm-dialog',
}: ConfirmDialogProps) {
  const theme = useAppTheme();

  return (
    <OverlayShell
      accessibilityLabel={title}
      onDismiss={onCancel}
      testID={testID}
      visible={visible}
    >
      <Text accessibilityRole="header" testID={`${testID}-title`} variant="titleMedium">
        {title}
      </Text>

      <Text
        style={{ color: theme.colors.onSurfaceVariant }}
        testID={`${testID}-body`}
        variant="bodyLarge"
      >
        {body}
      </Text>

      <View style={styles.actions}>
        {/** Cancel comes first in reading and tab order: the safe route is the easy one. */}
        <ActionButton onPress={onCancel} testID={`${testID}-cancel`} variant="text">
          {cancelLabel}
        </ActionButton>

        <ActionButton onPress={onConfirm} testID={`${testID}-confirm`} variant="primary">
          {confirmLabel}
        </ActionButton>
      </View>
    </OverlayShell>
  );
}

const styles = StyleSheet.create({
  actions: {
    flexDirection: 'row',
    gap: spacing.sm,
    justifyContent: 'flex-end',
  },
});
