import { useCallback, useMemo, useState } from 'react';

/**
 * Reports whether a form holds changes the user has not saved, so the overlay slice can warn
 * before the form is dismissed. Section 11.1 makes naming the consequence and keeping a safe
 * cancel route the requirement; this hook only reports state and renders nothing.
 */
export interface UnsavedChanges {
  isDirty: boolean;
  markChanged: () => void;
  markSaved: () => void;
  reset: () => void;
}

export function useUnsavedChanges(initiallyDirty = false): UnsavedChanges {
  const [isDirty, setIsDirty] = useState(initiallyDirty);

  const markChanged = useCallback(() => setIsDirty(true), []);
  const markSaved = useCallback(() => setIsDirty(false), []);
  const reset = useCallback(() => setIsDirty(false), []);

  return useMemo(
    () => ({ isDirty, markChanged, markSaved, reset }),
    [isDirty, markChanged, markSaved, reset],
  );
}
