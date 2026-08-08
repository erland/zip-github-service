export const STAGING_CLAIM_TOKEN_KEY = 'zip-github.staging.claim-token';

export function captureClaimToken(hash: string, storage: Pick<Storage, 'setItem'>): boolean {
  const fragment = hash.startsWith('#') ? hash.slice(1) : hash;
  const token = new URLSearchParams(fragment).get('token');
  if (!token) return false;
  storage.setItem(STAGING_CLAIM_TOKEN_KEY, token);
  return true;
}
