import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

import { useNavigationMode } from '@/ui/navigation-mode';
import { useResponsiveLayout } from '@/ui/responsive';
import { sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { BottomNav } from './bottom-nav';
import { CreateAction, type CreateRecordType } from './create-action';
import { NavRail } from './nav-rail';
import { Sidebar } from './sidebar';
import { TopAppBar } from './top-app-bar';
import { DESTINATIONS, type Destination } from './destinations';

interface AppShellProps {
  activeRoute: string;
  children: ReactNode;
  onNavigate?: (destination: Destination) => void;
  onOpenProfile?: () => void;
  onCreate?: (recordType: CreateRecordType) => void;
}

function titleForRoute(activeRoute: string): string {
  return DESTINATIONS.find((destination) => destination.route === activeRoute)?.label ?? '';
}

/**
 * The application shell: a layout boundary, not a screen. It selects the navigation chrome for
 * the current width, bounds the content area on desktop, and hosts the global create action.
 * It holds no domain logic; destinations render whatever the router puts in `children`.
 */
export function AppShell({
  activeRoute,
  children,
  onNavigate = () => {},
  onOpenProfile = () => {},
  onCreate = () => {},
}: AppShellProps) {
  const theme = useAppTheme();
  const mode = useNavigationMode();
  const { horizontalPadding } = useResponsiveLayout();

  const sideChrome =
    mode === 'rail' ? (
      <NavRail activeRoute={activeRoute} onNavigate={onNavigate} />
    ) : mode === 'sidebar' ? (
      <Sidebar activeRoute={activeRoute} onNavigate={onNavigate} />
    ) : null;

  return (
    <View
      style={[styles.root, { backgroundColor: theme.colors.background }]}
      testID="shell-root"
    >
      <View style={styles.body}>
        {sideChrome}

        <View style={styles.main}>
          <TopAppBar onOpenProfile={onOpenProfile} title={titleForRoute(activeRoute)} />

          <View
            style={[
              styles.content,
              { paddingHorizontal: horizontalPadding },
              /**
               * Section 5.3 bounds the shell's content area at desktop widths only. Below that
               * the column is already the width of the screen, so a maxWidth would do nothing
               * but complicate the layout.
               */
              mode === 'sidebar' && { maxWidth: sizing.shellContentMaxWidth },
            ]}
            testID="shell-content"
          >
            {children}
          </View>

          {/**
           * Anchored inside the shell, above the bottom navigation. The #36 review found this
           * action escaping the shell border on mobile, so its containment is asserted rather
           * than assumed.
           */}
          <View style={[styles.createSlot, { paddingHorizontal: horizontalPadding }]}>
            <CreateAction onSelectRecordType={onCreate} />
          </View>
        </View>
      </View>

      {mode === 'bottom' ? (
        <BottomNav activeRoute={activeRoute} onNavigate={onNavigate} />
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  body: {
    flex: 1,
    flexDirection: 'row',
  },
  main: {
    flex: 1,
  },
  content: {
    alignSelf: 'center',
    flex: 1,
    paddingVertical: spacing.lg,
    width: '100%',
  },
  createSlot: {
    alignItems: 'flex-end',
    paddingBottom: spacing.md,
  },
});
