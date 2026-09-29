import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

import { ActionButton } from '@/components/action';
import { spacing } from '@/ui/tokens';
import { OverlayShell, type DismissPolicy } from './overlay-shell';

export interface BottomSheetProps {
  visible: boolean;
  onDismiss: () => void;
  children: ReactNode;
  accessibilityLabel: string;
  dismissLabel?: string;
  /**
   * Sheets that lose nothing when dismissed accept every route. A sheet holding entered data
   * passes 'explicit-only', per section 12.
   */
  dismissPolicy?: DismissPolicy;
  testID?: string;
}

/**
 * The generic sheet container.
 *
 * This is the presentation the create action and the filter sheet were built to receive: both
 * take their container as a prop so those slices could ship before this one existed.
 */
export function BottomSheet({
  visible,
  onDismiss,
  children,
  accessibilityLabel,
  dismissLabel = 'Đóng',
  dismissPolicy = 'any-route',
  testID = 'bottom-sheet',
}: BottomSheetProps) {
  return (
    <OverlayShell
      accessibilityLabel={accessibilityLabel}
      dismissPolicy={dismissPolicy}
      onDismiss={onDismiss}
      placement="bottom"
      testID={testID}
      visible={visible}
    >
      {children}

      <View style={styles.dismissRow}>
        <ActionButton onPress={onDismiss} testID={`${testID}-dismiss`} variant="text">
          {dismissLabel}
        </ActionButton>
      </View>
    </OverlayShell>
  );
}

const styles = StyleSheet.create({
  dismissRow: {
    alignItems: 'flex-end',
    paddingTop: spacing.xs,
  },
});
