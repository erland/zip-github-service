import { afterEach, describe, expect, it, vi } from 'vitest';

import { promoteStagingImport } from './staging';

describe('promoteStagingImport', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('serializes an existing project target directly', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      stagingId: 'staging-1', projectId: 'project-1', importId: 'import-1', status: 'PROMOTED', alreadyPromoted: false,
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }));
    vi.stubGlobal('fetch', fetchMock);

    await promoteStagingImport('staging-1', { projectId: 'project-1' });

    expect(JSON.parse(String(fetchMock.mock.calls[0][1]?.body))).toEqual({ projectId: 'project-1', confirmOpenPullRequest: false });
  });

  it('serializes a repository target directly for lazy project creation', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      stagingId: 'staging-1', projectId: 'project-2', importId: 'import-1', status: 'PROMOTED', alreadyPromoted: false,
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }));
    vi.stubGlobal('fetch', fetchMock);

    await promoteStagingImport('staging-1', { githubInstallationId: 11, githubRepositoryId: 21 });

    expect(JSON.parse(String(fetchMock.mock.calls[0][1]?.body))).toEqual({ githubInstallationId: 11, githubRepositoryId: 21, confirmOpenPullRequest: false });
  });
});
