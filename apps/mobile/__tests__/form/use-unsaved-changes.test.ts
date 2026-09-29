import { act, renderHook } from '@testing-library/react-native';

/**
 * React 19 batches these updates, so each act has to be awaited for the hook to re-render
 * before the assertion reads it.
 */

import { useUnsavedChanges } from '@/components/form/use-unsaved-changes';

describe('unsaved changes', () => {
  it('starts clean', async () => {
    const { result } = await renderHook(() => useUnsavedChanges());

    expect(result.current.isDirty).toBe(false);
  });

  it('becomes dirty after a change', async () => {
    const { result } = await renderHook(() => useUnsavedChanges());

    await act(async () => {
      result.current.markChanged();
    });

    expect(result.current.isDirty).toBe(true);
  });

  it('becomes clean again after a successful save', async () => {
    const { result } = await renderHook(() => useUnsavedChanges());

    await act(async () => {
      result.current.markChanged();
    });
    await act(async () => {
      result.current.markSaved();
    });

    expect(result.current.isDirty).toBe(false);
  });

  it('becomes clean again after a reset', async () => {
    const { result } = await renderHook(() => useUnsavedChanges());

    await act(async () => {
      result.current.markChanged();
    });
    await act(async () => {
      result.current.reset();
    });

    expect(result.current.isDirty).toBe(false);
  });

  it('renders nothing itself, so the warning dialog stays with the overlay slice', async () => {
    const { result } = await renderHook(() => useUnsavedChanges());

    expect(Object.keys(result.current).sort()).toEqual([
      'isDirty',
      'markChanged',
      'markSaved',
      'reset',
    ]);
  });
});
