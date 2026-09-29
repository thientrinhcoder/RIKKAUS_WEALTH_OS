import { useCallback, useRef, useState, type ReactNode } from 'react';

import { ConfirmDialog } from './confirm-dialog';
import { DestructiveDialog } from './destructive-dialog';

export interface ConfirmRequest {
  title: string;
  confirmLabel: string;
  cancelLabel?: string;
  /** Present means destructive: the consequence must be named. */
  consequence?: string;
  /** Present means ordinary: what the user is agreeing to. */
  body?: string;
}

export interface UseConfirm {
  /** Resolves true when confirmed and false when cancelled or dismissed. */
  confirm: (request: ConfirmRequest) => Promise<boolean>;
  /** Render this inside the screen so the dialog has somewhere to appear. */
  dialog: ReactNode;
}

/**
 * Gives a call site an awaitable confirmation instead of making every caller manage open state
 * and two callbacks.
 *
 * A second call while one is open resolves false immediately rather than opening a second
 * overlay: two stacked confirmations leave the user unsure which question they just answered.
 */
export function useConfirm(): UseConfirm {
  const [request, setRequest] = useState<ConfirmRequest | null>(null);
  const resolver = useRef<((confirmed: boolean) => void) | null>(null);

  const settle = useCallback((confirmed: boolean) => {
    const resolve = resolver.current;

    resolver.current = null;
    setRequest(null);
    resolve?.(confirmed);
  }, []);

  const confirm = useCallback((next: ConfirmRequest) => {
    if (resolver.current !== null) {
      return Promise.resolve(false);
    }

    return new Promise<boolean>((resolve) => {
      resolver.current = resolve;
      setRequest(next);
    });
  }, []);

  const visible = request !== null;

  const dialog =
    request === null ? null : request.consequence !== undefined ? (
      <DestructiveDialog
        cancelLabel={request.cancelLabel}
        confirmLabel={request.confirmLabel}
        consequence={request.consequence}
        onCancel={() => settle(false)}
        onConfirm={() => settle(true)}
        title={request.title}
        visible={visible}
      />
    ) : (
      <ConfirmDialog
        body={request.body ?? ''}
        cancelLabel={request.cancelLabel}
        confirmLabel={request.confirmLabel}
        onCancel={() => settle(false)}
        onConfirm={() => settle(true)}
        title={request.title}
        visible={visible}
      />
    );

  return { confirm, dialog };
}
