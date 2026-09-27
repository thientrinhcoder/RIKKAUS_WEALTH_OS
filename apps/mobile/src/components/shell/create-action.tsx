import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Surface, Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { elevation } from '@/ui/elevation';
import { radius, sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * The five record types the global create action offers, per section 6.2.
 *
 * The sheet only selects a type; the form opens as a navigable screen so back behaviour,
 * drafts, keyboard avoidance, validation and deep links stay predictable.
 */
export const CREATE_RECORD_TYPES = [
  { key: 'asset', label: 'Tài sản', route: '/holdings/new-asset' },
  { key: 'liability', label: 'Công nợ', route: '/holdings/new-liability' },
  { key: 'income', label: 'Thu nhập', route: '/cash-flow/new-income' },
  { key: 'expense', label: 'Chi phí', route: '/cash-flow/new-expense' },
  { key: 'goal', label: 'Mục tiêu', route: '/goals/new' },
] as const;

export type CreateRecordType = (typeof CREATE_RECORD_TYPES)[number];

interface CreateActionProps {
  onSelectRecordType: (recordType: CreateRecordType) => void;
}

/**
 * Deliberate seam: the sheet below is a temporary inline presentation so this phase stays
 * independent of the overlay slice. Phase 8 replaces it with the shared bottom sheet from
 * #119. The selection contract above it does not change when that happens.
 */
export function CreateAction({ onSelectRecordType }: CreateActionProps) {
  const theme = useAppTheme();
  const [open, setOpen] = useState(false);

  const choose = (recordType: CreateRecordType) => {
    setOpen(false);
    onSelectRecordType(recordType);
  };

  return (
    <View testID="shell-create-action">
      <ActionButton
        icon="plus"
        onPress={() => setOpen(true)}
        testID="shell-create-trigger"
        variant="primary"
      >
        Thêm
      </ActionButton>

      {open ? (
        <Surface
          elevation={elevation.modal.level}
          style={[styles.sheet, { backgroundColor: theme.colors.surface }]}
          testID="shell-create-sheet"
        >
          <Text accessibilityRole="header" variant="titleMedium">
            Thêm bản ghi mới
          </Text>

          {CREATE_RECORD_TYPES.map((recordType) => (
            <ActionButton
              key={recordType.key}
              onPress={() => choose(recordType)}
              testID={`shell-create-${recordType.key}`}
              variant="text"
            >
              {recordType.label}
            </ActionButton>
          ))}
        </Surface>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  sheet: {
    borderTopLeftRadius: radius.sheet,
    borderTopRightRadius: radius.sheet,
    gap: spacing.sm,
    minHeight: sizing.minControlHeight,
    padding: spacing.md,
  },
});
