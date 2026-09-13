import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState, type PropsWithChildren } from 'react';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { appTheme } from '@/ui/theme';

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
        <PaperProvider theme={appTheme}>{children}</PaperProvider>
      </QueryClientProvider>
    </SafeAreaProvider>
  );
}
