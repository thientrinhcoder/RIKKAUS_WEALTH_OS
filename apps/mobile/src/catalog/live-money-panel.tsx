import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { View } from "react-native";
import { Text } from "react-native-paper";
import { z } from "zod";

import {
  classifyApiError,
  createApiClient,
  isApiBaseUrlConfigured,
  readApiBaseUrlFromEnv,
  resolveApiBaseUrl,
  type ApiClient,
} from "@/api";
import { ActionButton } from "@/components/action";
import { CardShell } from "@/components/data";
import { ErrorState, LoadingState } from "@/components/feedback";
import {
  ConvertedMoney,
  DateText,
  ExchangeRateText,
  FreshnessBadge,
  TimestampText,
} from "@/components/value";
import {
  convertToReporting,
  fromApiValue,
  isCurrencyCode,
  isPrecisionAtRisk,
  readExchangeRateSettings,
  type ExchangeRateSettings,
  type ExchangeRateSettingsPayload,
} from "@/money";
import { parseIsoDate } from "@/time";
import { spacing } from "@/ui/tokens";

/**
 * Renders the real exchange-rate settings and asset list through the primitives, so the catalog
 * proves them against the API's actual payloads rather than hand-written ones.
 *
 * Verification only, for a developer running the API under the `local` profile. The session comes
 * from the local-only `POST /api/v1/auth/dev-sign-in` and the access token is held in component
 * state for this page view only: nothing is written to browser storage, and the refresh token is
 * discarded. Requests go through the typed API client. Product screens get identity from #41.
 */
const DEV_USER = { email: "an.nguyen@example.com", displayName: "An Nguyễn" };

const DevSessionSchema = z.object({ accessToken: z.string().min(1) });

/** The contract does not mark these as nullable, but the API returns `null` until a rate is declared. */
const optionalNullable = <T extends z.ZodType>(schema: T) =>
  schema.nullish().transform((value) => value ?? undefined);

const ExchangeRateSchema = z.object({
  baseCurrency: optionalNullable(z.string()),
  usdToVndRate: optionalNullable(z.number()),
  effectiveDate: optionalNullable(z.string()),
  updatedAt: optionalNullable(z.string()),
});

const AssetListSchema = z.array(
  z.object({
    id: z.string(),
    name: z.string(),
    currentValue: z.number(),
    currency: z.enum(["VND", "USD"]),
    valuationDate: optionalNullable(z.string()),
  }),
);

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

/** Freshness threshold for this verification panel only; the product has not fixed one. */
const PANEL_MAX_RATE_AGE_DAYS = 30;

/** `dev-sign-in` is deliberately outside the published contract, so the typed client cannot call it. */
async function devSignIn(): Promise<string> {
  const response = await fetch(
    `${resolveApiBaseUrl(readApiBaseUrlFromEnv())}/api/v1/auth/dev-sign-in`,
    {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(DEV_USER),
    },
  );

  if (!response.ok) {
    throw new Error(
      `dev-sign-in trả về HTTP ${response.status} (API có chạy profile local không?)`,
    );
  }

  return DevSessionSchema.parse(await response.json()).accessToken;
}

async function loadLiveData(client: ApiClient): Promise<LiveData> {
  const [settings, assets] = await Promise.all([
    client.request("get", "/api/v1/settings/exchange-rate", {
      schema: ExchangeRateSchema,
    }),
    client.request("get", "/api/v1/assets", { schema: AssetListSchema }),
  ]);

  return {
    settings: readExchangeRateSettings({
      baseCurrency: settings.data.baseCurrency ?? null,
      usdToVndRate: settings.data.usdToVndRate ?? null,
      effectiveDate: settings.data.effectiveDate ?? null,
      updatedAt: settings.data.updatedAt ?? null,
    } satisfies ExchangeRateSettingsPayload),
    assets: assets.data,
  };
}

export function LiveMoneyPanel() {
  const [token, setToken] = useState<string | null>(null);
  const [signInError, setSignInError] = useState<string | null>(null);
  const configured = isApiBaseUrlConfigured(readApiBaseUrlFromEnv());

  const client = useMemo(
    () =>
      createApiClient({
        baseUrl: readApiBaseUrlFromEnv,
        auth: { getAccessToken: () => token },
      }),
    [token],
  );

  const query = useQuery({
    queryKey: ["catalog", "live-money", token],
    queryFn: () => loadLiveData(client),
    enabled: token !== null,
    retry: false,
  });

  if (!configured) {
    return (
      <Text testID="live-no-api" variant="bodyMedium">
        Chưa cấu hình EXPO_PUBLIC_API_BASE_URL nên không gọi API thật. Phần này
        chỉ dùng khi kiểm thử cục bộ.
      </Text>
    );
  }

  if (token === null) {
    return (
      <View style={{ gap: spacing.sm }}>
        <Text testID="live-no-token" variant="bodyMedium">
          Đăng nhập phiên dev ({DEV_USER.email}) để đọc tỷ giá và tài sản thật.
          Chỉ hoạt động khi API chạy profile local.
        </Text>
        {signInError ? <Text variant="bodyMedium">{signInError}</Text> : null}
        <ActionButton
          onPress={() => {
            setSignInError(null);
            devSignIn().then(setToken, (error: unknown) =>
              setSignInError(String(error)),
            );
          }}
          testID="live-dev-sign-in"
          variant="secondary"
        >
          Đăng nhập phiên dev
        </ActionButton>
      </View>
    );
  }

  if (query.isPending) {
    return (
      <LoadingState
        accessibilityLabel="Đang tải tỷ giá và tài sản"
        reservedHeight={160}
      />
    );
  }

  if (query.isError) {
    return (
      <ErrorState
        message={`Không tải được dữ liệu thật: ${classifyApiError(query.error).message}`}
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
        const id = typeof asset.id === "string" ? asset.id : String(index);

        return (
          <CardShell key={id} testID={`live-asset-${index}`}>
            <Text variant="titleMedium">{String(asset.name)}</Text>
            {amount === null || currency === null ? (
              <Text variant="bodyMedium">
                Giá trị hoặc tiền tệ không đọc được.
              </Text>
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
                isPrecisionAtRisk(asset.currentValue)
                  ? " · có thể đã mất độ chính xác"
                  : ""
              }`}
            </Text>
          </CardShell>
        );
      })}

      <ActionButton
        onPress={() => void query.refetch()}
        testID="live-reload"
        variant="text"
      >
        Tải lại
      </ActionButton>
    </View>
  );
}
