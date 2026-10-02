import { StyleSheet, View } from 'react-native';
import { Chip, Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { SearchField } from '@/components/form/fields/search-field';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

export interface AppliedFilter {
  key: string;
  label: string;
}

export interface FilterBarProps {
  /** Controlled: this component holds no query or filter state of its own, per section 8.1. */
  query: string;
  onQueryChange: (query: string) => void;
  searchLabel: string;
  appliedFilters: readonly AppliedFilter[];
  onRemoveFilter: (key: string) => void;
  onClearFilters: () => void;
  /** Rendered so the user can see how many records the current query and filters match. */
  matchCount?: number;
  testID?: string;
}

/**
 * The shared discovery bar: the Phase 4 search field plus the applied-filter state from section
 * 8.1. Filters are additive, each individually removable, with one control that clears all.
 *
 * The search input itself is not redefined here; a second search field would be a second set of
 * behaviours to keep consistent.
 */
export function FilterBar({
  query,
  onQueryChange,
  searchLabel,
  appliedFilters,
  onRemoveFilter,
  onClearFilters,
  matchCount,
  testID = 'filter-bar',
}: FilterBarProps) {
  const theme = useAppTheme();

  return (
    <View style={styles.bar} testID={testID}>
      <SearchField
        label={searchLabel}
        onChangeValue={onQueryChange}
        testID={`${testID}-search`}
        value={query}
      />

      {appliedFilters.length === 0 ? null : (
        <View style={styles.chips} testID={`${testID}-applied`}>
          {appliedFilters.map((filter) => (
            <Chip
              closeIcon="close"
              /**
               * The label belongs to the close control rather than to the chip: pressing the
               * chip body does nothing, so naming the whole chip "remove filter" would promise
               * an action the user cannot take there.
               */
              closeIconAccessibilityLabel={`Bỏ bộ lọc ${filter.label}`}
              key={filter.key}
              onClose={() => onRemoveFilter(filter.key)}
              testID={`${testID}-filter-${filter.key}`}
            >
              {filter.label}
            </Chip>
          ))}

          <ActionButton
            onPress={onClearFilters}
            testID={`${testID}-clear-all`}
            variant="text"
          >
            Xoá tất cả bộ lọc
          </ActionButton>
        </View>
      )}

      {matchCount === undefined ? null : (
        <Text
          accessibilityLiveRegion="polite"
          style={{ color: theme.colors.onSurfaceVariant }}
          testID={`${testID}-match-count`}
          variant="bodyMedium"
        >
          {matchCount === 0
            ? 'Không có bản ghi nào khớp'
            : `${matchCount} bản ghi khớp`}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    gap: spacing.sm,
  },
  chips: {
    alignItems: 'center',
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.xs,
  },
});
