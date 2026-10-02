import { StyleSheet, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { DestinationItem, LABEL_HIDDEN_BELOW_WIDTH } from './destination-item';
import { DESTINATIONS, type Destination } from './destinations';

interface BottomNavProps {
  activeRoute: string;
  onNavigate: (destination: Destination) => void;
}

export function BottomNav({ activeRoute, onNavigate }: BottomNavProps) {
  const theme = useAppTheme();
  const insets = useSafeAreaInsets();
  const { width } = useWindowDimensions();

  /**
   * Five destinations share the width, so each gets a fifth of it. Below the threshold a label
   * cannot be read beside the icon, and the icon alone is clearer than a clipped word. The
   * accessible name is unaffected.
   */
  const showLabel = width >= LABEL_HIDDEN_BELOW_WIDTH;

  return (
    <View
      accessibilityRole="tablist"
      style={[
        styles.bar,
        {
          backgroundColor: theme.colors.surface,
          borderTopColor: theme.colors.outlineVariant,
          paddingBottom: insets.bottom,
        },
      ]}
      testID="shell-bottom-nav"
    >
      {DESTINATIONS.map((destination) => (
        <DestinationItem
          destination={destination}
          key={destination.key}
          labelVariant="short"
          onPress={onNavigate}
          orientation="vertical"
          selected={destination.route === activeRoute}
          showLabel={showLabel}
          style={styles.item}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    borderTopWidth: StyleSheet.hairlineWidth,
    flexDirection: 'row',
    paddingTop: spacing.xs,
  },
  item: {
    flex: 1,
  },
});
