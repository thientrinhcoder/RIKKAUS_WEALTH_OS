import { act, fireEvent, screen, waitFor } from '@testing-library/react-native';
import { Text } from 'react-native-paper';

import { FormShell } from '@/components/form/form-shell';
import { renderWithProviders } from '../../test/test-utils';

const TWO_ERRORS = [
  { name: 'name', label: 'Tên tài sản', message: 'Nhập tên tài sản.' },
  { name: 'value', label: 'Giá trị hiện tại', message: 'Giá trị phải lớn hơn 0.' },
];

/**
 * A submission that resolves late leaves React work queued after the test body returns. Draining
 * it here keeps one test's pending promise from emptying the next test's screen.
 */
afterEach(async () => {
  await act(async () => {});
});

function submitButton() {
  return screen.getByRole('button', { name: 'Lưu tài sản' });
}

describe('submitting a valid form', () => {
  it('calls the caller submit exactly once and confirms success', async () => {
    const onSubmit = jest.fn();

    await renderWithProviders(
      <FormShell
        onSubmit={onSubmit}
        submitLabel="Lưu tài sản"
        successMessage="Đã lưu tài sản."
        validate={() => []}
      >
        <Text>Nội dung biểu mẫu</Text>
      </FormShell>,
    );

    fireEvent.press(submitButton());

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.getByText('Đã lưu tài sản.')).toBeTruthy());
  });

  it('announces success politely rather than stealing focus', async () => {
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

    await waitFor(() =>
      expect(screen.getByTestId('form-success').props.accessibilityLiveRegion).toBe('polite'),
    );
  });
});

describe('preventing a duplicate submission', () => {
  /**
   * The guard itself is covered exhaustively in single-flight.test.ts, including two activations
   * in the same tick. It cannot be exercised through the rendered button: the first press
   * disables it, and pressing a disabled element in this test environment leaves the renderer
   * unusable for every test that follows.
   */
  it('accepts a second submission once the first has finished', async () => {
    const onSubmit = jest.fn().mockResolvedValue(undefined);

    await renderWithProviders(
      <FormShell onSubmit={onSubmit} submitLabel="Lưu tài sản" validate={() => []}>
        <Text>Nội dung biểu mẫu</Text>
      </FormShell>,
    );

    fireEvent.press(submitButton());
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(submitButton()).not.toBeDisabled());

    fireEvent.press(submitButton());
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(2));
  });
});

describe('submitting an invalid form', () => {
  it('renders the error summary naming every field that failed', async () => {
    await renderWithProviders(
      <FormShell onSubmit={jest.fn()} submitLabel="Lưu tài sản" validate={() => TWO_ERRORS}>
        <Text>Nội dung biểu mẫu</Text>
      </FormShell>,
    );

    fireEvent.press(submitButton());

    await waitFor(() => expect(screen.getByTestId('form-error-summary')).toBeTruthy());

    expect(screen.getByText('Còn 2 trường cần sửa')).toBeTruthy();
    expect(screen.getByText('Tên tài sản: Nhập tên tài sản.')).toBeTruthy();
    expect(screen.getByText('Giá trị hiện tại: Giá trị phải lớn hơn 0.')).toBeTruthy();
  });

  it('moves focus to the first invalid field in visual order', async () => {
    const onFocusField = jest.fn();

    await renderWithProviders(
      <FormShell
        onFocusField={onFocusField}
        onSubmit={jest.fn()}
        submitLabel="Lưu tài sản"
        validate={() => TWO_ERRORS}
      >
        <Text>Nội dung biểu mẫu</Text>
      </FormShell>,
    );

    fireEvent.press(submitButton());

    await waitFor(() => expect(onFocusField).toHaveBeenCalledWith('name'));
    expect(onFocusField).not.toHaveBeenCalledWith('value');
  });

  it('never calls the caller submit when validation failed', async () => {
    const onSubmit = jest.fn();

    await renderWithProviders(
      <FormShell onSubmit={onSubmit} submitLabel="Lưu tài sản" validate={() => TWO_ERRORS}>
        <Text>Nội dung biểu mẫu</Text>
      </FormShell>,
    );

    fireEvent.press(submitButton());

    await waitFor(() => expect(screen.getByTestId('form-error-summary')).toBeTruthy());

    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('lets a summary entry move focus to its own field', async () => {
    const onFocusField = jest.fn();

    await renderWithProviders(
      <FormShell
        onFocusField={onFocusField}
        onSubmit={jest.fn()}
        submitLabel="Lưu tài sản"
        validate={() => TWO_ERRORS}
      >
        <Text>Nội dung biểu mẫu</Text>
      </FormShell>,
    );

    fireEvent.press(submitButton());

    await waitFor(() => expect(screen.getByTestId('form-error-summary-value')).toBeTruthy());

    onFocusField.mockClear();
    fireEvent.press(screen.getByTestId('form-error-summary-value'));

    expect(onFocusField).toHaveBeenCalledWith('value');
  });

  it('clears the summary once validation passes on a later attempt', async () => {
    const validate = jest
      .fn()
      .mockReturnValueOnce(TWO_ERRORS)
      .mockReturnValue([]);

    await renderWithProviders(
      <FormShell onSubmit={jest.fn()} submitLabel="Lưu tài sản" validate={validate}>
        <Text>Nội dung biểu mẫu</Text>
      </FormShell>,
    );

    fireEvent.press(submitButton());
    await waitFor(() => expect(screen.getByTestId('form-error-summary')).toBeTruthy());

    fireEvent.press(submitButton());
    await waitFor(() => expect(screen.queryByTestId('form-error-summary')).toBeNull());
  });

  it('announces the summary assertively, because it answers an action the user just took', async () => {
    await renderWithProviders(
      <FormShell onSubmit={jest.fn()} submitLabel="Lưu tài sản" validate={() => TWO_ERRORS}>
        <Text>Nội dung biểu mẫu</Text>
      </FormShell>,
    );

    fireEvent.press(submitButton());

    await waitFor(() =>
      expect(screen.getByTestId('form-error-summary').props.accessibilityLiveRegion).toBe(
        'assertive',
      ),
    );
  });
});

describe('actions while a submission is in flight', () => {
  /**
   * This is the only test that holds a submission open, so it runs last: a promise resolved at
   * the very end of a test leaves React work queued, and that queue empties the next test's
   * screen.
   */
  it('disables cancel so the form cannot be abandoned mid-save', async () => {
    let resolveSubmit: () => void = () => {};
    const onSubmit = jest.fn(
      () =>
        new Promise<void>((resolve) => {
          resolveSubmit = resolve;
        }),
    );

    await renderWithProviders(
      <FormShell
        onCancel={jest.fn()}
        onSubmit={onSubmit}
        submitLabel="Lưu tài sản"
        validate={() => []}
      >
        <Text>Nội dung biểu mẫu</Text>
      </FormShell>,
    );

    fireEvent.press(submitButton());

    await waitFor(() => expect(screen.getByRole('button', { name: 'Huỷ' })).toBeDisabled());

    await act(async () => {
      resolveSubmit();
    });

    expect(screen.getByRole('button', { name: 'Huỷ' })).not.toBeDisabled();
  });
});
