import { Redirect } from 'expo-router';
import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

import { classifyApiError } from '@/api';
import { ErrorState, LoadingState } from '@/components/feedback';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { useSessionLiveness } from './session-liveness';
import { useSession, useSessionStore } from './session-provider';
import { SignInFlow } from './sign-in-flow';

/**
 * Route protection for everything inside the signed-in shell. Protected content is rendered only
 * for a live session; every other state replaces it outright, so an expired or revoked session
 * never leaves financial data readable behind a dialog.
 */
export function SessionGate({ children }: { children: ReactNode }) {
  const session = useSession();
  const store = useSessionStore();

  switch (session.status) {
    case 'signed-in':
      return (
        <>
          <SessionLiveness />
          {children}
        </>
      );
    case 'signed-out':
      return <Redirect href="/sign-in" />;
    case 'ended':
      return (
        <SignInFlow
          key={session.endReason}
          initialPhase={session.endReason ?? 'expired'}
          onBackToStart={() => store.acknowledgeEnded()}
        />
      );
    case 'unreachable': {
      const failure = classifyApiError(session.restoreError);

      return (
        <GateFrame>
          <ErrorState
            message={failure.title}
            onRetry={() => void store.restore()}
            preserved={`Chưa kiểm tra được phiên đăng nhập, nên dữ liệu tài chính chưa được hiển thị. ${failure.message}`}
            retryLabel="Thử lại"
            testID="session-unreachable"
            variant={failure.variant}
          />
        </GateFrame>
      );
    }
    case 'restoring':
      return (
        <GateFrame>
          <LoadingState
            accessibilityLabel="Đang kiểm tra phiên đăng nhập"
            reservedHeight={160}
            testID="session-restoring"
          />
        </GateFrame>
      );
  }
}

function SessionLiveness() {
  useSessionLiveness();

  return null;
}

function GateFrame({ children }: { children: ReactNode }) {
  const theme = useAppTheme();

  return (
    <View style={[styles.frame, { backgroundColor: theme.colors.background }]}>
      <View style={styles.column}>{children}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  frame: {
    flex: 1,
    justifyContent: 'center',
    padding: spacing.md,
  },
  column: {
    alignSelf: 'center',
    maxWidth: 480,
    width: '100%',
  },
});
