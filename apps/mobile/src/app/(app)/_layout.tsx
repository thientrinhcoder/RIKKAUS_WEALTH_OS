import { Slot, usePathname, useRouter } from 'expo-router';

import { AppShell } from '@/components/shell';

/**
 * Mounts the shell around every primary destination. The shell owns navigation chrome; the
 * slot renders whichever destination the router resolved. No domain logic lives here.
 */
export default function AppGroupLayout() {
  const pathname = usePathname();
  const router = useRouter();

  return (
    <AppShell
      activeRoute={pathname}
      onCreate={(recordType) => router.push(recordType.route as never)}
      onNavigate={(destination) => router.push(destination.route as never)}
      onOpenProfile={() => router.push('/profile' as never)}
    >
      <Slot />
    </AppShell>
  );
}
