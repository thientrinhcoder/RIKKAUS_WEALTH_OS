import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Card, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { isApiBaseUrlConfigured } from '@/features/health/health-client';
import { useHealthQuery } from '@/features/health/use-health-query';
import { CONTENT_MAX_WIDTH, useResponsiveLayout } from '@/ui/responsive';

function HealthStatus() {
  const apiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL;
  const configured = isApiBaseUrlConfigured(apiBaseUrl);
  const healthQuery = useHealthQuery(apiBaseUrl);

  if (!configured) {
    return (
      <View testID="health-state-empty" style={styles.stateContainer}>
        <Text accessibilityLiveRegion="polite" variant="titleMedium">
          API endpoint is not configured.
        </Text>
        <Text variant="bodyMedium">
          Set EXPO_PUBLIC_API_BASE_URL and restart Expo.
        </Text>
      </View>
    );
  }

  if (healthQuery.isPending) {
    return (
      <View testID="health-state-loading" style={styles.stateContainer}>
        <ActivityIndicator accessibilityLabel="Checking API health" />
        <Text accessibilityLiveRegion="polite" variant="titleMedium">
          Checking API health…
        </Text>
      </View>
    );
  }

  if (healthQuery.isError) {
    const reason =
      healthQuery.error instanceof Error
        ? healthQuery.error.message
        : 'The health check could not be completed.';

    return (
      <View testID="health-state-error" style={styles.stateContainer}>
        <Text accessibilityLiveRegion="assertive" variant="titleMedium">
          API health check failed.
        </Text>
        <Text variant="bodyMedium">{reason}</Text>
        <Button
          accessibilityLabel="Try API health check again"
          contentStyle={styles.retryButtonContent}
          disabled={healthQuery.isFetching}
          loading={healthQuery.isFetching}
          mode="contained"
          onPress={() => {
            void healthQuery.refetch();
          }}
        >
          Try again
        </Button>
      </View>
    );
  }

  return (
    <View testID="health-state-success" style={styles.stateContainer}>
      <Text accessibilityLiveRegion="polite" variant="titleMedium">
        API is reachable.
      </Text>
      <Text variant="bodyLarge">Status: {healthQuery.data.status}</Text>
    </View>
  );
}

export default function HomeScreen() {
  const { colors } = useTheme();
  const { horizontalPadding } = useResponsiveLayout();

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: colors.background }]}>
      <ScrollView
        contentContainerStyle={[styles.scrollContent, { paddingHorizontal: horizontalPadding }]}
        testID="screen-content"
      >
        <View style={styles.content}>
          <Text accessibilityRole="header" variant="headlineMedium">
            Rikkaus Wealth OS
          </Text>
          <Text variant="bodyLarge">
            Frontend foundation for local web and mobile development.
          </Text>

          <Card accessible accessibilityLabel="API health status" mode="contained">
            <Card.Content style={styles.cardContent}>
              <Text variant="titleLarge">API health</Text>
              <HealthStatus />
            </Card.Content>
          </Card>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    paddingVertical: 24,
  },
  content: {
    alignSelf: 'center',
    gap: 16,
    maxWidth: CONTENT_MAX_WIDTH,
    width: '100%',
  },
  cardContent: {
    gap: 16,
    minHeight: 184,
  },
  stateContainer: {
    alignItems: 'flex-start',
    gap: 12,
  },
  retryButtonContent: {
    minHeight: 48,
  },
});
