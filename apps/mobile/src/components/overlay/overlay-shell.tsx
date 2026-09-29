import { useEffect, useRef, type ReactNode } from 'react';
import { BackHandler, Platform, Pressable, StyleSheet, View } from 'react-native';
import { Portal, Surface } from 'react-native-paper';

import { elevation } from '@/ui/elevation';
import { radius, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * Every overlay's accessibility guarantees live here, once.
 *
 * Focus trapping and focus return reimplemented per dialog would diverge, and divergence in this
 * area only shows up with a screen reader, which is the last place it gets noticed.
 *
 * Section 12 groups overlays by what dismissing costs. An overlay that loses nothing accepts
 * every dismissal route including a scrim tap; one that would discard data or confirm a
 * destructive action accepts only an explicit choice, because an accidental tap outside would
 * silently choose for the user.
 *
 * It renders through a portal at the app root. Absolute positioning resolves against the nearest
 * positioned ancestor, so an overlay rendered in place inside a scrolling screen is trapped in
 * the content box: it scrolls with the content and its scrim dims only that box rather than the
 * screen. Every real screen scrolls, so this is not an edge case.
 */
export type DismissPolicy = 'any-route' | 'explicit-only';

export interface OverlayShellProps {
  visible: boolean;
  /** The safe route out. It must make no change. */
  onDismiss: () => void;
  children: ReactNode;
  /** Names the overlay for assistive technology. */
  accessibilityLabel: string;
  dismissPolicy?: DismissPolicy;
  /** Sheets sit against the bottom edge; dialogs are centred. */
  placement?: 'center' | 'bottom';
  testID?: string;
}

export function OverlayShell({
  visible,
  onDismiss,
  children,
  accessibilityLabel,
  dismissPolicy = 'any-route',
  placement = 'center',
  testID = 'overlay',
}: OverlayShellProps) {
  const theme = useAppTheme();

  /** The element that had focus before the overlay opened, so it can be restored on close. */
  const invoker = useRef<{ focus?: () => void } | null>(null);

  useEffect(() => {
    if (!visible) {
      return;
    }

    if (Platform.OS === 'web' && typeof document !== 'undefined') {
      invoker.current = document.activeElement as unknown as { focus?: () => void };
    }

    return () => {
      /** Section 12: focus returns to the invoking element when the overlay closes. */
      invoker.current?.focus?.();
      invoker.current = null;
    };
  }, [visible]);

  useEffect(() => {
    if (!visible || Platform.OS === 'web') {
      return;
    }

    /** The platform back gesture resolves to the dismiss route for both policies. */
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      onDismiss();
      return true;
    });

    return () => subscription.remove();
  }, [onDismiss, visible]);

  useEffect(() => {
    if (!visible || Platform.OS !== 'web' || typeof document === 'undefined') {
      return;
    }

    /**
     * Escape is bound at the document rather than to the panel, so it works wherever focus
     * currently sits inside the overlay. Section 12 makes Escape a dismissal route under both
     * policies, since it is an explicit choice by the user.
     */
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onDismiss();
      }
    };

    document.addEventListener('keydown', onKeyDown);

    return () => document.removeEventListener('keydown', onKeyDown);
  }, [onDismiss, visible]);

  if (!visible) {
    return null;
  }

  const scrimDismisses = dismissPolicy === 'any-route';

  return (
    <Portal>
      <View
        /** Background content is inert to assistive technology while this is open. */
        accessibilityViewIsModal
        aria-modal
        style={[styles.layer, placement === 'bottom' && styles.bottom]}
        testID={testID}
      >
        <Pressable
          accessibilityElementsHidden
          aria-hidden
          importantForAccessibility="no-hide-descendants"
          onPress={scrimDismisses ? onDismiss : undefined}
          style={[styles.scrim, { backgroundColor: elevation.modal.scrim }]}
          testID={`${testID}-scrim`}
        />

        <Surface
          accessibilityLabel={accessibilityLabel}
          accessibilityRole="none"
          elevation={elevation.modal.level}
          style={[
            styles.panel,
            placement === 'bottom' ? styles.panelBottom : styles.panelCenter,
            { backgroundColor: theme.colors.surface },
          ]}
          testID={`${testID}-panel`}
        >
          {children}
        </Surface>
      </View>
    </Portal>
  );
}

const styles = StyleSheet.create({
  layer: {
    alignItems: 'center',
    bottom: 0,
    justifyContent: 'center',
    left: 0,
    position: 'absolute',
    right: 0,
    top: 0,
  },
  bottom: {
    justifyContent: 'flex-end',
  },
  scrim: {
    bottom: 0,
    left: 0,
    position: 'absolute',
    right: 0,
    top: 0,
  },
  panel: {
    gap: spacing.md,
    maxWidth: 560,
    padding: spacing.md,
    width: '100%',
  },
  panelCenter: {
    borderRadius: radius.dialog,
  },
  panelBottom: {
    borderTopLeftRadius: radius.sheet,
    borderTopRightRadius: radius.sheet,
  },
});
