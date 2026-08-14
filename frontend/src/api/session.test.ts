import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  assertSessionActive,
  resetSessionExpiredForTests,
  SessionExpiredError,
  signalSessionExpired,
  subscribeSessionExpired,
} from './session';

afterEach(() => {
  resetSessionExpiredForTests();
});

describe('session expiry signaling', () => {
  it('signals and throws a dedicated error for HTTP 401', () => {
    const listener = vi.fn();
    const unsubscribe = subscribeSessionExpired(listener);

    expect(() => assertSessionActive({ status: 401 })).toThrow(SessionExpiredError);
    expect(listener).toHaveBeenCalledTimes(1);

    unsubscribe();
  });

  it('does not treat forbidden or ordinary API failures as session expiry', () => {
    const listener = vi.fn();
    const unsubscribe = subscribeSessionExpired(listener);

    expect(() => assertSessionActive({ status: 403 })).not.toThrow();
    expect(() => assertSessionActive({ status: 500 })).not.toThrow();
    expect(listener).not.toHaveBeenCalled();

    unsubscribe();
  });

  it('notifies a late subscriber when expiry happened before the layout listener was installed', async () => {
    signalSessionExpired();
    const listener = vi.fn();
    const unsubscribe = subscribeSessionExpired(listener);

    await Promise.resolve();
    expect(listener).toHaveBeenCalledTimes(1);

    unsubscribe();
  });
});
