import { useWindowDimensions } from 'react-native';
import { fireEvent, screen } from '@testing-library/react-native';

import { ListView } from '@/components/data/list-view';
import { RecordRow } from '@/components/data/rows/record-row';
import { UNAVAILABLE_LABEL } from '@/components/data/cards/kpi-card';
import { renderWithProviders } from '../../test/test-utils';

jest.mock('react-native/Libraries/Utilities/useWindowDimensions');

const mockedUseWindowDimensions = jest.mocked(useWindowDimensions);

beforeEach(() => {
  mockedUseWindowDimensions.mockReturnValue({ width: 375, height: 812, scale: 2, fontScale: 1 });
});

/** The five shapes section 16.2 requires, expressed through the one row primitive. */
const SHAPES = [
  {
    name: 'asset',
    props: {
      title: 'Căn hộ Thảo Điền',
      category: 'Bất động sản',
      primaryValue: '8.000.000.000 ₫',
      date: 'Định giá 12/09/2026',
    },
  },
  {
    name: 'liability',
    props: {
      title: 'Vay mua nhà Techcombank',
      category: 'Vay thế chấp',
      primaryValue: '2.400.000.000 ₫',
      date: 'Kỳ tới 05/10/2026',
    },
  },
  {
    name: 'cash-flow',
    props: {
      title: 'Lương tháng',
      category: 'Thu nhập cố định',
      primaryValue: '85.000.000 ₫',
      date: 'Hằng tháng',
    },
  },
  {
    name: 'obligation',
    props: {
      title: 'Trả góp ô tô',
      primaryValue: '18.500.000 ₫',
      date: 'Đến hạn 20/10/2026',
    },
  },
  {
    name: 'valuation-history',
    props: {
      title: 'Định giá tháng 6',
      primaryValue: '7.800.000.000 ₫',
      secondaryValue: 'Tăng 200.000.000 ₫ so với kỳ trước',
      date: '30/06/2026',
    },
  },
] as const;

describe('the one row primitive covers all five shapes', () => {
  it.each(SHAPES)('renders the $name shape', async ({ props }) => {
    await renderWithProviders(<RecordRow {...props} testID="row" />);

    expect(screen.getByTestId('row-title')).toHaveTextContent(props.title);
    expect(screen.getByTestId('row-primary-value')).toHaveTextContent(props.primaryValue);
  });

  it('omits the metadata line entirely when there is none to show', async () => {
    await renderWithProviders(
      <RecordRow primaryValue="18.500.000 ₫" testID="row" title="Trả góp ô tô" />,
    );

    expect(screen.queryByTestId('row-metadata')).toBeNull();
  });

  it('states an unavailable value rather than showing a zero', async () => {
    await renderWithProviders(<RecordRow testID="row" title="Cổ phiếu chưa định giá" />);

    expect(screen.getByTestId('row-primary-value')).toHaveTextContent(UNAVAILABLE_LABEL);
  });

  it('exposes a button role only when the row leads somewhere', async () => {
    await renderWithProviders(
      <RecordRow primaryValue="8.000.000.000 ₫" testID="row" title="Căn hộ Thảo Điền" />,
    );

    expect(screen.queryByRole('button')).toBeNull();
  });

  it('names the row by its title and value when it is tappable', async () => {
    const onPress = jest.fn();

    await renderWithProviders(
      <RecordRow
        onPress={onPress}
        primaryValue="8.000.000.000 ₫"
        testID="row"
        title="Căn hộ Thảo Điền"
      />,
    );

    fireEvent.press(screen.getByRole('button', { name: 'Căn hộ Thảo Điền: 8.000.000.000 ₫' }));

    expect(onPress).toHaveBeenCalledTimes(1);
  });
});

describe('reflow at a large text size', () => {
  it('never truncates a long Vietnamese title or its amount', async () => {
    mockedUseWindowDimensions.mockReturnValue({
      width: 360,
      height: 780,
      scale: 2,
      fontScale: 1.6,
    });

    await renderWithProviders(
      <RecordRow
        category="Bất động sản để ở, đồng sở hữu với người thân"
        primaryValue="12.002.000.000 ₫"
        testID="row"
        title="Căn hộ chung cư cao cấp tại Thảo Điền, Thành phố Thủ Đức"
      />,
    );

    for (const testID of ['row-title', 'row-primary-value']) {
      expect(screen.getByTestId(testID).props.numberOfLines).toBeUndefined();
      expect(screen.getByTestId(testID).props.ellipsizeMode).toBeUndefined();
    }
  });
});

describe('list view', () => {
  const ITEMS = [
    { id: 'a', title: 'Căn hộ Thảo Điền' },
    { id: 'b', title: 'Tiền gửi Vietcombank' },
    { id: 'c', title: 'Cổ phiếu FPT' },
  ];

  function renderList(items: typeof ITEMS, onEmptyAction = jest.fn()) {
    return renderWithProviders(
      <ListView
        emptyActionLabel="Thêm tài sản đầu tiên"
        emptyDescription="Thêm tài sản để bắt đầu theo dõi giá trị ròng."
        emptyTitle="Chưa có tài sản nào"
        items={items}
        keyExtractor={(item) => item.id}
        onEmptyAction={onEmptyAction}
        renderItem={(item) => <RecordRow testID={`row-${item.id}`} title={item.title} />}
      />,
    );
  }

  it('renders one row per item', async () => {
    await renderList(ITEMS);

    for (const item of ITEMS) {
      expect(screen.getByTestId(`row-${item.id}-title`)).toHaveTextContent(item.title);
    }
  });

  it('puts a divider between rows and never after the last one', async () => {
    await renderList(ITEMS);

    expect(screen.getByTestId('list-divider-a')).toBeTruthy();
    expect(screen.getByTestId('list-divider-b')).toBeTruthy();
    expect(screen.queryByTestId('list-divider-c')).toBeNull();
  });

  it('shows no divider at all for a single row', async () => {
    await renderList([ITEMS[0]]);

    expect(screen.queryByTestId('list-divider-a')).toBeNull();
  });

  it('offers the next action instead of a bare message when empty', async () => {
    const onEmptyAction = jest.fn();

    await renderList([], onEmptyAction);

    expect(screen.getByTestId('list-empty-title')).toHaveTextContent('Chưa có tài sản nào');

    fireEvent.press(screen.getByRole('button', { name: 'Thêm tài sản đầu tiên' }));

    expect(onEmptyAction).toHaveBeenCalledTimes(1);
  });

  it('renders no rows when empty', async () => {
    await renderList([]);

    expect(screen.queryByTestId('row-a-title')).toBeNull();
  });
});
