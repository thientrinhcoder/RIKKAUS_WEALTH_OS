import { Appbar } from 'react-native-paper';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAppTheme } from '@/ui/theme';

interface TopAppBarProps {
  title: string;
  onOpenProfile: () => void;
}

/**
 * Section 6.1 puts profile and settings behind the top app bar avatar rather than in primary
 * navigation. The avatar is icon-only, so section 5.4 requires it to carry an accessible label.
 */
export function TopAppBar({ title, onOpenProfile }: TopAppBarProps) {
  const theme = useAppTheme();
  const insets = useSafeAreaInsets();

  return (
    <Appbar.Header
      statusBarHeight={insets.top}
      style={{ backgroundColor: theme.colors.surface }}
      testID="shell-top-app-bar"
    >
      <Appbar.Content title={title} titleStyle={{ color: theme.colors.onSurface }} />
      <Appbar.Action
        accessibilityLabel="Hồ sơ và cài đặt"
        icon="account-circle-outline"
        onPress={onOpenProfile}
        testID="shell-profile-action"
      />
    </Appbar.Header>
  );
}
