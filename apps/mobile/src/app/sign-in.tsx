import { Redirect } from 'expo-router';
import { useState } from 'react';

import { LoadingState } from '@/components/feedback';
import { SignInFlow, useSession } from '@/features/identity';

/**
 * The single Google entry point for both first sign-in and returning users. A visitor with a live
 * session goes straight to Tổng quan; once this screen starts a sign-in it stays, to show the
 * result panel instead of redirecting mid-flow.
 */
export default function SignInScreen() {
  const session = useSession();
  const [startedHere, setStartedHere] = useState(false);

  if (session.status === 'restoring') {
    return <LoadingState accessibilityLabel="Đang kiểm tra phiên đăng nhập" reservedHeight={160} />;
  }

  if (session.status === 'signed-in' && !startedHere) return <Redirect href="/" />;

  return <SignInFlow onAttemptStart={() => setStartedHere(true)} />;
}
