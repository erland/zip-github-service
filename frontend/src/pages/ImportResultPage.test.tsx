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
  const actions = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: 'a'.repeat(40), state: 'success', terminal: true,
    detailsUrl: 'https://github.com/erland/example/commit/x/checks', checkedAt: '2026-08-06T20:01:00Z',
    workflows: [{ id: 10, name: 'CI', state: 'success', terminal: true, event: 'push', htmlUrl: 'https://github.com/erland/example/actions/runs/10', createdAt: '2026-08-06T20:00:10Z', updatedAt: '2026-08-06T20:01:00Z', jobs: [{ id: 11, name: 'backend', state: 'success', terminal: true, htmlUrl: 'https://github.com/erland/example/actions/runs/10/job/11', startedAt: '2026-08-06T20:00:20Z', completedAt: '2026-08-06T20:00:50Z' }] }],
    checks: [{ id: 12, name: 'frontend', state: 'success', terminal: true, htmlUrl: 'https://github.com/erland/example/runs/12', appName: 'GitHub Actions', startedAt: '2026-08-06T20:00:20Z', completedAt: '2026-08-06T20:00:55Z' }] };
  const actionDetails = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: 'a'.repeat(40), detailsUrl: actions.detailsUrl, checkedAt: '2026-08-06T20:01:01Z',
    artifacts: [{ id: 100, name: 'frontend-dist', sizeBytes: 2048, expired: false, createdAt: '2026-08-06T20:01:00Z', expiresAt: '2026-08-07T20:01:00Z', workflowRunId: 10, workflowName: 'CI', githubUrl: 'https://github.com/erland/example/actions/runs/10' }], failures: [] };
  const pullRequest = { importId: 'import-1', repositoryFullName: 'erland/example', baseBranch: 'main', branchName: 'zip-github/work-w1',
    commitSha: 'a'.repeat(40), planDigestSha256: 'b'.repeat(64), pullRequestNumber: 42, pullRequestUrl: 'https://github.com/erland/example/pull/42',
    draft: true, state: 'open', status: 'PULL_REQUEST_CREATED', createdAt: '2026-08-06T20:02:00Z' };
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith('/api/imports/import-1/delivery')) return new Response(JSON.stringify(result), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/checks')) return new Response(JSON.stringify(checks), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions')) return new Response(JSON.stringify(actions), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions/details')) return new Response(JSON.stringify(actionDetails), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/projects/p1/work/pull-request') && init?.method === 'POST') return new Response(JSON.stringify(pullRequest), { status: 200, headers: { 'Content-Type': 'application/json' } });
    throw new Error(`Unexpected fetch: ${url}`);
  });
  vi.stubGlobal('fetch', fetchMock);
  render(<MemoryRouter initialEntries={['/projects/p1/imports/import-1/result']}><Routes><Route path="projects/:projectId/imports/:importId/result" element={<ImportResultPage />} /></Routes></MemoryRouter>);
  expect(await screen.findByRole('heading', { name: 'Commit skapad' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Ladda upp nästa ZIP' })).toHaveAttribute('href', '/projects/p1/imports/new');
  expect(screen.getByRole('button', { name: 'Arbetet är klart – skapa pull request' })).toBeEnabled();
  expect(screen.getByRole('link', { name: result.branchName })).toHaveAttribute('href', expect.stringContaining('/tree/'));
  expect(await screen.findByText('CI')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'backend' })).toHaveAttribute('href', expect.stringContaining('/job/11'));
  expect(screen.getByRole('link', { name: 'frontend' })).toHaveAttribute('href', expect.stringContaining('/runs/12'));
  expect(await screen.findByText('frontend-dist')).toBeInTheDocument();
  expect(screen.getByText('2.0 kB · CI')).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'Arbetet är klart – skapa pull request' }));
  expect(await screen.findByRole('link', { name: 'Öppna pull request' })).toHaveAttribute('href', pullRequest.pullRequestUrl);
  expect(screen.getByRole('button', { name: 'Pull request skapad' })).toBeDisabled();
  expect(fetchMock).toHaveBeenCalledWith('/api/projects/p1/work/pull-request', expect.objectContaining({ method: 'POST' }));
});



it('retries direct finish-work after a transient failure without creating a second UI action', async () => {
  const user = userEvent.setup();
  const checks = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: 'a'.repeat(40), state: 'success', terminal: true,
    total: 1, pending: 0, successful: 1, failed: 0, cancelled: 0, detailsUrl: 'x', checkedAt: '2026-08-06T20:01:00Z' };
  const actions = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: 'a'.repeat(40), state: 'success', terminal: true,
    detailsUrl: 'https://github.com/erland/example/commit/x/checks', checkedAt: '2026-08-06T20:01:00Z',
    workflows: [{ id: 10, name: 'CI', state: 'success', terminal: true, event: 'push', htmlUrl: 'https://github.com/erland/example/actions/runs/10', createdAt: '2026-08-06T20:00:10Z', updatedAt: '2026-08-06T20:01:00Z', jobs: [{ id: 11, name: 'backend', state: 'success', terminal: true, htmlUrl: 'https://github.com/erland/example/actions/runs/10/job/11', startedAt: '2026-08-06T20:00:20Z', completedAt: '2026-08-06T20:00:50Z' }] }],
    checks: [{ id: 12, name: 'frontend', state: 'success', terminal: true, htmlUrl: 'https://github.com/erland/example/runs/12', appName: 'GitHub Actions', startedAt: '2026-08-06T20:00:20Z', completedAt: '2026-08-06T20:00:55Z' }] };
  const pullRequest = { importId: 'import-1', repositoryFullName: 'erland/example', baseBranch: 'main', branchName: 'zip-github/work-w1',
    commitSha: 'a'.repeat(40), planDigestSha256: 'b'.repeat(64), pullRequestNumber: 42, pullRequestUrl: 'https://github.com/erland/example/pull/42',
    draft: true, state: 'open', status: 'PULL_REQUEST_CREATED', createdAt: '2026-08-06T20:02:00Z' };
  let prAttempts = 0;
  vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith('/api/imports/import-1/delivery')) return new Response(JSON.stringify(result), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/checks')) return new Response(JSON.stringify(checks), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions')) return new Response(JSON.stringify(actions), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions/details')) return new Response(JSON.stringify({ importId: 'import-1', repositoryFullName: 'erland/example', commitSha: 'a'.repeat(40), detailsUrl: actions.detailsUrl, checkedAt: '2026-08-06T20:01:01Z', artifacts: [], failures: [] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
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


it('shows a bounded condensed failure with workflow job step and GitHub source', async () => {
  const actions = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: 'a'.repeat(40), state: 'failure', terminal: true,
    detailsUrl: 'https://github.com/erland/example/commit/x/checks', checkedAt: '2026-08-06T20:01:00Z',
    workflows: [{ id: 10, name: 'CI', state: 'failure', terminal: true, event: 'push', htmlUrl: 'https://github.com/erland/example/actions/runs/10', createdAt: null, updatedAt: null,
      jobs: [{ id: 11, name: 'backend', state: 'failure', terminal: true, htmlUrl: 'https://github.com/erland/example/actions/runs/10/job/11', startedAt: null, completedAt: null }] }], checks: [] };
  const details = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: 'a'.repeat(40), detailsUrl: actions.detailsUrl, checkedAt: '2026-08-06T20:01:01Z', artifacts: [],
    failures: [{ workflowRunId: 10, workflowName: 'CI', jobId: 11, jobName: 'backend', stepName: 'Build backend', tool: 'Maven/Gradle', lines: ['[ERROR] Failed to execute goal compiler'], githubUrl: 'https://github.com/erland/example/actions/runs/10/job/11' }] };
  vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url.endsWith('/api/imports/import-1/delivery')) return new Response(JSON.stringify(result), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions')) return new Response(JSON.stringify(actions), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions/details')) return new Response(JSON.stringify(details), { status: 200, headers: { 'Content-Type': 'application/json' } });
    throw new Error(`Unexpected fetch: ${url}`);
  }));
  render(<MemoryRouter initialEntries={['/projects/p1/imports/import-1/result']}><Routes><Route path="projects/:projectId/imports/:importId/result" element={<ImportResultPage />} /></Routes></MemoryRouter>);
  expect(await screen.findByRole('heading', { name: 'Kondenserade fel' })).toBeInTheDocument();
  expect(screen.getByText('CI / backend')).toBeInTheDocument();
  expect(screen.getByText('Build backend · Maven/Gradle')).toBeInTheDocument();
  expect(screen.getByText('[ERROR] Failed to execute goal compiler')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Öppna jobb på GitHub' })).toHaveAttribute('href', expect.stringContaining('/job/11'));
});

it('dispatches and reruns only server-allowed workflows for the displayed Work head', async () => {
  const user = userEvent.setup();
  const actions = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: 'a'.repeat(40), state: 'failure', terminal: true,
    detailsUrl: 'https://github.com/erland/example/commit/x/checks', checkedAt: '2026-08-07T20:01:00Z',
    workflows: [{ id: 10, workflowId: 77, workflowPath: '.github/workflows/ci.yml', headBranch: result.branchName, headSha: result.commitSha,
      name: 'CI', state: 'failure', terminal: true, event: 'push', htmlUrl: 'https://github.com/erland/example/actions/runs/10', createdAt: null, updatedAt: null, jobs: [] }], checks: [] };
  const options = { importId: 'import-1', repositoryFullName: 'erland/example', branchRef: result.branchName, commitSha: result.commitSha,
    currentWork: true, disabledReason: null, workflows: [{ identifier: 'ci.yml', workflowId: 77, name: 'CI', path: '.github/workflows/ci.yml',
      htmlUrl: 'https://github.com/erland/example/actions/workflows/ci.yml', dispatchAllowed: true, rerunAllowed: true }] };
  const details = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: result.commitSha, detailsUrl: actions.detailsUrl,
    checkedAt: '2026-08-07T20:01:01Z', artifacts: [], failures: [] };
  const calls: Array<{url:string; body:unknown}> = [];
  vi.stubGlobal('crypto', { randomUUID: () => '12345678-abcd-4abc-8abc-123456789012' });
  vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith('/api/imports/import-1/delivery')) return new Response(JSON.stringify(result), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions/control')) return new Response(JSON.stringify(options), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions/details')) return new Response(JSON.stringify(details), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions') && !init?.method) return new Response(JSON.stringify(actions), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions/dispatch') && init?.method === 'POST') {
      calls.push({ url, body: JSON.parse(String(init.body)) });
      return new Response(JSON.stringify({ operationId: 'op-1', operation: 'WORKFLOW_DISPATCH', status: 'SUCCEEDED', replayed: false,
        workflowIdentifier: 'ci.yml', workflowId: 77, workflowRunId: 11, branchRef: result.branchName, targetCommitSha: result.commitSha,
        githubUrl: 'https://github.com/erland/example/actions/runs/11', errorCode: null, createdAt: '2026-08-07T20:02:00Z', updatedAt: '2026-08-07T20:02:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    if (url.endsWith('/api/imports/import-1/actions/rerun-failed') && init?.method === 'POST') {
      calls.push({ url, body: JSON.parse(String(init.body)) });
      return new Response(JSON.stringify({ operationId: 'op-2', operation: 'RERUN_FAILED_JOBS', status: 'SUCCEEDED', replayed: false,
        workflowIdentifier: '.github/workflows/ci.yml', workflowId: 77, workflowRunId: 10, branchRef: result.branchName, targetCommitSha: result.commitSha,
        githubUrl: 'https://github.com/erland/example/actions/runs/10', errorCode: null, createdAt: '2026-08-07T20:03:00Z', updatedAt: '2026-08-07T20:03:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    throw new Error(`Unexpected fetch: ${url}`);
  }));

  render(<MemoryRouter initialEntries={['/projects/p1/imports/import-1/result']}><Routes><Route path="projects/:projectId/imports/:importId/result" element={<ImportResultPage />} /></Routes></MemoryRouter>);
  expect(await screen.findByRole('heading', { name: 'Kontrollerade Actions' })).toBeInTheDocument();
  expect(screen.getByText(/zip-github\/work-w1/)).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Kör workflow' }));
  expect(await screen.findByText(/CI startades/)).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Kör om misslyckade jobb' }));
  expect(await screen.findByText(/Misslyckade jobb i CI köas om/)).toBeInTheDocument();
  expect(calls).toHaveLength(2);
  expect(calls[0].body).toMatchObject({ workflowIdentifier: 'ci.yml', expectedRef: result.branchName, expectedCommitSha: result.commitSha, confirmed: true });
  expect(calls[1].body).toMatchObject({ workflowRunId: 10, expectedRef: result.branchName, expectedCommitSha: result.commitSha, confirmed: true });
});

it('keeps the same idempotency key after an ambiguous GitHub dispatch error', async () => {
  const user = userEvent.setup();
  const actions = { importId: 'import-1', repositoryFullName: 'erland/example', commitSha: result.commitSha, state: 'not_started', terminal: false,
    detailsUrl: 'https://github.com/erland/example/commit/x/checks', checkedAt: '2026-08-07T20:01:00Z', workflows: [], checks: [] };
  const options = { importId: 'import-1', repositoryFullName: 'erland/example', branchRef: result.branchName, commitSha: result.commitSha,
    currentWork: true, disabledReason: null, workflows: [{ identifier: 'ci.yml', workflowId: 77, name: 'CI', path: '.github/workflows/ci.yml',
      htmlUrl: 'https://github.com/erland/example/actions/workflows/ci.yml', dispatchAllowed: true, rerunAllowed: false }] };
  const keys: string[] = [];
  vi.stubGlobal('crypto', { randomUUID: () => '12345678-abcd-4abc-8abc-123456789012' });
  vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith('/api/imports/import-1/delivery')) return new Response(JSON.stringify(result), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions/control')) return new Response(JSON.stringify(options), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions') && !init?.method) return new Response(JSON.stringify(actions), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-1/actions/dispatch') && init?.method === 'POST') {
      keys.push(JSON.parse(String(init.body)).idempotencyKey);
      return new Response(JSON.stringify({ detail: 'GitHub could not dispatch the workflow.' }), { status: 502, headers: { 'Content-Type': 'application/json' } });
    }
    throw new Error(`Unexpected fetch: ${url}`);
  }));

  render(<MemoryRouter initialEntries={['/projects/p1/imports/import-1/result']}><Routes><Route path="projects/:projectId/imports/:importId/result" element={<ImportResultPage />} /></Routes></MemoryRouter>);
  const button = await screen.findByRole('button', { name: 'Kör workflow' });
  await user.click(button);
  expect(await screen.findByRole('alert')).toHaveTextContent('GitHub could not dispatch the workflow.');
  await user.click(screen.getByRole('button', { name: 'Kör workflow' }));
  expect(keys).toEqual(['12345678-abcd-4abc-8abc-123456789012', '12345678-abcd-4abc-8abc-123456789012']);
});
