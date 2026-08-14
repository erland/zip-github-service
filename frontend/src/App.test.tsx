import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import { resetSessionExpiredForTests } from './api/session';

const authenticatedUser = { id: 'user-1', githubUserId: 123, login: 'erland', avatarUrl: null, gitName: 'Erland', gitEmail: '123+erland@users.noreply.github.com' };
const project = {
  id: 'project-1', name: 'example-book-project', githubInstallationId: 10, githubRepositoryId: 20,
  repositoryFullName: 'erland/example-book-project', privateRepository: true, defaultBranch: 'main',
  active: true, createdAt: '2026-08-06T20:00:00Z', updatedAt: '2026-08-06T20:00:00Z',
};
const repository = { githubInstallationId: 10, githubRepositoryId: 20, repositoryFullName: 'erland/example-book-project', repositoryName: 'example-book-project', privateRepository: true, defaultBranch: 'main', htmlUrl: 'https://github.com/erland/example-book-project', projectId: 'project-1' };

function json(value: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(value), { status, headers: { 'Content-Type': 'application/json' } }));
}

function renderAt(route: string) {
  return render(<MemoryRouter initialEntries={[route]}><App /></MemoryRouter>);
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  resetSessionExpiredForTests();
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

  it('returns to login on API 401 after an authenticated session expires and preserves the current route', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/auth/me') return json(authenticatedUser);
      if (url === '/api/repositories') return json({ title: 'Unauthorized' }, 401);
      return Promise.reject(new Error(`Unexpected fetch: ${url}`));
    }));

    renderAt('/projects');

    expect(await screen.findByRole('heading', { name: 'Logga in för att fortsätta' })).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Din session har gått ut. Logga in igen för att fortsätta där du var.');
    expect(screen.getByRole('link', { name: 'Logga in med GitHub' })).toHaveAttribute('href', '/api/auth/github/login?returnTo=%2Fprojects');
    expect(screen.queryByText('API-fel 401')).not.toBeInTheDocument();
  });

  it('lists GitHub App repositories and filters them by partial name', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/auth/me') return json(authenticatedUser);
      if (url === '/api/repositories') return json([repository, { ...repository, githubRepositoryId: 21, repositoryFullName: 'erland/other-repo', repositoryName: 'other-repo', projectId: null }]);
      return Promise.reject(new Error(`Unexpected fetch: ${url}`));
    }));
    renderAt('/');
    expect(await screen.findByRole('heading', { name: 'Repositories' })).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: /example-book-project/ })).toBeInTheDocument();
    await user.type(screen.getByRole('searchbox', { name: 'Sök repositories' }), 'other');
    expect(screen.getByRole('link', { name: 'other-repo' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /example-book-project/ })).not.toBeInTheDocument();
  });

  it('creates the internal project lazily when work starts for a new repository', async () => {
    const user = userEvent.setup();
    let created = false;
    const newRepository = { ...repository, projectId: null };
    const work = { id: 'work-1', projectId: 'project-1', baseBranch: 'main', branchName: 'zip-github/work-1', status: 'ACTIVE', headCommitSha: null, lastImportId: null, pullRequestNumber: null, pullRequestUrl: null, createdAt: '2026-08-08T15:00:00Z', updatedAt: '2026-08-08T15:00:00Z' };
    vi.stubGlobal('fetch', vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === '/api/auth/me') return json(authenticatedUser);
      if (url === '/api/repositories') return json([created ? { ...newRepository, projectId: 'project-1' } : newRepository]);
      if (url === '/api/repositories/10/20/work' && init?.method === 'POST') { created = true; return json({ project, work }); }
      if (url === '/api/projects/project-1') return json(project);
      if (url === '/api/projects/project-1/imports') return json([]);
      if (url === '/api/projects/project-1/work') return json(work);
      if (url === '/api/projects/project-1/work/commits') return json({ githubAvailable: true, commits: [] });
      return Promise.reject(new Error(`Unexpected fetch: ${url}`));
    }));
    renderAt('/projects');
    await user.click(await screen.findByRole('link', { name: 'example-book-project' }));
    expect(await screen.findByRole('heading', { name: 'example-book-project' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Starta arbete' }));
    expect(await screen.findByText('zip-github/work-1')).toBeInTheDocument();
  });

  it('routes to the stored import result page', async () => {
    const result = { importId: 'i1', repositoryFullName: 'erland/example', baseBranch: 'main',
      branchName: 'zip-github/work-w1', baseCommitSha: 'c'.repeat(40), commitSha: 'a'.repeat(40), planDigestSha256: 'b'.repeat(64),
      status: 'PUSHED', pushedAt: '2026-08-06T20:00:00Z' };
    const checks = { importId: 'i1', commitSha: 'a'.repeat(40), state: 'success', terminal: true,
      total: 1, pending: 0, successful: 1, failed: 0, cancelled: 0,
      detailsUrl: 'https://github.com/erland/example/commit/a/checks', checkedAt: '2026-08-07T05:00:00Z' };
    vi.stubGlobal('fetch', vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/auth/me') return json(authenticatedUser);
      if (url.endsWith('/delivery')) return json(result);
      if (url.endsWith('/checks')) return json(checks);
      return Promise.reject(new Error(`Unexpected fetch: ${url}`));
    }));
    renderAt('/projects/p1/imports/i1/result');
    expect(await screen.findByRole('heading', { name: 'Commit skapad' })).toBeInTheDocument();
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
