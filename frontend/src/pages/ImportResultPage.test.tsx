import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, expect, it, vi } from 'vitest';
import ImportResultPage from './ImportResultPage';

const result = { importId: 'import-1', repositoryFullName: 'erland/example', baseBranch: 'main', branchName: 'zip-github/work-w1',
  baseCommitSha: 'c'.repeat(40), commitSha: 'a'.repeat(40), planDigestSha256: 'b'.repeat(64), status: 'PUSHED', pushedAt: '2026-08-06T20:00:00Z' };
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
it('shows the committed work branch and next-import action', async () => {
  const checks = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: 'a'.repeat(40), state: 'success', terminal: true,
    total: 2, pending: 0, successful: 2, failed: 0, cancelled: 0, detailsUrl: 'x', checkedAt: '2026-08-06T20:01:00Z' };
  vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce({ ok: true, json: async () => result }).mockResolvedValueOnce({ ok: true, json: async () => checks }));
  render(<MemoryRouter initialEntries={['/projects/p1/imports/import-1/result']}><Routes><Route path="projects/:projectId/imports/:importId/result" element={<ImportResultPage />} /></Routes></MemoryRouter>);
  expect(await screen.findByRole('heading', { name: 'Commit skapad' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Ladda upp nästa ZIP' })).toHaveAttribute('href', '/projects/p1/imports/new');
  expect(screen.getByRole('link', { name: result.branchName })).toHaveAttribute('href', expect.stringContaining('/tree/'));
});
