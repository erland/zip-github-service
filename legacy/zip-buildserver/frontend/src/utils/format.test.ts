import { describe, expect, it } from 'vitest';
import { formatDuration } from './format';

describe('formatDuration', () => {
  it('returns a dash for missing or invalid values', () => {
    expect(formatDuration(null)).toBe('—');
    expect(formatDuration(undefined)).toBe('—');
    expect(formatDuration(Number.NaN)).toBe('—');
  });

  it('formats sub-second durations as milliseconds', () => {
    expect(formatDuration(0)).toBe('0 ms');
    expect(formatDuration(999)).toBe('999 ms');
  });

  it('formats second-plus durations with one decimal place', () => {
    expect(formatDuration(1000)).toBe('1.0 s');
    expect(formatDuration(65000)).toBe('65.0 s');
  });
});
