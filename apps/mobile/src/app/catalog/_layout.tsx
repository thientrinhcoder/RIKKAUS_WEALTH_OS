import { Slot } from 'expo-router';
import { ScrollView, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useResponsiveLayout } from '@/ui/responsive';
import { sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * The component catalog.
 *
 * It is a route group rather than a separate harness so every example runs against the real
 * theme, the real providers and the real primitives. A separate harness would prove less: it
 * could pass while the product was broken.
 *
 * It is deliberately outside the five primary destinations and never appears in navigation.
 */
export default function CatalogLayout() {
  const theme = useAppTheme();
  const { horizontalPadding } = useResponsiveLayout();

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: theme.colors.background }]}>
      <ScrollView
        contentContainerStyle={[styles.content, { paddingHorizontal: horizontalPadding }]}
        testID="catalog-scroll"
      >
        <Slot />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  content: {
    alignSelf: 'center',
    flexGrow: 1,
    gap: spacing.lg,
    maxWidth: sizing.shellContentMaxWidth,
    paddingVertical: spacing.lg,
    width: '100%',
  },
});
