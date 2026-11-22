import { useQuery } from '@tanstack/react-query';
import { Platform, View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { CardShell } from '@/components/data';
import { ErrorState, LoadingState } from '@/components/feedback';
import {
  ConvertedMoney,
  DateText,
  ExchangeRateText,
  FreshnessBadge,
  TimestampText,
} from '@/components/value';
import {
  convertToReporting,
  fromApiValue,
  isCurrencyCode,
  isPrecisionAtRisk,
  readExchangeRateSettings,
  type ExchangeRateSettings,
  type ExchangeRateSettingsPayload,
} from '@/money';
import { parseIsoDate } from '@/time';
import { spacing } from '@/ui/tokens';

/**
 * Renders the real exchange-rate settings and asset list through the primitives, so the catalog
 * proves them against the API's actual payloads rather than hand-written ones.
 *
 * Verification only. The typed API client is issue #38's and identity is issue #41's; until both
 * land, this panel does a bare `fetch` with a bearer token a developer places in the web session
 * (`sessionStorage['rikkaus.catalog.accessToken']`). Nothing here is reused by product screens.
 */
const TOKEN_KEY = 'rikkaus.catalog.accessToken';

interface AssetPayload {
  id: unknown;
  name: unknown;
  currentValue: unknown;
  currency: unknown;
  valuationDate: unknown;
}

interface LiveData {
  settings: ExchangeRateSettings;
  assets: AssetPayload[];
}

function readToken(): string | null {
  if (Platform.OS !== 'web' || typeof sessionStorage === 'undefined') {
    return null;
  }

  return sessionStorage.getItem(TOKEN_KEY);
}

async function getJson(baseUrl: string, path: string, token: string): Promise<unknown> {
  const response = await fetch(`${baseUrl}${path}`, {
    headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
  });

  if (!response.ok) {
    throw new Error(`${path} trả về HTTP ${response.status}`);
  }

  return response.json();
}

/** Freshness threshold for this verification panel only; the product has not fixed one. */
const PANEL_MAX_RATE_AGE_DAYS = 30;

async function loadLiveData(baseUrl: string, token: string): Promise<LiveData> {
  const [settings, assets] = await Promise.all([
    getJson(baseUrl, '/api/v1/settings/exchange-rate', token),
    getJson(baseUrl, '/api/v1/assets', token),
  ]);

  return {
    settings: readExchangeRateSettings(settings as ExchangeRateSettingsPayload),
    assets: Array.isArray(assets) ? (assets as AssetPayload[]) : [],
  };
}

export function LiveMoneyPanel() {
  const baseUrl = process.env.EXPO_PUBLIC_API_BASE_URL?.replace(/\/+$/, '') ?? '';
  const token = readToken();
  const enabled = token !== null && baseUrl !== '';

  const query = useQuery({
    queryKey: ['catalog', 'live-money', baseUrl],
    queryFn: () => loadLiveData(baseUrl, token ?? ''),
    enabled,
    retry: false,
  });

  if (!enabled) {
    return (
      <Text testID="live-no-token" variant="bodyMedium">
        Chưa có access token trong phiên web nên không gọi API thật. Phần này chỉ dùng khi kiểm thử
        cục bộ.
      </Text>
    );
  }

  if (query.isPending) {
    return <LoadingState accessibilityLabel="Đang tải tỷ giá và tài sản" reservedHeight={160} />;
  }

  if (query.isError) {
    return (
      <ErrorState
        message={`Không tải được dữ liệu thật: ${query.error.message}`}
        onRetry={() => void query.refetch()}
        preserved="Không có thay đổi nào bị mất."
        retryLabel="Thử lại"
        testID="live-error"
        variant="request"
      />
    );
  }

  const { settings, assets } = query.data;

  return (
    <View style={{ gap: spacing.md }} testID="live-ready">
      <CardShell testID="live-rate">
        <ExchangeRateText rate={settings.rate} testID="live-rate-text" />
        <FreshnessBadge
          asOf={settings.rate?.effectiveDate ?? null}
          datePrefix="hiệu lực"
          maxAgeDays={PANEL_MAX_RATE_AGE_DAYS}
          testID="live-rate-freshness"
        />
        <TimestampText
          instant={settings.updatedAt}
          prefix="Cập nhật lúc"
          showRelative
          showSource
          testID="live-rate-updated"
        />
      </CardShell>

      {assets.map((asset, index) => {
        const amount = fromApiValue(asset.currentValue);
        const currency = isCurrencyCode(asset.currency) ? asset.currency : null;
        const id = typeof asset.id === 'string' ? asset.id : String(index);

        return (
          <CardShell key={id} testID={`live-asset-${index}`}>
            <Text variant="titleMedium">{String(asset.name)}</Text>
            {amount === null || currency === null ? (
              <Text variant="bodyMedium">Giá trị hoặc tiền tệ không đọc được.</Text>
            ) : (
              <ConvertedMoney
                conversion={convertToReporting(amount, currency, settings.rate)}
                testID={`live-asset-${index}-value`}
              />
            )}
            <DateText
              date={parseIsoDate(asset.valuationDate)}
              prefix="Ngày định giá"
              testID={`live-asset-${index}-date`}
            />
            <Text testID={`live-asset-${index}-raw`} variant="bodyMedium">
              {`JSON: ${JSON.stringify(asset.currentValue)} ${String(asset.currency)}${
                isPrecisionAtRisk(asset.currentValue) ? ' · có thể đã mất độ chính xác' : ''
              }`}
            </Text>
          </CardShell>
        );
      })}

      <ActionButton onPress={() => void query.refetch()} testID="live-reload" variant="text">
        Tải lại
      </ActionButton>
    </View>
  );
}
