import { StyleSheet, View } from 'react-native';
import { Text, TouchableRipple } from 'react-native-paper';

import { radius, sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

export interface SegmentItem {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface SegmentedControlProps {
  items: readonly SegmentItem[];
  value: string;
  onChange: (value: string) => void;
  accessibilityLabel: string;
  testID?: string;
}

/**
 * Changes how one set of content is presented or narrowed, per section 8.1. That makes it a
 * filter, so it follows the filter rules: the caller holds the state and the result stays
 * visible while the control changes.
 *
 * Exactly one segment is selected at any time. Labels come from props; the control holds no
 * domain vocabulary.
 */
export function SegmentedControl({
  items,
  value,
  onChange,
  accessibilityLabel,
  testID = 'segmented',
}: SegmentedControlProps) {
  const theme = useAppTheme();

  return (
    <View
      accessibilityLabel={accessibilityLabel}
      accessibilityRole="radiogroup"
      style={[styles.group, { borderColor: theme.colors.outline }]}
      testID={testID}
    >
      {items.map((item) => {
        const selected = item.value === value;
        const disabled = item.disabled === true;

        return (
          <TouchableRipple
            accessibilityLabel={item.label}
            accessibilityRole="radio"
            accessibilityState={{ checked: selected, disabled }}
            aria-checked={selected}
            aria-disabled={disabled}
            disabled={disabled}
            key={item.value}
            onPress={() => onChange(item.value)}
            style={[
              styles.segment,
              selected && { backgroundColor: theme.colors.primaryContainer },
            ]}
            testID={`${testID}-${item.value}`}
          >
            <Text
              style={{
                color: selected ? theme.colors.onPrimaryContainer : theme.colors.onSurfaceVariant,
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
  group: {
    borderRadius: radius.input,
    borderWidth: 1,
    flexDirection: 'row',
    overflow: 'hidden',
  },
  segment: {
    alignItems: 'center',
    flex: 1,
    justifyContent: 'center',
    minHeight: sizing.minTouchTarget.android,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.sm,
  },
});
