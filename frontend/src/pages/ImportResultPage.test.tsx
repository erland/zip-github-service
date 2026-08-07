import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ImportResultPage from './ImportResultPage';

const result = {
  importId: 'import-1', repositoryFullName: 'erland/example', baseBranch: 'main',
  branchName: 'zip-github/import-import-1', commitSha: 'a'.repeat(40), planDigestSha256: 'b'.repeat(64),
  pullRequestNumber: 42, pullRequestUrl: 'https://github.com/erland/example/pull/42', draft: true,
  state: 'open', status: 'PULL_REQUEST_CREATED', createdAt: '2026-08-06T20:00:00Z',
};

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

it('shows persistent links from stored pull request metadata', async () => {
  const checks = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: 'a'.repeat(40),
    state: 'success', terminal: true, total: 2, pending: 0, successful: 2, failed: 0, cancelled: 0,
    detailsUrl: 'https://github.com/erland/example/commit/' + 'a'.repeat(40) + '/checks', checkedAt: '2026-08-06T20:01:00Z' };
  vi.stubGlobal('fetch', vi.fn()
    .mockResolvedValueOnce({ ok: true, json: async () => result })
    .mockResolvedValueOnce({ ok: true, json: async () => checks }));
  render(<MemoryRouter initialEntries={['/projects/p1/imports/import-1/result']}><Routes>
    <Route path="projects/:projectId/imports/:importId/result" element={<ImportResultPage />} />
  </Routes></MemoryRouter>);

  expect(await screen.findByRole('heading', { name: 'Pull request skapad' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Öppna pull request' })).toHaveAttribute('href', result.pullRequestUrl);
  expect(screen.getByRole('link', { name: result.branchName })).toHaveAttribute('href', expect.stringContaining('/tree/'));
  expect(await screen.findByRole('heading', { name: 'Alla kontroller lyckades' })).toBeInTheDocument();
  expect(screen.getAllByRole('link', { name: 'Öppna checks' })[0]).toHaveAttribute('href', expect.stringContaining('/checks'));
  expect(screen.getByText(/högst tolv gånger/)).toBeInTheDocument();
});
