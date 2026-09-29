import { act, fireEvent, screen, waitFor } from '@testing-library/react-native';
import { Text } from 'react-native-paper';

import { FormShell } from '@/components/form/form-shell';
import { renderWithProviders } from '../../test/test-utils';

/**
 * The confirmation tests live apart from the rest of the form-shell suite.
 *
 * A raised snackbar keeps a dwell timer running after the test body returns, and that pending
 * work empties the screen of whatever test runs next in the same file. Isolating them is
 * cheaper and clearer than teaching every other test to clean up after these.
 */
function submitButton() {
  return screen.getByRole('button', { name: 'Lưu tài sản' });
}

afterEach(async () => {
  await act(async () => {});
});

describe('the submission confirmation', () => {
  it('raises the confirmation as a snackbar rather than parking it under the form', async () => {
    await renderWithProviders(
      <FormShell
        onSubmit={jest.fn()}
        submitLabel="Lưu tài sản"
        successMessage="Đã lưu tài sản."
        validate={() => []}
      >
        <Text>Nội dung biểu mẫu</Text>
      </FormShell>,
    );

    fireEvent.press(submitButton());

    await waitFor(() => expect(screen.getByTestId('snackbar-message')).toBeTruthy());

    expect(screen.getByTestId('snackbar-message')).toHaveTextContent('Đã lưu tài sản.');
    /** Polite, and it never takes focus. */
    expect(screen.getByTestId('snackbar').props.accessibilityLiveRegion).toBe('polite');
    expect(screen.queryByTestId('form-success')).toBeNull();
  });

  it('offers undo on the confirmation when the caller passes one', async () => {
    const onUndoSubmit = jest.fn();

    await renderWithProviders(
      <FormShell
        onSubmit={jest.fn()}
        onUndoSubmit={onUndoSubmit}
        submitLabel="Lưu tài sản"
        successMessage="Đã xoá tài sản."
        validate={() => []}
      >
        <Text>Nội dung biểu mẫu</Text>
      </FormShell>,
    );

    fireEvent.press(submitButton());

    await waitFor(() => expect(screen.getByRole('button', { name: 'Hoàn tác' })).toBeTruthy());

    fireEvent.press(screen.getByRole('button', { name: 'Hoàn tác' }));

    expect(onUndoSubmit).toHaveBeenCalledWith();
  });

  it('raises no snackbar when the caller gave no confirmation message', async () => {
    await renderWithProviders(
      <FormShell onSubmit={jest.fn()} submitLabel="Lưu tài sản" validate={() => []}>
        <Text>Nội dung biểu mẫu</Text>
      </FormShell>,
    );

    fireEvent.press(submitButton());

    await act(async () => {});

    expect(screen.queryByTestId('snackbar')).toBeNull();
  });
});
