import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import DataCatalog from '@/app/catalog/data';
import { ASSETS, NET_WORTH, formatVnd } from '@/catalog/fixture';
import { UNAVAILABLE_LABEL } from '@/components/data';
import { renderWithProviders } from '../../test/test-utils';

describe('the data example', () => {
  it('renders the fixture totals so the screen reconciles with its rows', async () => {
    await renderWithProviders(<DataCatalog />);

    expect(screen.getByTestId('catalog-kpi-net-worth-value')).toHaveTextContent(
      formatVnd(NET_WORTH),
    );
  });

  it('states that an incalculable ratio is unavailable rather than showing a zero', async () => {
    await renderWithProviders(<DataCatalog />);

    expect(screen.getByTestId('catalog-kpi-unavailable-value')).toHaveTextContent(
      UNAVAILABLE_LABEL,
    );
  });

  it('renders one row per asset record', async () => {
    await renderWithProviders(<DataCatalog />);

    for (const asset of ASSETS) {
      expect(screen.getByTestId(`catalog-row-${asset.id}-title`)).toHaveTextContent(asset.title);
    }
  });

  it('shows the original currency only on the record that has one', async () => {
    await renderWithProviders(<DataCatalog />);

    expect(screen.getByTestId('catalog-row-usd-deposit-secondary-value')).toHaveTextContent(
      '20.000,00 USD',
    );
    expect(screen.queryByTestId('catalog-row-cash-secondary-value')).toBeNull();
  });

  it('narrows the visible rows as the user searches, and states the match count', async () => {
    await renderWithProviders(<DataCatalog />);

    expect(screen.getByTestId('catalog-filter-bar-match-count')).toHaveTextContent(
      `${ASSETS.length} bản ghi khớp`,
    );

    fireEvent.changeText(screen.getByTestId('catalog-filter-bar-search-input'), 'Thảo Điền');

    await waitFor(() =>
      expect(screen.getByTestId('catalog-filter-bar-match-count')).toHaveTextContent(
        '1 bản ghi khớp',
      ),
    );

    expect(screen.getByTestId('catalog-row-real-estate-title')).toBeTruthy();
    expect(screen.queryByTestId('catalog-row-cash-title')).toBeNull();
  });

  it('offers a way out of a search that matches nothing', async () => {
    await renderWithProviders(<DataCatalog />);

    fireEvent.changeText(
      screen.getByTestId('catalog-filter-bar-search-input'),
      'không tồn tại',
    );

    await waitFor(() => expect(screen.getByTestId('catalog-list-empty-title')).toBeTruthy());

    expect(screen.getByTestId('catalog-filter-bar-match-count')).toHaveTextContent(
      'Không có bản ghi nào khớp',
    );
    /**
     * The empty state's action is named apart from the search field's own clear control, so a
     * screen reader user is not offered the same name twice for two different things.
     */
    expect(screen.getByRole('button', { name: 'Xoá từ khoá và bộ lọc' })).toBeTruthy();
  });

  it('switches the list between assets and liabilities', async () => {
    await renderWithProviders(<DataCatalog />);

    fireEvent.press(screen.getByRole('tab', { name: 'Công nợ' }));

    await waitFor(() => expect(screen.getByTestId('catalog-row-mortgage-title')).toBeTruthy());

    expect(screen.queryByTestId('catalog-row-cash-title')).toBeNull();
  });

  it('applies a filter only when the sheet is explicitly applied', async () => {
    await renderWithProviders(<DataCatalog />);

    expect(screen.getByTestId('catalog-row-cash-title')).toBeTruthy();

    fireEvent.press(screen.getByRole('button', { name: 'Mở bộ lọc' }));
    await waitFor(() => expect(screen.getByTestId('catalog-filter-sheet')).toBeTruthy());

    fireEvent.press(screen.getByTestId('catalog-filter-category-anchor'));
    await waitFor(() =>
      expect(screen.getByTestId('catalog-filter-category-options')).toBeTruthy(),
    );

    fireEvent.press(screen.getByTestId('catalog-filter-category-option-Bất động sản'));
    await waitFor(() =>
      expect(
        screen.getByTestId('catalog-filter-category-anchor').props.accessibilityValue.text,
      ).toBe('Bất động sản'),
    );

    /**
     * Nothing is applied yet. The list behind the sheet is inert to assistive technology while
     * the overlay is open, which is the modal behaviour the overlay shell guarantees, so the
     * query has to opt into hidden elements to see it at all.
     */
    expect(
      screen.getByTestId('catalog-row-cash-title', { includeHiddenElements: true }),
    ).toBeTruthy();

    fireEvent.press(screen.getByTestId('catalog-filter-sheet-apply'));

    await waitFor(() => expect(screen.queryByTestId('catalog-row-cash-title')).toBeNull());

    expect(screen.getByTestId('catalog-row-real-estate-title')).toBeTruthy();
  });

  it('uses the shared bottom sheet as the filter sheet container', async () => {
    await renderWithProviders(<DataCatalog />);

    fireEvent.press(screen.getByRole('button', { name: 'Mở bộ lọc' }));

    await waitFor(() =>
      expect(screen.getByTestId('catalog-filter-sheet-container-panel')).toBeTruthy(),
    );

    expect(
      screen.getByTestId('catalog-filter-sheet-container-scrim', { includeHiddenElements: true }),
    ).toBeTruthy();
  });
});
