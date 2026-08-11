import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { RepositoryEntry } from '../api/repositories';
import { markRepositoryRecent } from './recentRepositories';
import { stableName, suggestRepository } from './repositorySuggestion';

const repo = (name: string, extra: Partial<RepositoryEntry> = {}): RepositoryEntry => ({
  githubInstallationId: 10, githubRepositoryId: Math.floor(Math.random() * 100000), repositoryFullName: `erland/${name}`,
  repositoryName: name, privateRepository: true, defaultBranch: 'main', htmlUrl: `https://github.com/erland/${name}`, projectId: null, ...extra,
});

beforeEach(() => { localStorage.clear(); vi.useRealTimers(); });

describe('repositorySuggestion', () => {
  it('normalizes common repository and revision noise', () => {
    expect(stableName('fyrens-vaktare-v0.8.8-repo-cleanup.zip')).toBe('fyrens-vaktare');
    expect(stableName('bradspel-fyrens-vaktare')).toBe('fyrens-vaktare');
  });

  it('suggests a strong filename match without selecting anything implicitly', () => {
    const target = repo('bradspel-fyrens-vaktare');
    const result = suggestRepository('fyrens-vaktare-v0.8.8.zip', [repo('annat-spel'), target]);
    expect(result?.repository).toBe(target);
    expect(result?.confidence).toBe('high');
  });

  it('uses a previous upload filename as a strong recurring-project signal', () => {
    const target = repo('mystiskt-repo', { projectId: 'p1', lastSourceFilename: 'de-fyra-elementens-vaktare-r0040.zip' });
    const result = suggestRepository('de-fyra-elementens-vaktare-r0041.zip', [repo('de-fyra-elementens-historia'), target]);
    expect(result?.repository).toBe(target);
    expect(result?.reason).toMatch(/tidigare uppladdning/i);
  });

  it('does not suggest when two candidates are too close', () => {
    expect(suggestRepository('granslinjen-r0042.zip', [repo('roman-granslinjen'), repo('bradspel-granslinjen')])).toBeNull();
  });

  it('uses recency only as a small tie breaker', () => {
    const recent = repo('roman-skuggorna', { lastUsedAt: new Date().toISOString() });
    const other = repo('skuggorna-bok');
    markRepositoryRecent(recent);
    const result = suggestRepository('skuggorna-v2.0.zip', [other, recent]);
    expect(result?.repository).toBe(recent);
  });
});
