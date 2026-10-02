import { StyleSheet, View } from 'react-native';
import { Text, TouchableRipple } from 'react-native-paper';

import { sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

export interface TabItem {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface TabsProps {
  items: readonly TabItem[];
  value: string;
  onChange: (value: string) => void;
  /** Named so assistive technology can tell one tab list from another on the same screen. */
  accessibilityLabel: string;
  testID?: string;
}

/**
 * Tabs switch between sibling sections that each own their content and scroll position, per
 * section 8.1. To narrow one set of content, use the segmented control instead.
 */
export function Tabs({ items, value, onChange, accessibilityLabel, testID = 'tabs' }: TabsProps) {
  const theme = useAppTheme();

  return (
    <View
      accessibilityLabel={accessibilityLabel}
      accessibilityRole="tablist"
      style={[styles.list, { borderBottomColor: theme.colors.outlineVariant }]}
      testID={testID}
    >
      {items.map((item) => {
        const selected = item.value === value;
        const disabled = item.disabled === true;

        return (
          <TouchableRipple
            accessibilityLabel={item.label}
            accessibilityRole="tab"
            accessibilityState={{ selected, disabled }}
            aria-disabled={disabled}
            aria-selected={selected}
            disabled={disabled}
            key={item.value}
            onPress={() => onChange(item.value)}
            style={[
              styles.tab,
              selected && { borderBottomColor: theme.colors.primary, borderBottomWidth: 2 },
            ]}
            testID={`${testID}-${item.value}`}
          >
            <Text
              style={{
                color: disabled
                  ? theme.colors.onSurfaceVariant
                  : selected
                    ? theme.colors.primary
                    : theme.colors.onSurfaceVariant,
              }}
              variant="labelLarge"
            >
              {item.label}
            </Text>
          </TouchableRipple>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  list: {
    borderBottomWidth: StyleSheet.hairlineWidth,
    flexDirection: 'row',
  },
  tab: {
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: sizing.minTouchTarget.android,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
});
