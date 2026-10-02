import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { DestinationItem } from './destination-item';
import { DESTINATIONS, type Destination } from './destinations';

interface NavRailProps {
  activeRoute: string;
  onNavigate: (destination: Destination) => void;
}

export function NavRail({ activeRoute, onNavigate }: NavRailProps) {
  const theme = useAppTheme();
  const insets = useSafeAreaInsets();

  return (
    <View
      accessibilityRole="tablist"
      style={[
        styles.rail,
        {
          backgroundColor: theme.colors.surface,
          borderRightColor: theme.colors.outlineVariant,
          paddingBottom: insets.bottom,
          paddingTop: insets.top + spacing.sm,
        },
      ]}
      testID="shell-nav-rail"
    >
      {DESTINATIONS.map((destination) => (
        <DestinationItem
          destination={destination}
          key={destination.key}
          onPress={onNavigate}
          orientation="vertical"
          selected={destination.route === activeRoute}
          style={styles.item}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  rail: {
    borderRightWidth: StyleSheet.hairlineWidth,
    gap: spacing.sm,
    paddingHorizontal: spacing.xs,
    width: 88,
  },
  item: {
    alignSelf: 'stretch',
  },
});
