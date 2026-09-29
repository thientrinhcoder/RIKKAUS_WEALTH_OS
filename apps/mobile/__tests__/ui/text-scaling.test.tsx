import { screen } from '@testing-library/react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { typographyRoles } from '@/ui/typography';
import { renderWithProviders } from '../../test/test-utils';

const LONG_LABEL = 'Cập nhật định giá tài sản và ghi nhận lịch sử thay đổi';

describe('text scaling', () => {
  it('never pins a line count on a typography role, so text wraps instead of clipping', () => {
    for (const role of Object.values(typographyRoles)) {
      expect(role).not.toHaveProperty('numberOfLines');
      expect(role).not.toHaveProperty('ellipsizeMode');
    }
  });

  it('renders a long Vietnamese label in full rather than truncating it', async () => {
    await renderWithProviders(
      <ActionButton onPress={jest.fn()} testID="action-long" variant="primary">
        {LONG_LABEL}
      </ActionButton>,
    );

    expect(screen.getByText(LONG_LABEL)).toBeTruthy();
    expect(screen.getByRole('button', { name: LONG_LABEL })).toBeTruthy();
  });

  it('lets a text role grow with the platform font scale rather than allowing opt-out', async () => {
    await renderWithProviders(<Text variant="bodyLarge">{LONG_LABEL}</Text>);

    const node = screen.getByText(LONG_LABEL);

    expect(node.props.allowFontScaling).not.toBe(false);
    expect(node.props.numberOfLines).toBeUndefined();
  });
});
