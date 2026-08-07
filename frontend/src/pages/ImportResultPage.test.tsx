import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, expect, it, vi } from 'vitest';
import ImportResultPage from './ImportResultPage';

const result = { importId: 'import-1', repositoryFullName: 'erland/example', baseBranch: 'main', branchName: 'zip-github/work-w1',
  baseCommitSha: 'c'.repeat(40), commitSha: 'a'.repeat(40), planDigestSha256: 'b'.repeat(64), status: 'PUSHED', pushedAt: '2026-08-06T20:00:00Z' };
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
it('shows both next-ZIP and direct finish-work actions after commit', async () => {
  const user = userEvent.setup();
  const checks = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: 'a'.repeat(40), state: 'success', terminal: true,
    total: 2, pending: 0, successful: 2, failed: 0, cancelled: 0, detailsUrl: 'x', checkedAt: '2026-08-06T20:01:00Z' };
  const pullRequest = { importId: 'import-1', repositoryFullName: 'erland/example', baseBranch: 'main', branchName: 'zip-github/work-w1',
    commitSha: 'a'.repeat(40), planDigestSha256: 'b'.repeat(64), pullRequestNumber: 42, pullRequestUrl: 'https://github.com/erland/example/pull/42',
    draft: true, state: 'open', status: 'PULL_REQUEST_CREATED', createdAt: '2026-08-06T20:02:00Z' };
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith('/api/imports/import-1/delivery')) return new Response(JSON.stringify(result), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/checks')) return new Response(JSON.stringify(checks), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/projects/p1/work/pull-request') && init?.method === 'POST') return new Response(JSON.stringify(pullRequest), { status: 200, headers: { 'Content-Type': 'application/json' } });
    throw new Error(`Unexpected fetch: ${url}`);
  });
  vi.stubGlobal('fetch', fetchMock);
  render(<MemoryRouter initialEntries={['/projects/p1/imports/import-1/result']}><Routes><Route path="projects/:projectId/imports/:importId/result" element={<ImportResultPage />} /></Routes></MemoryRouter>);
  expect(await screen.findByRole('heading', { name: 'Commit skapad' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Ladda upp nästa ZIP' })).toHaveAttribute('href', '/projects/p1/imports/new');
  expect(screen.getByRole('button', { name: 'Arbetet är klart – skapa pull request' })).toBeEnabled();
  expect(screen.getByRole('link', { name: result.branchName })).toHaveAttribute('href', expect.stringContaining('/tree/'));

  await user.click(screen.getByRole('button', { name: 'Arbetet är klart – skapa pull request' }));
  expect(await screen.findByRole('link', { name: 'Öppna pull request' })).toHaveAttribute('href', pullRequest.pullRequestUrl);
  expect(screen.getByRole('button', { name: 'Pull request skapad' })).toBeDisabled();
  expect(fetchMock).toHaveBeenCalledWith('/api/projects/p1/work/pull-request', expect.objectContaining({ method: 'POST' }));
});



it('retries direct finish-work after a transient failure without creating a second UI action', async () => {
  const user = userEvent.setup();
  const checks = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: 'a'.repeat(40), state: 'success', terminal: true,
    total: 1, pending: 0, successful: 1, failed: 0, cancelled: 0, detailsUrl: 'x', checkedAt: '2026-08-06T20:01:00Z' };
  const pullRequest = { importId: 'import-1', repositoryFullName: 'erland/example', baseBranch: 'main', branchName: 'zip-github/work-w1',
    commitSha: 'a'.repeat(40), planDigestSha256: 'b'.repeat(64), pullRequestNumber: 42, pullRequestUrl: 'https://github.com/erland/example/pull/42',
    draft: true, state: 'open', status: 'PULL_REQUEST_CREATED', createdAt: '2026-08-06T20:02:00Z' };
  let prAttempts = 0;
  vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith('/api/imports/import-1/delivery')) return new Response(JSON.stringify(result), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/checks')) return new Response(JSON.stringify(checks), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/projects/p1/work/pull-request') && init?.method === 'POST') {
      prAttempts += 1;
      if (prAttempts === 1) return new Response(JSON.stringify({ detail: 'GitHub svarade inte i tid.' }), { status: 502, headers: { 'Content-Type': 'application/json' } });
      return new Response(JSON.stringify(pullRequest), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    throw new Error(`Unexpected fetch: ${url}`);
  }));

  render(<MemoryRouter initialEntries={['/projects/p1/imports/import-1/result']}><Routes><Route path="projects/:projectId/imports/:importId/result" element={<ImportResultPage />} /></Routes></MemoryRouter>);
  const finish = await screen.findByRole('button', { name: 'Arbetet är klart – skapa pull request' });
  await user.click(finish);
  expect(await screen.findByRole('alert')).toHaveTextContent('GitHub svarade inte i tid.');
  expect(screen.getAllByRole('button', { name: 'Arbetet är klart – skapa pull request' })).toHaveLength(1);

  await user.click(screen.getByRole('button', { name: 'Arbetet är klart – skapa pull request' }));
  expect(await screen.findByRole('link', { name: 'Öppna pull request' })).toHaveAttribute('href', pullRequest.pullRequestUrl);
  expect(prAttempts).toBe(2);
  expect(screen.getByRole('button', { name: 'Pull request skapad' })).toBeDisabled();
});
