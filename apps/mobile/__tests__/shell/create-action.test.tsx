import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { CreateAction, CREATE_RECORD_TYPES } from '@/components/shell/create-action';
import { renderWithProviders } from '../../test/test-utils';

/**
 * React 19 batches the state update that opens the sheet, so the assertion has to wait for the
 * re-render to flush rather than reading the tree in the same tick as the press.
 */
async function openSheet() {
  fireEvent.press(screen.getByRole('button', { name: 'Thêm' }));
  await waitFor(() => expect(screen.getByTestId('shell-create-sheet')).toBeTruthy());
}

describe('the Thêm action', () => {
  it('carries a visible text label, not an icon alone', async () => {
    await renderWithProviders(<CreateAction onSelectRecordType={jest.fn()} />);

    expect(screen.getByText('Thêm')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Thêm' })).toBeTruthy();
  });

  it('keeps the sheet closed until it is asked for', async () => {
    await renderWithProviders(<CreateAction onSelectRecordType={jest.fn()} />);

    expect(screen.queryByTestId('shell-create-sheet')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Tài sản' })).toBeNull();
  });

  it('offers exactly the five approved record types when opened', async () => {
    await renderWithProviders(<CreateAction onSelectRecordType={jest.fn()} />);
    await openSheet();

    expect(CREATE_RECORD_TYPES.map((type) => type.label)).toEqual([
      'Tài sản',
      'Công nợ',
      'Thu nhập',
      'Chi phí',
      'Mục tiêu',
    ]);

    for (const type of CREATE_RECORD_TYPES) {
      expect(screen.getByRole('button', { name: type.label })).toBeTruthy();
    }
  });

  it('only selects a record type and never renders a form in the sheet', async () => {
    const onSelectRecordType = jest.fn();

    await renderWithProviders(<CreateAction onSelectRecordType={onSelectRecordType} />);
    await openSheet();

    fireEvent.press(screen.getByRole('button', { name: 'Tài sản' }));

    await waitFor(() => expect(onSelectRecordType).toHaveBeenCalledWith(CREATE_RECORD_TYPES[0]));

    expect(screen.queryByLabelText(/Tên tài sản|Giá trị|Số tiền/)).toBeNull();
    expect(screen.queryByRole('none')).toBeNull();
  });

  it('closes the sheet once a type is chosen, so the form opens as a screen', async () => {
    await renderWithProviders(<CreateAction onSelectRecordType={jest.fn()} />);
    await openSheet();

    fireEvent.press(screen.getByRole('button', { name: 'Chi phí' }));

    await waitFor(() => expect(screen.queryByTestId('shell-create-sheet')).toBeNull());

    expect(screen.queryByRole('button', { name: 'Công nợ' })).toBeNull();
  });

  it('routes each record type to its own screen rather than to one generic form', () => {
    const routes = CREATE_RECORD_TYPES.map((type) => type.route);

    expect(new Set(routes).size).toBe(routes.length);

    for (const route of routes) {
      expect(route).toMatch(/^\//);
    }
  });
});
