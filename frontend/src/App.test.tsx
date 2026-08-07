import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from './App';

const authenticatedUser = { id: 'user-1', githubUserId: 123, login: 'erland', avatarUrl: null };
const project = {
  id: 'project-1', name: 'Bokprojekt', githubInstallationId: 10, githubRepositoryId: 20,
  repositoryFullName: 'erland/example-book-project', privateRepository: true, defaultBranch: 'main',
  active: true, createdAt: '2026-08-06T20:00:00Z', updatedAt: '2026-08-06T20:00:00Z',
};

function json(value: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(value), { status, headers: { 'Content-Type': 'application/json' } }));
}

function renderAt(route: string) {
  return render(<MemoryRouter initialEntries={[route]}><App /></MemoryRouter>);
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('App routing and authentication', () => {
  it('shows GitHub login when no session exists', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation((input: RequestInfo | URL) => {
      if (String(input) === '/api/auth/me') return json({ title: 'Unauthorized' }, 401);
      return Promise.reject(new Error(`Unexpected fetch: ${String(input)}`));
    }));
    renderAt('/projects');
    expect(await screen.findByRole('heading', { name: 'Logga in för att fortsätta' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Logga in med GitHub' })).toHaveAttribute('href', '/api/auth/github/login?returnTo=%2Fprojects');
  });

  it('loads the real project list for an authenticated user', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/auth/me') return json(authenticatedUser);
      if (url === '/api/projects') return json([project]);
      return Promise.reject(new Error(`Unexpected fetch: ${url}`));
    }));
    renderAt('/');
    expect(await screen.findByRole('heading', { name: 'Dina projekt' })).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Bokprojekt' })).toBeInTheDocument();
    expect(screen.queryByText(/tillfälliga exempeldata/i)).not.toBeInTheDocument();
  });

  it('navigates from the real project list to project details and a new import', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/auth/me') return json(authenticatedUser);
      if (url === '/api/projects') return json([project]);
      if (url === '/api/projects/project-1') return json(project);
      if (url === '/api/projects/project-1/imports') return json([]);
      return Promise.reject(new Error(`Unexpected fetch: ${url}`));
    }));
    renderAt('/projects');

    await user.click(await screen.findByRole('link', { name: 'Öppna projekt' }));
    const projectHeading = await screen.findByRole('heading', { name: 'Bokprojekt' });
    expect(projectHeading).toHaveFocus();

    await user.click(screen.getByRole('link', { name: 'Ny import' }));
    expect(screen.getByRole('heading', { name: 'Ladda upp projekt-ZIP' })).toBeInTheDocument();
  });

  it('routes to the stored import result page', async () => {
    const result = { importId: 'i1', repositoryFullName: 'erland/example', baseBranch: 'main',
      branchName: 'zip-github/import-i1', commitSha: 'a'.repeat(40), planDigestSha256: 'b'.repeat(64),
      pullRequestNumber: 7, pullRequestUrl: 'https://github.com/erland/example/pull/7', draft: true,
      state: 'open', status: 'PULL_REQUEST_CREATED', createdAt: '2026-08-06T20:00:00Z' };
    const checks = { importId: 'i1', commitSha: 'a'.repeat(40), state: 'success', terminal: true,
      total: 1, pending: 0, successful: 1, failed: 0, cancelled: 0,
      detailsUrl: 'https://github.com/erland/example/commit/a/checks', checkedAt: '2026-08-07T05:00:00Z' };
    vi.stubGlobal('fetch', vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/auth/me') return json(authenticatedUser);
      if (url.endsWith('/pull-request')) return json(result);
      if (url.endsWith('/checks')) return json(checks);
      return Promise.reject(new Error(`Unexpected fetch: ${url}`));
    }));
    renderAt('/projects/p1/imports/i1/result');
    expect(await screen.findByRole('heading', { name: 'Pull request skapad' })).toBeInTheDocument();
  });

  it('shows a not-found page for unknown authenticated routes', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation((input: RequestInfo | URL) => {
      if (String(input) === '/api/auth/me') return json(authenticatedUser);
      return Promise.reject(new Error(`Unexpected fetch: ${String(input)}`));
    }));
    renderAt('/saknas');
    expect(await screen.findByRole('heading', { name: 'Sidan hittades inte' })).toBeInTheDocument();
  });
});
