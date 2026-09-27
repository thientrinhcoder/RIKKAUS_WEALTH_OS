import MaterialCommunityIcons from '@expo/vector-icons/MaterialCommunityIcons';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState, type PropsWithChildren } from 'react';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { SnackbarProvider } from '@/components/feedback/snackbar-provider';
import { appTheme } from '@/ui/theme';

/**
 * Section 5.4 requires one consistent vector icon family. Paper resolves icons through
 * react-native-vector-icons by default, which this project does not install, so every icon
 * rendered as an empty box on web. Naming the renderer explicitly keeps the family fixed and
 * makes the dependency visible rather than implicit.
 */
export const paperSettings = {
  icon: ({ name, color, size }: { name?: string; color?: string; size?: number }) => (
    <MaterialCommunityIcons color={color} name={name as never} size={size} />
  ),
};

export function createAppQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: 1,
        staleTime: 30_000,
      },
    },
  });
}

export function AppProviders({ children }: PropsWithChildren) {
  const [queryClient] = useState(createAppQueryClient);

  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <PaperProvider settings={paperSettings} theme={appTheme}>
          <SnackbarProvider>{children}</SnackbarProvider>
        </PaperProvider>
      </QueryClientProvider>
    </SafeAreaProvider>
  );
}
