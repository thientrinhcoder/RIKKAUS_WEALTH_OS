import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';
import { Icon, Text, TouchableRipple } from 'react-native-paper';

import { radius, sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import type { Destination } from './destinations';

/**
 * One destination, shared by all three chromes.
 *
 * Section 5.4 requires navigation icons to carry a text label. The bottom bar gives each of five
 * destinations a fifth of the width, so it shows the short label and never wraps it: a wrapped
 * label made the bar taller than its neighbours and pushed the row out of alignment. Below the
 * width where even a short label reads as truncated, the visible text is dropped and the icon
 * stands alone.
 *
 * Dropping the visible text never drops the name. The accessible label is always the full one,
 * so a screen reader announces "Tài sản & Nợ" at every width.
 *
 * The selected state is exposed through accessibility rather than colour alone, per section 12.
 */

/**
 * Below this, a fifth of the width is under about 64pt, which is not enough for a readable label
 * beside a 24pt icon inside a 44pt target.
 */
export const LABEL_HIDDEN_BELOW_WIDTH = 340;

interface DestinationItemProps {
  destination: Destination;
  selected: boolean;
  onPress: (destination: Destination) => void;
  orientation: 'vertical' | 'horizontal';
  /** The bottom bar passes its short label; the rail and sidebar pass the full one. */
  labelVariant?: 'full' | 'short';
  /** When false the label is not rendered, leaving the icon and its accessible name. */
  showLabel?: boolean;
  style?: StyleProp<ViewStyle>;
}

export function DestinationItem({
  destination,
  selected,
  onPress,
  orientation,
  labelVariant = 'full',
  showLabel = true,
  style,
}: DestinationItemProps) {
  const theme = useAppTheme();
  const tint = selected ? theme.colors.primary : theme.colors.onSurfaceVariant;
  const visibleLabel =
    labelVariant === 'short' ? destination.shortLabel : destination.label;

  return (
    <TouchableRipple
      accessibilityLabel={destination.label}
      accessibilityRole="tab"
      accessibilityState={{ selected }}
      /**
       * react-native-web does not translate accessibilityState.selected into aria-selected for
       * this element, so a screen reader on web could not tell which destination was active.
       * Both are declared: the first serves native, the second serves web.
       */
      aria-selected={selected}
      onPress={() => onPress(destination)}
      style={[styles.item, orientation === 'horizontal' && styles.horizontal, style]}
      testID={`shell-destination-${destination.key}`}
    >
      <View style={orientation === 'vertical' ? styles.vertical : styles.horizontalInner}>
        {/**
          * The active destination carries a filled pill behind its icon and a heavier label, not
          * just a different colour. Section 12 forbids a distinction that exists only in colour,
          * and a tint change alone was genuinely hard to see.
          */}
        <View
          style={[
            styles.iconSlot,
            selected && { backgroundColor: theme.colors.primaryContainer },
          ]}
          testID={`shell-destination-${destination.key}-indicator`}
        >
          <Icon color={tint} size={24} source={destination.icon} />
        </View>

        {showLabel ? (
          <Text
            /** Never wraps: a two-line label makes this item taller than its siblings. */
            numberOfLines={1}
            style={{ color: tint, fontWeight: selected ? '600' : '400' }}
            variant={orientation === 'vertical' ? 'labelSmall' : 'labelLarge'}
          >
            {visibleLabel}
          </Text>
        ) : null}
      </View>
    </TouchableRipple>
  );
}

const styles = StyleSheet.create({
  item: {
    justifyContent: 'center',
    minHeight: sizing.minTouchTarget.android,
    minWidth: sizing.minTouchTarget.android,
    paddingHorizontal: spacing.xs,
    paddingVertical: spacing.xs,
  },
  horizontal: {
    alignSelf: 'stretch',
  },
  vertical: {
    alignItems: 'center',
    gap: spacing.xs,
  },
  iconSlot: {
    alignItems: 'center',
    borderRadius: radius.sheet,
    justifyContent: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
  },
  horizontalInner: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: spacing.sm,
  },
});
