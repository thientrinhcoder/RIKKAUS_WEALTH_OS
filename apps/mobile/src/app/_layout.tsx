import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';

import { AppProviders } from '@/providers/app-providers';
import { fontAssets } from '@/ui/fonts';

/**
 * Hold the splash screen until the approved typefaces are ready. Without this the first
 * screen paints in a fallback family and then reflows when IBM Plex Sans arrives, which is
 * exactly the layout shift section 5.5 tells us not to put in front of financial values.
 */
void SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const [fontsLoaded, fontError] = useFonts(fontAssets);

  useEffect(() => {
    /**
     * A font that fails to load must not leave the user on a splash screen forever. Render in
     * the platform fallback instead; every approved contrast pair and line height still holds.
     */
    if (fontsLoaded || fontError) {
      void SplashScreen.hideAsync();
    }
  }, [fontsLoaded, fontError]);

  if (!fontsLoaded && !fontError) {
    return null;
  }

  return (
    <AppProviders>
      <Stack screenOptions={{ headerShown: false }} />
      <StatusBar style="auto" />
    </AppProviders>
  );
}
