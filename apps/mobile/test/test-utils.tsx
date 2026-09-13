import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, type RenderOptions } from '@testing-library/react-native';
import type { ReactElement, PropsWithChildren } from 'react';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { appTheme } from '@/ui/theme';

const initialWindowMetrics = {
  frame: { x: 0, y: 0, width: 375, height: 812 },
  insets: { top: 0, left: 0, right: 0, bottom: 0 },
};

export function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        gcTime: Infinity,
        retry: false,
      },
    },
  });
}

export async function renderWithProviders(ui: ReactElement, options?: RenderOptions) {
  const queryClient = createTestQueryClient();

  function Wrapper({ children }: PropsWithChildren) {
    return (
      <SafeAreaProvider initialMetrics={initialWindowMetrics}>
        <QueryClientProvider client={queryClient}>
          <PaperProvider theme={appTheme}>{children}</PaperProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    );
  }

  const renderResult = await render(ui, { wrapper: Wrapper, ...options });

  return {
    queryClient,
    ...renderResult,
  };
}
