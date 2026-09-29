import { StyleSheet } from 'react-native';
import { fireEvent, screen } from '@testing-library/react-native';
import { Text } from 'react-native-paper';

import { CardShell } from '@/components/data/cards/card-shell';
import { EmptyCard } from '@/components/data/cards/empty-card';
import { GoalCard } from '@/components/data/cards/goal-card';
import { InsightCard } from '@/components/data/cards/insight-card';
import { KpiCard, UNAVAILABLE_LABEL } from '@/components/data/cards/kpi-card';
import { RecordCard } from '@/components/data/cards/record-card';
import { radius, sizing } from '@/ui/tokens';
import { renderWithProviders } from '../../test/test-utils';

function flattenedStyle(testID: string) {
  return (StyleSheet.flatten(screen.getByTestId(testID).props.style) ?? {}) as Record<
    string,
    number
  >;
}

describe('card shell', () => {
  it('exposes no button role when the card does nothing', async () => {
    await renderWithProviders(
      <CardShell testID="card">
        <Text>Tổng tài sản</Text>
      </CardShell>,
    );

    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByTestId('card')).toBeTruthy();
  });

  it('exposes a named button role when the card is tappable', async () => {
    const onPress = jest.fn();

    await renderWithProviders(
      <CardShell accessibilityLabel="Mở chi tiết tài sản" onPress={onPress} testID="card">
        <Text>Căn hộ Thảo Điền</Text>
      </CardShell>,
    );

    const card = screen.getByRole('button', { name: 'Mở chi tiết tài sản' });

    fireEvent.press(card);

    expect(onPress).toHaveBeenCalledTimes(1);
  });

  it('keeps a tappable card at or above the minimum touch target', async () => {
    await renderWithProviders(
      <CardShell accessibilityLabel="Mở chi tiết" onPress={jest.fn()} testID="card">
        <Text>Căn hộ Thảo Điền</Text>
      </CardShell>,
    );

    expect(flattenedStyle('card').minHeight).toBeGreaterThanOrEqual(sizing.minTouchTarget.ios);
  });

  it('uses a card radius inside the approved band', () => {
    expect(radius.card).toBeGreaterThanOrEqual(12);
    expect(radius.card).toBeLessThanOrEqual(16);
  });
});

describe('KPI card', () => {
  it('renders the label and the value as separately readable text', async () => {
    await renderWithProviders(
      <KpiCard label="Tổng tài sản ròng" testID="kpi" value="12.002.000.000 ₫" />,
    );

    expect(screen.getByText('Tổng tài sản ròng')).toBeTruthy();
    expect(screen.getByTestId('kpi-value')).toHaveTextContent('12.002.000.000 ₫');
  });

  it('states that a figure is unavailable rather than showing a zero', async () => {
    await renderWithProviders(<KpiCard label="Tỷ lệ nợ trên tài sản" testID="kpi" />);

    expect(screen.getByTestId('kpi-value')).toHaveTextContent(UNAVAILABLE_LABEL);
    expect(screen.queryByText('0')).toBeNull();
    expect(screen.queryByText('0 ₫')).toBeNull();
  });

  it('carries the unavailable state into the accessible name', async () => {
    await renderWithProviders(
      <KpiCard label="Tỷ lệ nợ trên tài sản" onPress={jest.fn()} testID="kpi" />,
    );

    expect(
      screen.getByRole('button', { name: `Tỷ lệ nợ trên tài sản: ${UNAVAILABLE_LABEL}` }),
    ).toBeTruthy();
  });
});

describe('record card', () => {
  it('renders each part so a caller can address them individually', async () => {
    await renderWithProviders(
      <RecordCard
        category="Bất động sản"
        date="Cập nhật 12/09/2026"
        primaryValue="8.000.000.000 ₫"
        testID="rec"
        title="Căn hộ Thảo Điền"
      />,
    );

    expect(screen.getByTestId('rec-title')).toHaveTextContent('Căn hộ Thảo Điền');
    expect(screen.getByTestId('rec-category')).toHaveTextContent('Bất động sản');
    expect(screen.getByTestId('rec-primary-value')).toHaveTextContent('8.000.000.000 ₫');
    expect(screen.getByTestId('rec-date')).toHaveTextContent('Cập nhật 12/09/2026');
  });

  it('shows the original currency only when one is supplied', async () => {
    await renderWithProviders(
      <RecordCard primaryValue="1.250.750.000 ₫" testID="rec" title="Tiền gửi USD" />,
    );

    expect(screen.queryByTestId('rec-secondary-value')).toBeNull();
  });

  it('shows the original currency beside the converted value when it is supplied', async () => {
    await renderWithProviders(
      <RecordCard
        primaryValue="1.250.750.000 ₫"
        secondaryValue="49.830,68 USD"
        testID="rec"
        title="Tiền gửi USD"
      />,
    );

    expect(screen.getByTestId('rec-secondary-value')).toHaveTextContent('49.830,68 USD');
  });
});

describe('insight card', () => {
  const STATUS = { label: 'Cần chú ý', icon: 'alert-outline', tone: 'warning' } as const;

  it('renders the observation and the suggested action', async () => {
    await renderWithProviders(
      <InsightCard
        action="Xem lại lịch trả nợ trong 3 tháng tới."
        observation="Tỷ lệ nợ trên tài sản là 42%, cao hơn ngưỡng tham chiếu 35%."
        status={STATUS}
        testID="ins"
        title="Tỷ lệ nợ vượt ngưỡng tham chiếu"
      />,
    );

    expect(screen.getByTestId('ins-observation')).toHaveTextContent(/42%/);
    expect(screen.getByTestId('ins-action')).toHaveTextContent(/Xem lại lịch trả nợ/);
  });

  it('conveys status with text, never colour alone', async () => {
    await renderWithProviders(
      <InsightCard
        observation="Tỷ lệ nợ trên tài sản là 42%."
        status={STATUS}
        testID="ins"
        title="Tỷ lệ nợ vượt ngưỡng"
      />,
    );

    expect(screen.getByTestId('ins-status')).toHaveTextContent('Cần chú ý');
  });

  it('takes its status vocabulary from the caller rather than hard-coding it', async () => {
    await renderWithProviders(
      <InsightCard
        observation="Số dư quỹ khẩn cấp đủ cho 7 tháng chi tiêu."
        status={{ label: 'Đang ổn', icon: 'check-circle-outline', tone: 'success' }}
        testID="ins"
        title="Quỹ khẩn cấp"
      />,
    );

    expect(screen.getByTestId('ins-status')).toHaveTextContent('Đang ổn');
  });
});

describe('goal card', () => {
  it('exposes progress as an accessible value, not only as a bar', async () => {
    await renderWithProviders(
      <GoalCard
        progress={0.42}
        progressLabel="840.000.000 ₫ trên mục tiêu 2.000.000.000 ₫"
        testID="goal"
        title="Mua nhà"
      />,
    );

    expect(screen.getByTestId('goal-progress').props.accessibilityValue).toMatchObject({
      now: 42,
      text: '42%',
    });
  });

  it('keeps an out-of-range progress inside 0 to 100 rather than reporting nonsense', async () => {
    await renderWithProviders(
      <GoalCard progress={1.8} progressLabel="Đã vượt mục tiêu" testID="goal" title="Mua nhà" />,
    );

    expect(screen.getByTestId('goal-progress').props.accessibilityValue).toMatchObject({
      now: 100,
    });
  });

  it('renders goal vocabulary supplied by the domain', async () => {
    await renderWithProviders(
      <GoalCard
        progress={0.2}
        progressLabel="400.000.000 ₫ trên mục tiêu 2.000.000.000 ₫"
        statusLabel="Chậm tiến độ"
        testID="goal"
        title="Mua nhà"
      />,
    );

    expect(screen.getByTestId('goal-status')).toHaveTextContent('Chậm tiến độ');
  });
});

describe('empty card', () => {
  it('names what is absent and offers exactly one next action', async () => {
    const onAction = jest.fn();

    await renderWithProviders(
      <EmptyCard
        actionLabel="Thêm tài sản đầu tiên"
        description="Thêm tài sản để bắt đầu theo dõi giá trị ròng."
        onAction={onAction}
        testID="empty"
        title="Chưa có tài sản nào"
      />,
    );

    expect(screen.getByTestId('empty-title')).toHaveTextContent('Chưa có tài sản nào');
    expect(screen.getAllByRole('button')).toHaveLength(1);

    fireEvent.press(screen.getByRole('button', { name: 'Thêm tài sản đầu tiên' }));

    expect(onAction).toHaveBeenCalledTimes(1);
  });
});
