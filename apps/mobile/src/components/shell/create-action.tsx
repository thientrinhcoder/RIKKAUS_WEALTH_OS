import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { BottomSheet } from '@/components/overlay';
import { spacing } from '@/ui/tokens';

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
 * The sheet is the shared bottom sheet from the overlay kit. This started as a temporary inline
 * presentation so the shell could merge before the overlay slice existed; the selection contract
 * did not change when the real container replaced it.
 *
 * It dismisses by any route: choosing a record type is not a decision that costs anything, so a
 * tap outside is a safe way out.
 */
export function CreateAction({ onSelectRecordType }: CreateActionProps) {
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

      <BottomSheet
        accessibilityLabel="Thêm bản ghi mới"
        onDismiss={() => setOpen(false)}
        testID="shell-create-sheet"
        visible={open}
      >
        <Text accessibilityRole="header" variant="titleMedium">
          Thêm bản ghi mới
        </Text>

        <View style={styles.options}>
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
        </View>
      </BottomSheet>
    </View>
  );
}

const styles = StyleSheet.create({
  options: {
    gap: spacing.xs,
  },
});
