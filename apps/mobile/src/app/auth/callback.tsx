import { useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { StateView } from '@/components/feedback';
import { handOffGoogleCallback } from '@/features/identity/google-popup';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * Google redirects the sign-in window here (`/auth/callback`). On web that window is a popup the
 * sign-in screen opened: this hands the redirect URL back to it and closes. Nothing is redeemed,
 * displayed or stored here.
 */
export default function AuthCallbackScreen() {
  const theme = useAppTheme();
  const router = useRouter();
  /** Opened directly rather than by a waiting sign-in screen: there is nothing to hand back. */
  const [orphaned, setOrphaned] = useState(false);

  useEffect(() => {
    let active = true;

    void handOffGoogleCallback().then((taken) => {
      if (active && !taken) setOrphaned(true);
    });

    return () => {
      active = false;
    };
  }, []);

  return (
    <View style={[styles.frame, { backgroundColor: theme.colors.background }]}>
      <View style={styles.column}>
        {orphaned ? (
          <StateView
            action={
              <ActionButton
                onPress={() => router.replace('/sign-in')}
                testID="auth-callback-back"
                variant="primary"
              >
                Về màn hình đầu
              </ActionButton>
            }
            detail={
              <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyLarge">
                Kết quả trả về chưa được xử lý. Không có thay đổi nào được lưu.
              </Text>
            }
            icon="close-circle-outline"
            live="assertive"
            message="Không hoàn tất được đăng nhập"
            testID="auth-callback-orphaned"
            tone="error"
          />
        ) : (
          <StateView
            busy
            detail={
              <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyLarge">
                Rikkaus đang xử lý kết quả trả về từ Google. Không đóng cửa sổ này.
              </Text>
            }
            icon="progress-clock"
            message="Đang hoàn tất đăng nhập"
            testID="auth-callback-processing"
            tone="info"
          />
        )}
      </View>
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
