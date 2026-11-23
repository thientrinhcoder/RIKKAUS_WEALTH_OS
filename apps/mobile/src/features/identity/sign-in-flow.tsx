import { useRouter } from 'expo-router';
import { useRef, useState, type ReactNode } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Icon, Text } from 'react-native-paper';
import { z } from 'zod';

import { classifyApiError, type ApiClient } from '@/api';
import { ActionButton } from '@/components/action';
import { ErrorState, StateView, type StateTone } from '@/components/feedback';
import { TextField } from '@/components/form';
import { ScreenHeading } from '@/components/shell/screen-heading';
import { SERIF_FAMILY } from '@/ui/fonts';
import { radius, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { DEV_SIGN_IN_DEFAULTS, DEV_SIGN_IN_ENABLED } from './dev-sign-in';
import { useGoogleSignIn } from './google-sign-in';
import { useSessionStore } from './session-provider';
import type { SessionEndReason } from './session-store';

/**
 * Every app-owned state of the accepted identity design (docs/identity-session-ownership-review.html):
 * the Google entry point, processing, first and returning success, cancel, deny, callback failure,
 * and the expired and revoked screens that replace protected content.
 */
type Phase =
  | 'entry'
  | 'processing'
  | 'first'
  | 'returning'
  | 'cancelled'
  | 'denied'
  | 'failed'
  | 'offline'
  | SessionEndReason;

type Attempt = 'google' | 'dev';

export interface SignInFlowProps {
  /** `expired` or `revoked` when a session ended under the user; `entry` otherwise. */
  initialPhase?: 'entry' | SessionEndReason;
  /** Leaves an ended-session screen; the entry screen handles this itself. */
  onBackToStart?: () => void;
  /** Told when a sign-in starts, so the sign-in route stays to show its result. */
  onAttemptStart?: () => void;
}

/** The contract does not mark it nullable, but the API returns `null` until a rate is declared. */
const ExchangeRatePresenceSchema = z.object({
  usdToVndRate: z
    .number()
    .nullish()
    .transform((value) => value ?? undefined),
});

/**
 * The session response does not say whether the account was just created. The first-login panel
 * exists to lead into exchange-rate setup, so an account with no declared rate is treated as new.
 */
async function hasDeclaredExchangeRate(client: ApiClient): Promise<boolean> {
  try {
    const { data } = await client.request('get', '/api/v1/settings/exchange-rate', {
      schema: ExchangeRatePresenceSchema,
    });

    return data.usdToVndRate !== undefined;
  } catch {
    return true;
  }
}

interface PanelCopy {
  tone: StateTone;
  icon: string;
  title: string;
  body: string;
}

const PANELS: Record<Exclude<Phase, 'entry' | 'processing' | 'offline'>, PanelCopy> = {
  first: {
    tone: 'success',
    icon: 'check-circle-outline',
    title: 'Tài khoản Rikkaus đã sẵn sàng',
    body: 'Danh tính Google đã được xác nhận. Tiếp theo, thiết lập tỷ giá để bắt đầu nhập tài sản.',
  },
  returning: {
    tone: 'success',
    icon: 'check-circle-outline',
    title: 'Đã đăng nhập',
    body: 'Phiên Rikkaus đã được khôi phục. Bạn có thể tiếp tục từ điểm an toàn gần nhất.',
  },
  cancelled: {
    tone: 'info',
    icon: 'information-outline',
    title: 'Bạn đã dừng đăng nhập',
    body: 'Chưa có phiên Rikkaus nào được tạo. Bạn có thể thử lại khi sẵn sàng.',
  },
  denied: {
    tone: 'warning',
    icon: 'alert-outline',
    title: 'Chưa thể tiếp tục',
    body: 'Google chưa cho phép Rikkaus nhận thông tin nhận diện cần thiết. Chưa có tài khoản hoặc phiên nào được tạo.',
  },
  failed: {
    tone: 'error',
    icon: 'close-circle-outline',
    title: 'Không hoàn tất được đăng nhập',
    body: 'Kết quả trả về chưa được xử lý. Không có thay đổi nào được lưu.',
  },
  expired: {
    tone: 'warning',
    icon: 'clock-alert-outline',
    title: 'Phiên đăng nhập đã hết hạn',
    body: 'Để bảo vệ dữ liệu tài chính, nội dung trước đó đã được ẩn. Tiếp tục với Google để đăng nhập lại.',
  },
  revoked: {
    tone: 'error',
    icon: 'shield-alert-outline',
    title: 'Phiên đăng nhập đã kết thúc',
    body: 'Vì lý do bảo mật, hãy đăng nhập lại. Sau khi thành công, Rikkaus sẽ mở Tổng quan thay vì quay lại liên kết nhạy cảm.',
  },
};

export function SignInFlow({
  initialPhase = 'entry',
  onBackToStart,
  onAttemptStart,
}: SignInFlowProps) {
  const theme = useAppTheme();
  const router = useRouter();
  const store = useSessionStore();
  const google = useGoogleSignIn();
  const [phase, setPhase] = useState<Phase>(initialPhase);
  const [devEmail, setDevEmail] = useState<string>(DEV_SIGN_IN_DEFAULTS.email);
  const [devName, setDevName] = useState<string>(DEV_SIGN_IN_DEFAULTS.displayName);
  const lastAttempt = useRef<Attempt>('google');
  /** Bumped by "Hủy và quay lại" so a late answer from an abandoned attempt is ignored. */
  const attemptId = useRef(0);
  const reauthenticating = initialPhase !== 'entry';

  function backToStart() {
    if (reauthenticating && onBackToStart) onBackToStart();
    else setPhase('entry');
  }

  async function finish(signIn: () => Promise<void>, id: number) {
    /**
     * After an ended session the app opens Tổng quan, never the deep link it was on. Moving there
     * first, while this screen still covers the content, means the old route never re-renders.
     */
    if (reauthenticating) router.replace('/');

    await signIn();

    if (id !== attemptId.current || reauthenticating) return;

    setPhase((await hasDeclaredExchangeRate(store.apiClient)) ? 'returning' : 'first');
  }

  async function run(attempt: Attempt, work: (id: number) => Promise<void>) {
    const id = ++attemptId.current;
    lastAttempt.current = attempt;
    onAttemptStart?.();
    setPhase('processing');

    try {
      await work(id);
    } catch (error) {
      if (id !== attemptId.current) return;

      setPhase(classifyApiError(error).kind === 'network' ? 'offline' : 'failed');
    }
  }

  function startGoogle() {
    void run('google', async (id) => {
      const outcome = await google.prompt();

      if (id !== attemptId.current) return;

      if (outcome.kind === 'code') {
        await finish(() => store.signInWithGoogle(outcome.input), id);
      } else {
        setPhase(outcome.kind);
      }
    });
  }

  function startDev() {
    void run('dev', (id) =>
      finish(
        () =>
          store.devSignIn({
            email: devEmail.trim(),
            ...(devName.trim() ? { displayName: devName.trim() } : {}),
          }),
        id,
      ),
    );
  }

  function retry() {
    if (lastAttempt.current === 'dev') startDev();
    else startGoogle();
  }

  const googleButton = (label: string) => (
    <ActionButton
      disabled={!google.ready}
      icon="google"
      onPress={startGoogle}
      testID="sign-in-google"
      variant="primary"
    >
      {label}
    </ActionButton>
  );

  const backButton = (label = 'Về màn hình đầu') => (
    <ActionButton onPress={backToStart} testID="sign-in-back" variant="text">
      {label}
    </ActionButton>
  );

  let body: ReactNode;

  switch (phase) {
    case 'entry':
      body = (
        <EntryPanel
          googleButton={googleButton('Tiếp tục với Google')}
          googleConfigured={google.configured}
        />
      );
      break;
    case 'processing':
      body = (
        <StateView
          action={
            <ActionButton
              onPress={() => {
                attemptId.current += 1;
                setPhase(initialPhase);
              }}
              testID="sign-in-cancel"
              variant="text"
            >
              Hủy và quay lại
            </ActionButton>
          }
          busy
          detail={
            <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyLarge">
              Rikkaus đang xử lý kết quả trả về từ Google. Không đóng cửa sổ này.
            </Text>
          }
          icon="progress-clock"
          message="Đang hoàn tất đăng nhập"
          testID="sign-in-processing"
          tone="info"
        />
      );
      break;
    case 'offline':
      body = (
        <ErrorState
          message="Không kết nối được máy chủ"
          onRetry={retry}
          onSafeReturn={backToStart}
          preserved="Chưa có phiên Rikkaus nào được tạo. Kiểm tra kết nối mạng rồi thử lại."
          retryLabel="Thử lại"
          safeReturnLabel="Về màn hình đầu"
          testID="sign-in-offline"
          variant="offline"
        />
      );
      break;
    default: {
      const actions: Record<typeof phase, ReactNode> = {
        first: (
          <ActionButton
            onPress={() => router.replace('/settings/exchange-rate' as never)}
            testID="sign-in-continue"
            variant="primary"
          >
            Thiết lập tỷ giá USD/VND
          </ActionButton>
        ),
        returning: (
          <ActionButton
            onPress={() => router.replace('/')}
            testID="sign-in-continue"
            variant="primary"
          >
            Mở Tổng quan
          </ActionButton>
        ),
        cancelled: (
          <>
            {googleButton('Thử lại với Google')}
            {backButton()}
          </>
        ),
        denied: (
          <>
            {googleButton('Xem lại với Google')}
            {backButton('Quay lại')}
          </>
        ),
        failed: (
          <>
            <ActionButton onPress={retry} testID="sign-in-retry" variant="primary">
              Thử lại
            </ActionButton>
            {backButton()}
          </>
        ),
        expired: (
          <>
            {googleButton('Tiếp tục với Google')}
            {backButton()}
          </>
        ),
        revoked: (
          <>
            {googleButton('Đăng nhập lại')}
            {backButton()}
          </>
        ),
      };
      body = <ResultPanel action={actions[phase]} copy={PANELS[phase]} phase={phase} />;
    }
  }

  const showDevSection =
    DEV_SIGN_IN_ENABLED && phase !== 'processing' && phase !== 'first' && phase !== 'returning';

  return (
    <ScrollView
      contentContainerStyle={styles.scroll}
      style={{ backgroundColor: theme.colors.background }}
      testID="sign-in-screen"
    >
      <View style={styles.column}>
        <BrandHeader />

        <View
          style={[
            styles.card,
            { backgroundColor: theme.colors.surface, borderColor: theme.colors.outlineVariant },
          ]}
        >
          {body}
        </View>

        {showDevSection ? (
          <View
            style={[styles.card, styles.devCard, { borderColor: theme.colors.outline }]}
            testID="dev-sign-in"
          >
            <Text variant="titleMedium">Đăng nhập phát triển</Text>
            <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyMedium">
              Chỉ có trong bản dev khi API chạy profile local. Bản phát hành không có mục này.
            </Text>
            <TextField
              autoCapitalize="none"
              keyboardType="email-address"
              label="Email"
              onChangeValue={setDevEmail}
              testID="dev-sign-in-email"
              value={devEmail}
            />
            <TextField
              label="Tên hiển thị"
              onChangeValue={setDevName}
              testID="dev-sign-in-name"
              value={devName}
            />
            <ActionButton
              disabled={!devEmail.trim()}
              onPress={startDev}
              testID="dev-sign-in-submit"
              variant="secondary"
            >
              Đăng nhập phát triển
            </ActionButton>
          </View>
        ) : null}
      </View>
    </ScrollView>
  );
}

function BrandHeader() {
  const theme = useAppTheme();

  return (
    <View style={styles.brand}>
      <Text
        accessibilityRole="header"
        style={[styles.wordmark, { color: theme.colors.brandNavy }]}
        testID="sign-in-wordmark"
      >
        Rikkaus
      </Text>
      {/** Gold is allowed here only as a decorative rule (design guidelines section 4). */}
      <View style={[styles.rule, { backgroundColor: theme.colors.heritageGold }]} />
      <View style={styles.badge}>
        <Icon color={theme.colors.onSurfaceVariant} size={16} source="lock-outline" />
        <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyMedium">
          Không lưu thông tin đăng nhập Google
        </Text>
      </View>
    </View>
  );
}

function EntryPanel({
  googleButton,
  googleConfigured,
}: {
  googleButton: ReactNode;
  googleConfigured: boolean;
}) {
  const theme = useAppTheme();
  const points = [
    'Không tạo mật khẩu riêng tại Rikkaus',
    'Chỉ yêu cầu thông tin nhận diện tối thiểu',
    'Mỗi tài khoản chỉ truy cập dữ liệu của chính mình',
  ];

  return (
    <View style={styles.panel} testID="sign-in-entry">
      <Text accessibilityRole="header" style={[styles.headline, { color: theme.colors.onSurface }]}>
        Tài sản của bạn. Một góc nhìn riêng tư.
      </Text>
      <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyLarge">
        Tiếp tục bằng Google Account để mở không gian tài chính cá nhân.
      </Text>

      <View style={styles.points}>
        {points.map((point) => (
          <View key={point} style={styles.point}>
            <Icon color={theme.colors.primary} size={20} source="check-circle-outline" />
            <Text style={styles.pointText} variant="bodyLarge">
              {point}
            </Text>
          </View>
        ))}
      </View>

      <View style={[styles.note, { backgroundColor: theme.colors.surfaceVariant }]}>
        <Text variant="titleSmall">Bắt đầu an toàn</Text>
        <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyMedium">
          Lần đầu tiếp tục sẽ tạo tài khoản Rikkaus từ danh tính được Google xác nhận.
        </Text>
      </View>

      {googleButton}

      {googleConfigured ? null : (
        <Text
          style={{ color: theme.colors.onSurfaceVariant }}
          testID="sign-in-google-unconfigured"
          variant="bodyMedium"
        >
          Đăng nhập Google chưa được cấu hình cho bản build này.
        </Text>
      )}

      <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyMedium">
        Bước chọn tài khoản và xác thực diễn ra trên trang của Google, bên ngoài Rikkaus.
      </Text>
    </View>
  );
}

function ResultPanel({
  phase,
  copy,
  action,
}: {
  phase: Phase;
  copy: PanelCopy;
  action: ReactNode;
}) {
  const theme = useAppTheme();
  const toneColor = {
    neutral: theme.colors.onSurfaceVariant,
    success: theme.colors.success,
    warning: theme.colors.warning,
    error: theme.colors.error,
    info: theme.colors.info,
  }[copy.tone];

  return (
    <View
      accessibilityLiveRegion={copy.tone === 'error' ? 'assertive' : 'polite'}
      style={styles.panel}
      testID={`sign-in-${phase}`}
    >
      <Icon color={toneColor} size={32} source={copy.icon} />
      <ScreenHeading>{copy.title}</ScreenHeading>
      <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyLarge">
        {copy.body}
      </Text>
      <View style={styles.actions}>{action}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  scroll: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: spacing.md,
  },
  column: {
    alignSelf: 'center',
    gap: spacing.md,
    maxWidth: 480,
    paddingVertical: spacing.lg,
    width: '100%',
  },
  brand: {
    gap: spacing.sm,
  },
  wordmark: {
    fontFamily: SERIF_FAMILY,
    fontSize: 32,
    lineHeight: 40,
  },
  rule: {
    height: 2,
    width: 48,
  },
  badge: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: spacing.xs,
  },
  card: {
    borderRadius: radius.card,
    borderWidth: 1,
    padding: spacing.lg,
  },
  devCard: {
    gap: spacing.sm,
  },
  panel: {
    gap: spacing.md,
  },
  headline: {
    fontFamily: SERIF_FAMILY,
    fontSize: 28,
    lineHeight: 36,
  },
  points: {
    gap: spacing.sm,
  },
  point: {
    alignItems: 'flex-start',
    flexDirection: 'row',
    gap: spacing.sm,
  },
  pointText: {
    flex: 1,
  },
  note: {
    borderRadius: radius.input,
    gap: spacing.xs,
    padding: spacing.md,
  },
  actions: {
    gap: spacing.sm,
  },
});
