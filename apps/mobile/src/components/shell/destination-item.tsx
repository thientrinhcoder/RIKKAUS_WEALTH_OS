import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';
import { Icon, Text, TouchableRipple } from 'react-native-paper';

import { sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import type { Destination } from './destinations';

/**
 * One destination, shared by all three chromes.
 *
 * Section 5.4 requires navigation icons to always carry a text label, so the label is not
 * optional and no chrome may drop it. The selected state is exposed through accessibility
 * rather than colour alone, per section 12.
 */
interface DestinationItemProps {
  destination: Destination;
  selected: boolean;
  onPress: (destination: Destination) => void;
  orientation: 'vertical' | 'horizontal';
  style?: StyleProp<ViewStyle>;
}

export function DestinationItem({
  destination,
  selected,
  onPress,
  orientation,
  style,
}: DestinationItemProps) {
  const theme = useAppTheme();
  const tint = selected ? theme.colors.primary : theme.colors.onSurfaceVariant;

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
        <Icon color={tint} size={24} source={destination.icon} />
        <Text
          style={{ color: tint }}
          variant={orientation === 'vertical' ? 'labelSmall' : 'labelLarge'}
        >
          {destination.label}
        </Text>
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
  horizontalInner: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: spacing.sm,
  },
});
