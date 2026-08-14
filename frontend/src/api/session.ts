export const SESSION_EXPIRED_EVENT = 'zip-github:session-expired';

let sessionExpired = false;

export class SessionExpiredError extends Error {
  constructor() {
    super('Din session har gått ut. Logga in igen för att fortsätta.');
    this.name = 'SessionExpiredError';
  }
}

export function assertSessionActive(response: Pick<Response, 'status'>): void {
  if (response.status !== 401) return;
  signalSessionExpired();
  throw new SessionExpiredError();
}

export function signalSessionExpired(): void {
  if (sessionExpired) return;
  sessionExpired = true;
  window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
}

export function subscribeSessionExpired(listener: () => void): () => void {
  let active = true;
  const handler = () => listener();
  window.addEventListener(SESSION_EXPIRED_EVENT, handler);
  if (sessionExpired) queueMicrotask(() => { if (active) listener(); });
  return () => {
    active = false;
    window.removeEventListener(SESSION_EXPIRED_EVENT, handler);
  };
}

export function resetSessionExpiredForTests(): void {
  sessionExpired = false;
}
