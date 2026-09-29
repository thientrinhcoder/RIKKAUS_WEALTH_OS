import { useState, type ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { FilterBar } from '@/components/data/filter-bar';
import { FilterSheet, type FilterSheetContainerProps } from '@/components/data/filter-sheet';
import { SegmentedControl } from '@/components/data/segmented-control';
import { Tabs } from '@/components/data/tabs';
import { SelectField } from '@/components/form/fields/select-field';
import { sizing } from '@/ui/tokens';
import { renderWithProviders } from '../../test/test-utils';

const TAB_ITEMS = [
  { value: 'detail', label: 'Chi tiết' },
  { value: 'history', label: 'Lịch sử định giá' },
  { value: 'documents', label: 'Chứng từ', disabled: true },
];

describe('tabs', () => {
  it('marks exactly one tab selected and the rest unselected', async () => {
    await renderWithProviders(
      <Tabs
        accessibilityLabel="Phần của tài sản"
        items={TAB_ITEMS}
        onChange={jest.fn()}
        value="history"
      />,
    );

    const states = TAB_ITEMS.map((item) => ({
      label: item.label,
      selected: screen.getByRole('tab', { name: item.label }).props.accessibilityState.selected,
    }));

    expect(states).toEqual([
      { label: 'Chi tiết', selected: false },
      { label: 'Lịch sử định giá', selected: true },
      { label: 'Chứng từ', selected: false },
    ]);
  });

  it('exposes the disabled tab as disabled and does not report a change from it', async () => {
    const onChange = jest.fn();

    await renderWithProviders(
      <Tabs
        accessibilityLabel="Phần của tài sản"
        items={TAB_ITEMS}
        onChange={onChange}
        value="detail"
      />,
    );

    const disabled = screen.getByRole('tab', { name: 'Chứng từ' });

    expect(disabled.props.accessibilityState.disabled).toBe(true);

    fireEvent.press(disabled);

    expect(onChange).not.toHaveBeenCalled();
  });

  it('reports the tab the user chose', async () => {
    const onChange = jest.fn();

    await renderWithProviders(
      <Tabs
        accessibilityLabel="Phần của tài sản"
        items={TAB_ITEMS}
        onChange={onChange}
        value="detail"
      />,
    );

    fireEvent.press(screen.getByRole('tab', { name: 'Lịch sử định giá' }));

    expect(onChange).toHaveBeenCalledWith('history');
  });

  it('names the tab list, so two lists on one screen stay distinguishable', async () => {
    await renderWithProviders(
      <Tabs
        accessibilityLabel="Phần của tài sản"
        items={TAB_ITEMS}
        onChange={jest.fn()}
        value="detail"
      />,
    );

    expect(screen.getByTestId('tabs').props.accessibilityLabel).toBe('Phần của tài sản');
  });
});

describe('segmented control', () => {
  const PERIODS = [
    { value: 'month', label: 'Tháng này' },
    { value: 'quarter', label: 'Quý này' },
    { value: 'year', label: 'Năm nay' },
  ];

  it('checks exactly one segment', async () => {
    await renderWithProviders(
      <SegmentedControl
        accessibilityLabel="Kỳ xem dòng tiền"
        items={PERIODS}
        onChange={jest.fn()}
        value="quarter"
      />,
    );

    const checked = PERIODS.filter(
      (item) =>
        screen.getByRole('radio', { name: item.label }).props.accessibilityState.checked === true,
    );

    expect(checked).toHaveLength(1);
    expect(checked[0].value).toBe('quarter');
  });

  it('reports the segment the user chose', async () => {
    const onChange = jest.fn();

    await renderWithProviders(
      <SegmentedControl
        accessibilityLabel="Kỳ xem dòng tiền"
        items={PERIODS}
        onChange={onChange}
        value="month"
      />,
    );

    fireEvent.press(screen.getByRole('radio', { name: 'Năm nay' }));

    expect(onChange).toHaveBeenCalledWith('year');
  });

  it('takes every label from props, holding no vocabulary of its own', async () => {
    await renderWithProviders(
      <SegmentedControl
        accessibilityLabel="Loại bản ghi"
        items={[
          { value: 'in', label: 'Thu' },
          { value: 'out', label: 'Chi' },
        ]}
        onChange={jest.fn()}
        value="in"
      />,
    );

    expect(screen.getByText('Thu')).toBeTruthy();
    expect(screen.getByText('Chi')).toBeTruthy();
    expect(screen.queryByText('Tháng này')).toBeNull();
  });

  it('keeps every segment at or above the minimum touch target', async () => {
    await renderWithProviders(
      <SegmentedControl
        accessibilityLabel="Kỳ xem dòng tiền"
        items={PERIODS}
        onChange={jest.fn()}
        value="month"
      />,
    );

    for (const item of PERIODS) {
      const style = StyleSheet.flatten(
        screen.getByTestId(`segmented-${item.value}`).props.style,
      ) as { minHeight?: number };

      expect(style.minHeight).toBeGreaterThanOrEqual(sizing.minTouchTarget.ios);
    }
  });
});

describe('filter bar', () => {
  const APPLIED = [
    { key: 'kind:real-estate', label: 'Bất động sản' },
    { key: 'currency:usd', label: 'Nguyên tệ USD' },
  ];

  it('reuses the shared search field rather than defining another one', async () => {
    await renderWithProviders(
      <FilterBar
        appliedFilters={[]}
        onClearFilters={jest.fn()}
        onQueryChange={jest.fn()}
        onRemoveFilter={jest.fn()}
        query=""
        searchLabel="Tìm tài sản"
      />,
    );

    expect(screen.getByTestId('filter-bar-search-input').props.accessibilityRole).toBe('search');
  });

  it('holds no query of its own and reports every change to the caller', async () => {
    const onQueryChange = jest.fn();

    await renderWithProviders(
      <FilterBar
        appliedFilters={[]}
        onClearFilters={jest.fn()}
        onQueryChange={onQueryChange}
        onRemoveFilter={jest.fn()}
        query="Vietcom"
        searchLabel="Tìm tài sản"
      />,
    );

    expect(screen.getByTestId('filter-bar-search-input').props.value).toBe('Vietcom');

    fireEvent.changeText(screen.getByTestId('filter-bar-search-input'), 'Vietcombank');

    expect(onQueryChange).toHaveBeenCalledWith('Vietcombank');
  });

  it('shows each applied filter and lets it be removed on its own', async () => {
    const onRemoveFilter = jest.fn();

    await renderWithProviders(
      <FilterBar
        appliedFilters={APPLIED}
        onClearFilters={jest.fn()}
        onQueryChange={jest.fn()}
        onRemoveFilter={onRemoveFilter}
        query=""
        searchLabel="Tìm tài sản"
      />,
    );

    expect(screen.getByText('Bất động sản')).toBeTruthy();
    expect(screen.getByText('Nguyên tệ USD')).toBeTruthy();

    fireEvent.press(screen.getByLabelText('Bỏ bộ lọc Bất động sản'));

    expect(onRemoveFilter).toHaveBeenCalledWith('kind:real-estate');
  });

  it('offers one control that clears every filter at once', async () => {
    const onClearFilters = jest.fn();

    await renderWithProviders(
      <FilterBar
        appliedFilters={APPLIED}
        onClearFilters={onClearFilters}
        onQueryChange={jest.fn()}
        onRemoveFilter={jest.fn()}
        query=""
        searchLabel="Tìm tài sản"
      />,
    );

    fireEvent.press(screen.getByRole('button', { name: 'Xoá tất cả bộ lọc' }));

    expect(onClearFilters).toHaveBeenCalledTimes(1);
  });

  it('offers nothing to clear when no filter is applied', async () => {
    await renderWithProviders(
      <FilterBar
        appliedFilters={[]}
        onClearFilters={jest.fn()}
        onQueryChange={jest.fn()}
        onRemoveFilter={jest.fn()}
        query=""
        searchLabel="Tìm tài sản"
      />,
    );

    expect(screen.queryByRole('button', { name: 'Xoá tất cả bộ lọc' })).toBeNull();
  });

  it('states how many records match, and announces the count politely', async () => {
    await renderWithProviders(
      <FilterBar
        appliedFilters={APPLIED}
        matchCount={4}
        onClearFilters={jest.fn()}
        onQueryChange={jest.fn()}
        onRemoveFilter={jest.fn()}
        query="Vietcombank"
        searchLabel="Tìm tài sản"
      />,
    );

    const count = screen.getByTestId('filter-bar-match-count');

    expect(count).toHaveTextContent('4 bản ghi khớp');
    expect(count.props.accessibilityLiveRegion).toBe('polite');
  });

  it('says plainly that nothing matched rather than showing an empty count', async () => {
    await renderWithProviders(
      <FilterBar
        appliedFilters={[]}
        matchCount={0}
        onClearFilters={jest.fn()}
        onQueryChange={jest.fn()}
        onRemoveFilter={jest.fn()}
        query="Không tồn tại"
        searchLabel="Tìm tài sản"
      />,
    );

    expect(screen.getByTestId('filter-bar-match-count')).toHaveTextContent(
      'Không có bản ghi nào khớp',
    );
  });
});

describe('filter sheet', () => {
  interface Filters {
    kind: string;
  }

  /** Stands in for the shared bottom sheet until the overlay slice provides one. */
  function TestContainer({
    visible,
    children,
    accessibilityLabel,
    testID,
  }: FilterSheetContainerProps) {
    return visible ? (
      <View accessibilityLabel={accessibilityLabel} testID={testID}>
        {children as ReactNode}
      </View>
    ) : null;
  }

  function Harness({ onApply }: { onApply: (filters: Filters) => void }) {
    const [visible, setVisible] = useState(true);
    const [applied, setApplied] = useState<Filters>({ kind: '' });

    return (
      <FilterSheet<Filters>
        applied={applied}
        container={TestContainer}
        onApply={(filters) => {
          setApplied(filters);
          setVisible(false);
          onApply(filters);
        }}
        onDismiss={() => setVisible(false)}
        visible={visible}
      >
        {(staged, setStaged) => (
          <SelectField
            label="Loại tài sản"
            onChangeValue={(kind) => setStaged({ ...staged, kind })}
            options={[
              { value: 'real-estate', label: 'Bất động sản' },
              { value: 'savings', label: 'Tiền gửi tiết kiệm' },
            ]}
            testID="kind"
            value={staged.kind}
          />
        )}
      </FilterSheet>
    );
  }

  it('renders its content inside the container it was given', async () => {
    await renderWithProviders(<Harness onApply={jest.fn()} />);

    expect(screen.getByTestId('filter-sheet-container')).toContainElement(
      screen.getByTestId('filter-sheet'),
    );
  });

  it('applies the staged filters only on the explicit apply action', async () => {
    const onApply = jest.fn();

    await renderWithProviders(<Harness onApply={onApply} />);

    fireEvent.press(screen.getByTestId('kind-anchor'));
    await waitFor(() => expect(screen.getByTestId('kind-option-savings')).toBeTruthy());
    fireEvent.press(screen.getByTestId('kind-option-savings'));

    /**
     * React 19 batches the staged update, so the selection has to be visible before the apply
     * press, or apply would send the value from before the choice.
     */
    await waitFor(() => expect(screen.getByText('Tiền gửi tiết kiệm')).toBeTruthy());

    expect(onApply).not.toHaveBeenCalled();

    fireEvent.press(screen.getByTestId('filter-sheet-apply'));

    await waitFor(() => expect(onApply).toHaveBeenCalledWith({ kind: 'savings' }));
  });

  it('discards staged changes when dismissed without applying', async () => {
    const onApply = jest.fn();

    await renderWithProviders(<Harness onApply={onApply} />);

    fireEvent.press(screen.getByTestId('kind-anchor'));
    await waitFor(() => expect(screen.getByTestId('kind-option-savings')).toBeTruthy());
    fireEvent.press(screen.getByTestId('kind-option-savings'));

    await waitFor(() => expect(screen.getByText('Tiền gửi tiết kiệm')).toBeTruthy());

    fireEvent.press(screen.getByTestId('filter-sheet-cancel'));

    await waitFor(() => expect(screen.queryByTestId('filter-sheet')).toBeNull());

    expect(onApply).not.toHaveBeenCalled();
  });
});
