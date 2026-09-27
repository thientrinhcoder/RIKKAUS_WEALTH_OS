import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { DestinationItem } from './destination-item';
import { DESTINATIONS, type Destination } from './destinations';

interface BottomNavProps {
  activeRoute: string;
  onNavigate: (destination: Destination) => void;
}

export function BottomNav({ activeRoute, onNavigate }: BottomNavProps) {
  const theme = useAppTheme();
  const insets = useSafeAreaInsets();

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
  bar: {
    borderTopWidth: StyleSheet.hairlineWidth,
    flexDirection: 'row',
    paddingTop: spacing.xs,
  },
  item: {
    flex: 1,
  },
});
