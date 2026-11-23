import { Slot, usePathname, useRouter } from 'expo-router';

import { AppShell } from '@/components/shell';
import { SessionGate } from '@/features/identity';

/**
 * Mounts the shell around every primary destination, behind the session gate: without a live
 * session the shell and its content are never rendered. The shell owns navigation chrome; the
 * slot renders whichever destination the router resolved. No domain logic lives here.
 */
export default function AppGroupLayout() {
  const pathname = usePathname();
  const router = useRouter();

  return (
    <SessionGate>
      <AppShell
        activeRoute={pathname}
        onCreate={(recordType) => router.push(recordType.route as never)}
        onNavigate={(destination) => router.push(destination.route as never)}
        onOpenProfile={() => router.push('/profile')}
      >
        <Slot />
      </AppShell>
    </SessionGate>
  );
}
