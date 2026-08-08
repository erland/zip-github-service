import { describe, expect, it } from 'vitest';
import { captureClaimToken, STAGING_CLAIM_TOKEN_KEY } from './claimToken';

describe('captureClaimToken', () => {
  it('stores a fragment token without putting it in query state', () => {
    const values = new Map<string, string>();
    const storage = { setItem: (key: string, value: string) => values.set(key, value) } as Pick<Storage, 'setItem'>;
    expect(captureClaimToken('#token=secret-value', storage)).toBe(true);
    expect(values.get(STAGING_CLAIM_TOKEN_KEY)).toBe('secret-value');
  });

  it('ignores fragments without a token', () => {
    const storage = { setItem: () => { throw new Error('must not write'); } } as Pick<Storage, 'setItem'>;
    expect(captureClaimToken('#other=value', storage)).toBe(false);
  });
});
