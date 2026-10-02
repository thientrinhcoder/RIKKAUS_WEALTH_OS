import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { OverlayShell } from './overlay-shell';

export interface UnsavedChangesDialogProps {
  /** Comes from the form kit's dirty state. A clean form never raises this dialog. */
  isDirty: boolean;
  visible: boolean;
  title?: string;
  consequence?: string;
  discardLabel?: string;
  keepEditingLabel?: string;
  /** Reports the intent. This dialog mutates nothing itself. */
  onDiscard: () => void;
  onKeepEditing: () => void;
  testID?: string;
}

/**
 * The warning before a form with unsaved changes is abandoned.
 *
 * It opens only when the form is actually dirty: warning about losing nothing trains people to
 * dismiss the warning without reading it. Like the destructive dialog, the scrim does not
 * dismiss it, because dismissing would discard the user's work.
 */
export function UnsavedChangesDialog({
  isDirty,
  visible,
  title = 'Thay đổi chưa được lưu',
  consequence = 'Nếu rời khỏi đây, những thay đổi bạn vừa nhập sẽ mất.',
  discardLabel = 'Rời đi và bỏ thay đổi',
  keepEditingLabel = 'Tiếp tục chỉnh sửa',
  onDiscard,
  onKeepEditing,
  testID = 'unsaved-changes-dialog',
}: UnsavedChangesDialogProps) {
  const theme = useAppTheme();

  return (
    <OverlayShell
      accessibilityLabel={title}
      dismissPolicy="explicit-only"
      onDismiss={onKeepEditing}
      testID={testID}
      visible={visible && isDirty}
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
        <ActionButton
          onPress={onKeepEditing}
          testID={`${testID}-keep-editing`}
          variant="secondary"
        >
          {keepEditingLabel}
        </ActionButton>

        <View style={[styles.separator, { backgroundColor: theme.colors.outlineVariant }]} />

        <ActionButton onPress={onDiscard} testID={`${testID}-discard`} variant="destructive">
          {discardLabel}
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
