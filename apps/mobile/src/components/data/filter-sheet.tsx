import { useState, type ComponentType, type ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { spacing } from '@/ui/tokens';

export interface FilterSheetContainerProps {
  visible: boolean;
  onDismiss: () => void;
  children: ReactNode;
  testID?: string;
}

export interface FilterSheetProps<Filters> {
  visible: boolean;
  onDismiss: () => void;
  /** The filters currently applied to the list behind the sheet. */
  applied: Filters;
  onApply: (filters: Filters) => void;
  /** Renders the filter controls over the staged value. */
  children: (staged: Filters, setStaged: (filters: Filters) => void) => ReactNode;
  /**
   * The presentation container. Taken as a prop so this phase stays independent of the overlay
   * slice; the shared bottom sheet is substituted here once it exists.
   */
  container: ComponentType<FilterSheetContainerProps>;
  title?: string;
  testID?: string;
}

/**
 * The mobile filter sheet from section 8.1.
 *
 * Changes are staged and applied on an explicit action, so a filter is never applied while the
 * sheet still covers the result the user is trying to judge. Dismissing discards the staged
 * changes and leaves the applied filters untouched.
 */
export function FilterSheet<Filters>({
  visible,
  onDismiss,
  applied,
  onApply,
  children,
  container: Container,
  title = 'Bộ lọc',
  testID = 'filter-sheet',
}: FilterSheetProps<Filters>) {
  const [staged, setStaged] = useState<Filters>(applied);
  const [wasVisible, setWasVisible] = useState(visible);

  /**
   * Reopening starts from what is actually applied, not from a discarded edit. Adjusting during
   * render rather than in an effect avoids a pass where the sheet shows the stale staged value.
   */
  if (visible !== wasVisible) {
    setWasVisible(visible);

    if (visible) {
      setStaged(applied);
    }
  }

  return (
    <Container onDismiss={onDismiss} testID={`${testID}-container`} visible={visible}>
      <View style={styles.sheet} testID={testID}>
        <Text accessibilityRole="header" variant="titleMedium">
          {title}
        </Text>

        {children(staged, setStaged)}

        <View style={styles.actions}>
          <ActionButton onPress={onDismiss} testID={`${testID}-cancel`} variant="text">
            Huỷ
          </ActionButton>

          <ActionButton
            onPress={() => onApply(staged)}
            testID={`${testID}-apply`}
            variant="primary"
          >
            Áp dụng
          </ActionButton>
        </View>
      </View>
    </Container>
  );
}

const styles = StyleSheet.create({
  sheet: {
    gap: spacing.md,
    padding: spacing.md,
  },
  actions: {
    flexDirection: 'row',
    gap: spacing.sm,
    justifyContent: 'flex-end',
  },
});
