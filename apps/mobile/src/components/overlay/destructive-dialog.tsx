import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { OverlayShell } from './overlay-shell';

export interface DestructiveDialogProps {
  visible: boolean;
  title: string;
  /**
   * What will be lost, in the caller's words. Required: section 11.1 makes naming the
   * consequence the requirement, so an unnamed consequence must not compile.
   */
  consequence: string;
  confirmLabel: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  testID?: string;
}

/**
 * A destructive confirmation.
 *
 * Section 12 keeps the destructive choice away from the ordinary one, so cancel and confirm are
 * separated rather than sitting side by side where a mis-tap costs data. The scrim does not
 * dismiss it: an accidental tap outside must not choose on the user's behalf.
 */
export function DestructiveDialog({
  visible,
  title,
  consequence,
  confirmLabel,
  cancelLabel = 'Huỷ',
  onConfirm,
  onCancel,
  testID = 'destructive-dialog',
}: DestructiveDialogProps) {
  const theme = useAppTheme();

  return (
    <OverlayShell
      accessibilityLabel={title}
      dismissPolicy="explicit-only"
      onDismiss={onCancel}
      testID={testID}
      visible={visible}
    >
      <Text accessibilityRole="header" testID={`${testID}-title`} variant="titleMedium">
        {title}
      </Text>

      <Text
        style={{ color: theme.colors.onSurface }}
        testID={`${testID}-consequence`}
        variant="bodyLarge"
      >
        {consequence}
      </Text>

      <View style={styles.actions}>
        {/** The safe route is first and full width; the destructive one sits apart below it. */}
        <ActionButton onPress={onCancel} testID={`${testID}-cancel`} variant="secondary">
          {cancelLabel}
        </ActionButton>

        <View style={[styles.separator, { backgroundColor: theme.colors.outlineVariant }]} />

        <ActionButton onPress={onConfirm} testID={`${testID}-confirm`} variant="destructive">
          {confirmLabel}
        </ActionButton>
      </View>
    </OverlayShell>
  );
}

const styles = StyleSheet.create({
  actions: {
    gap: spacing.sm,
  },
  separator: {
    height: StyleSheet.hairlineWidth,
    marginVertical: spacing.xs,
  },
});
