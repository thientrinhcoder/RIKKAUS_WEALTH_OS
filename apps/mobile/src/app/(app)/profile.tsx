import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { classifyApiError } from '@/api';
import { ActionButton } from '@/components/action';
import { ErrorState } from '@/components/feedback';
import { ConfirmDialog } from '@/components/overlay';
import { ScreenHeading } from '@/components/shell/screen-heading';
import { useSession, useSessionStore } from '@/features/identity';
import { radius, sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * Profile, opened from the top app bar avatar. MVP 0 shows who is signed in and owns sign-out;
 * personal profile settings arrive with their own slice.
 */
export default function ProfileScreen() {
  const theme = useAppTheme();
  const session = useSession();
  const store = useSessionStore();
  const [confirming, setConfirming] = useState(false);
  const [signingOut, setSigningOut] = useState(false);
  const [failure, setFailure] = useState<string | null>(null);

  async function signOut() {
    setConfirming(false);
    setSigningOut(true);
    setFailure(null);

    try {
      /** On success the session gate replaces this screen with the sign-in screen. */
      await store.signOut();
    } catch (error) {
      setFailure(classifyApiError(error).title);
      setSigningOut(false);
    }
  }

  return (
    <View style={styles.screen}>
      <ScreenHeading>Hồ sơ</ScreenHeading>

      <View
        style={[
          styles.card,
          { backgroundColor: theme.colors.surface, borderColor: theme.colors.outlineVariant },
        ]}
        testID="profile-identity"
      >
        <Text variant="titleMedium">{session.user?.displayName}</Text>
        <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyLarge">
          {session.user?.email}
        </Text>
        <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyMedium">
          Đăng nhập bằng Google Account. Rikkaus không tạo và không lưu mật khẩu.
        </Text>
      </View>

      {failure === null ? null : (
        <ErrorState
          message={`Chưa đăng xuất được. ${failure}.`}
          onRetry={() => void signOut()}
          preserved="Phiên trên thiết bị này vẫn còn hiệu lực. Kiểm tra kết nối mạng rồi thử lại."
          retryLabel="Thử lại"
          testID="profile-sign-out-error"
          variant="offline"
        />
      )}

      <ActionButton
        icon="logout"
        loading={signingOut}
        onPress={() => setConfirming(true)}
        style={styles.signOut}
        testID="profile-sign-out"
        variant="secondary"
      >
        Đăng xuất
      </ActionButton>

      <ConfirmDialog
        body="Quyền truy cập dữ liệu trên thiết bị này sẽ kết thúc. Việc đăng xuất không xóa Google Account."
        cancelLabel="Tiếp tục sử dụng"
        confirmLabel="Đăng xuất"
        onCancel={() => setConfirming(false)}
        onConfirm={() => void signOut()}
        testID="sign-out-dialog"
        title="Đăng xuất khỏi Rikkaus?"
        visible={confirming}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    gap: spacing.md,
    maxWidth: sizing.contentMaxWidth,
  },
  card: {
    borderRadius: radius.card,
    borderWidth: 1,
    gap: spacing.xs,
    padding: spacing.md,
  },
  signOut: {
    alignSelf: 'flex-start',
  },
});
