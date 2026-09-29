import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react';
import { StyleSheet, View } from 'react-native';
import { Surface, Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { elevation } from '@/ui/elevation';
import { radius, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * Section 11.1 gives a snackbar a minimum dwell of five seconds, longer when it carries an
 * action, an explicit dismiss route, and at most one visible at a time. A failure the user must
 * act on never arrives only this way, because a snackbar can expire unread.
 */
export const SNACKBAR_MINIMUM_MS = 5000;
export const SNACKBAR_WITH_ACTION_MS = 10000;

export interface SnackbarMessage {
  id: string;
  message: string;
  actionLabel?: string;
  onAction?: () => void;
}

export interface SnackbarApi {
  /** Shows a message, or queues it behind one already on screen. */
  show: (message: Omit<SnackbarMessage, 'id'>) => void;
  dismiss: () => void;
  /** The message currently on screen, or null. */
  current: SnackbarMessage | null;
  /** How many messages are waiting behind it. */
  queued: number;
  durationFor: (message: Pick<SnackbarMessage, 'actionLabel'>) => number;
}

const SnackbarContext = createContext<SnackbarApi | null>(null);

export function useSnackbar(): SnackbarApi {
  const api = useContext(SnackbarContext);

  if (api === null) {
    throw new Error('useSnackbar must be used inside SnackbarProvider');
  }

  return api;
}

export function durationFor(message: Pick<SnackbarMessage, 'actionLabel'>): number {
  return message.actionLabel === undefined ? SNACKBAR_MINIMUM_MS : SNACKBAR_WITH_ACTION_MS;
}

let nextId = 0;

export function SnackbarProvider({ children }: PropsWithChildren) {
  const theme = useAppTheme();
  const [queue, setQueue] = useState<SnackbarMessage[]>([]);

  const show = useCallback((message: Omit<SnackbarMessage, 'id'>) => {
    nextId += 1;
    /**
     * Queued rather than replacing what is on screen. Replacing would let a message the user
     * never read disappear, and a message carrying an action would lose the action with it.
     */
    setQueue((waiting) => [...waiting, { ...message, id: `snackbar-${nextId}` }]);
  }, []);

  const dismiss = useCallback(() => {
    setQueue((waiting) => waiting.slice(1));
  }, []);

  const current = queue[0] ?? null;

  /**
   * A snackbar that never leaves is a banner. Section 11.1 gives it a dwell of at least five
   * seconds, or longer when it carries an action so the action can actually be reached, after
   * which the next queued message takes its place.
   */
  useEffect(() => {
    if (current === null) {
      return;
    }

    const timer = setTimeout(() => {
      setQueue((waiting) => waiting.slice(1));
    }, durationFor(current));

    return () => clearTimeout(timer);
  }, [current]);

  const api = useMemo<SnackbarApi>(
    () => ({
      show,
      dismiss,
      current,
      queued: Math.max(0, queue.length - 1),
      durationFor,
    }),
    [current, dismiss, queue.length, show],
  );

  return (
    <SnackbarContext.Provider value={api}>
      {children}

      {current === null ? null : (
        <Surface
          /**
           * Polite, and never focused: section 12 requires a toast not to steal focus.
           */
          accessibilityLiveRegion="polite"
          elevation={elevation.raisedCard.level}
          style={[styles.snackbar, { backgroundColor: theme.colors.brandNavy }]}
          testID="snackbar"
        >
          <Text
            style={[styles.message, { color: theme.colors.surface }]}
            testID="snackbar-message"
            variant="bodyLarge"
          >
            {current.message}
          </Text>

          <View style={styles.actions}>
            {current.actionLabel === undefined ? null : (
              <ActionButton
                onPress={() => {
                  current.onAction?.();
                  dismiss();
                }}
                testID="snackbar-action"
                variant="text"
              >
                {current.actionLabel}
              </ActionButton>
            )}

            <ActionButton
              accessibilityLabel="Đóng thông báo"
              icon="close"
              onPress={dismiss}
              testID="snackbar-dismiss"
              variant="text"
            />
          </View>
        </Surface>
      )}
    </SnackbarContext.Provider>
  );
}

const styles = StyleSheet.create({
  snackbar: {
    alignItems: 'center',
    borderRadius: radius.input,
    bottom: spacing.md,
    flexDirection: 'row',
    gap: spacing.sm,
    left: spacing.md,
    padding: spacing.md,
    position: 'absolute',
    right: spacing.md,
  },
  message: {
    flex: 1,
  },
  actions: {
    alignItems: 'center',
    flexDirection: 'row',
  },
});
