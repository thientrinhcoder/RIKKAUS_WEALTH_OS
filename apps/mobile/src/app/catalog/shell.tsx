import { View } from 'react-native';

import { KpiCard, RecordRow } from '@/components/data';
import { AppShell } from '@/components/shell';
import { ScreenHeading } from '@/components/shell/screen-heading';
import { useNavigationMode } from '@/ui/navigation-mode';
import { spacing } from '@/ui/tokens';
import { ASSETS, NET_WORTH, formatVnd } from '@/catalog/fixture';

/**
 * The shell rendering real content at whatever width the viewport is, so the navigation chrome
 * can be reviewed against content rather than against an empty frame.
 */
export default function ShellCatalog() {
  const mode = useNavigationMode();

  return (
    <View style={{ gap: spacing.md }} testID="catalog-shell">
      <ScreenHeading>Khung ứng dụng</ScreenHeading>

      <KpiCard
        caption={`Chế độ điều hướng hiện tại: ${mode}`}
        label="Giá trị ròng"
        testID="catalog-shell-kpi"
        value={formatVnd(NET_WORTH)}
      />

      <View style={{ minHeight: 320 }} testID="catalog-shell-frame">
        <AppShell activeRoute="/">
          <View style={{ gap: spacing.sm }}>
            {ASSETS.slice(0, 3).map((asset) => (
              <RecordRow
                category={asset.category}
                date={asset.date}
                key={asset.id}
                primaryValue={formatVnd(asset.amount)}
                testID={`catalog-shell-row-${asset.id}`}
                title={asset.title}
              />
            ))}
          </View>
        </AppShell>
      </View>
    </View>
  );
}
