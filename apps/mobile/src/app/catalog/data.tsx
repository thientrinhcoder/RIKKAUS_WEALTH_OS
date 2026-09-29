import { useMemo, useState } from 'react';
import { View } from 'react-native';

import {
  FilterBar,
  FilterSheet,
  KpiCard,
  ListView,
  RecordRow,
  SegmentedControl,
  Tabs,
  type AppliedFilter,
} from '@/components/data';
import { SelectField } from '@/components/form';
import { BottomSheet } from '@/components/overlay';
import { ActionButton } from '@/components/action';
import { ScreenHeading } from '@/components/shell/screen-heading';
import { spacing } from '@/ui/tokens';
import {
  ASSETS,
  LIABILITIES,
  NET_WORTH,
  TOTAL_ASSETS,
  TOTAL_LIABILITIES,
  formatVnd,
} from '@/catalog/fixture';

interface Filters {
  category: string;
}

const TAB_ITEMS = [
  { value: 'assets', label: 'Tài sản' },
  { value: 'liabilities', label: 'Công nợ' },
];

const PERIODS = [
  { value: 'all', label: 'Tất cả' },
  { value: 'recent', label: 'Cập nhật gần đây' },
];

/**
 * A catalog and discovery screen built only from the data kit.
 *
 * The filter sheet receives the shared bottom sheet as its container, closing the seam the data
 * slice left open so it could ship before the overlay slice existed.
 */
export default function DataCatalog() {
  const [tab, setTab] = useState('assets');
  const [period, setPeriod] = useState('all');
  const [query, setQuery] = useState('');
  const [filters, setFilters] = useState<Filters>({ category: '' });
  const [sheetOpen, setSheetOpen] = useState(false);

  const source = tab === 'assets' ? ASSETS : LIABILITIES;

  const visible = useMemo(
    () =>
      source.filter((record) => {
        const matchesQuery =
          query === '' || record.title.toLowerCase().includes(query.toLowerCase());
        const matchesCategory = filters.category === '' || record.category === filters.category;
        const matchesPeriod = period === 'all' || record.date.includes('12/09/2026');

        return matchesQuery && matchesCategory && matchesPeriod;
      }),
    [filters.category, period, query, source],
  );

  const appliedFilters: AppliedFilter[] =
    filters.category === '' ? [] : [{ key: 'category', label: filters.category }];

  const categories = [...new Set(source.map((record) => record.category))].map((category) => ({
    value: category,
    label: category,
  }));

  return (
    <View style={{ gap: spacing.lg }} testID="catalog-data">
      <ScreenHeading>Hiển thị dữ liệu và tìm kiếm</ScreenHeading>

      <View style={{ gap: spacing.sm }}>
        <KpiCard
          caption="Tổng tài sản trừ tổng công nợ"
          label="Giá trị ròng"
          testID="catalog-kpi-net-worth"
          value={formatVnd(NET_WORTH)}
        />
        <KpiCard label="Tổng tài sản" testID="catalog-kpi-assets" value={formatVnd(TOTAL_ASSETS)} />
        <KpiCard
          label="Tổng công nợ"
          testID="catalog-kpi-liabilities"
          value={formatVnd(TOTAL_LIABILITIES)}
        />
        <KpiCard
          caption="Chưa đủ dữ liệu để tính"
          label="Tỷ lệ nợ trên tài sản"
          testID="catalog-kpi-unavailable"
        />
      </View>

      <Tabs
        accessibilityLabel="Loại bản ghi"
        items={TAB_ITEMS}
        onChange={setTab}
        testID="catalog-tabs"
        value={tab}
      />

      <SegmentedControl
        accessibilityLabel="Phạm vi hiển thị"
        items={PERIODS}
        onChange={setPeriod}
        testID="catalog-period"
        value={period}
      />

      <FilterBar
        appliedFilters={appliedFilters}
        matchCount={visible.length}
        onClearFilters={() => setFilters({ category: '' })}
        onQueryChange={setQuery}
        onRemoveFilter={() => setFilters({ category: '' })}
        query={query}
        searchLabel="Tìm bản ghi"
        testID="catalog-filter-bar"
      />

      <ActionButton onPress={() => setSheetOpen(true)} testID="catalog-open-filters" variant="secondary">
        Mở bộ lọc
      </ActionButton>

      <ListView
        emptyActionLabel="Xoá từ khoá và bộ lọc"
        emptyDescription="Không có bản ghi nào khớp với từ khoá và bộ lọc hiện tại."
        emptyTitle="Không tìm thấy bản ghi"
        items={visible}
        keyExtractor={(record) => record.id}
        onEmptyAction={() => {
          setQuery('');
          setFilters({ category: '' });
        }}
        renderItem={(record) => (
          <RecordRow
            category={record.category}
            date={record.date}
            onPress={() => {}}
            primaryValue={formatVnd(record.amount)}
            secondaryValue={record.originalAmount}
            testID={`catalog-row-${record.id}`}
            title={record.title}
          />
        )}
        testID="catalog-list"
      />

      <FilterSheet<Filters>
        applied={filters}
        container={BottomSheet}
        onApply={(next) => {
          setFilters(next);
          setSheetOpen(false);
        }}
        onDismiss={() => setSheetOpen(false)}
        testID="catalog-filter-sheet"
        visible={sheetOpen}
      >
        {(staged, setStaged) => (
          <SelectField
            label="Nhóm"
            onChangeValue={(category) => setStaged({ ...staged, category })}
            options={categories}
            testID="catalog-filter-category"
            value={staged.category}
          />
        )}
      </FilterSheet>
    </View>
  );
}
