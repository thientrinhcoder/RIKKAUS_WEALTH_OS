import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { DestinationItem } from './destination-item';
import { DESTINATIONS, type Destination } from './destinations';

interface SidebarProps {
  activeRoute: string;
  onNavigate: (destination: Destination) => void;
}

export function Sidebar({ activeRoute, onNavigate }: SidebarProps) {
  const theme = useAppTheme();
  const insets = useSafeAreaInsets();

  return (
    <View
      accessibilityRole="tablist"
      style={[
        styles.sidebar,
        {
          backgroundColor: theme.colors.surface,
          borderRightColor: theme.colors.outlineVariant,
          paddingBottom: insets.bottom,
          paddingTop: insets.top + spacing.md,
        },
      ]}
      testID="shell-sidebar"
    >
      {DESTINATIONS.map((destination) => (
        <DestinationItem
          destination={destination}
          key={destination.key}
          onPress={onNavigate}
          orientation="horizontal"
          selected={destination.route === activeRoute}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  sidebar: {
    borderRightWidth: StyleSheet.hairlineWidth,
    gap: spacing.xs,
    paddingHorizontal: spacing.sm,
    width: 248,
  },
});
