import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from './App';

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

function renderAt(route: string) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <App />
    </MemoryRouter>,
  );
}

describe('App routing', () => {
  it('redirects the root route to the project list', async () => {
    renderAt('/');
    const heading = await screen.findByRole('heading', { name: 'Dina projekt' });
    expect(heading).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Hoppa till huvudinnehållet' })).toHaveAttribute('href', '#main-content');
  });

  it('navigates from the project list to project details and a new import', async () => {
    const user = userEvent.setup();
    const project = {
      id: 'demo-book-project', name: 'Bokprojekt', githubInstallationId: 10, githubRepositoryId: 20,
      repositoryFullName: 'erland/example-book-project', privateRepository: true, defaultBranch: 'main',
      active: true, createdAt: '2026-08-06T20:00:00Z', updatedAt: '2026-08-06T20:00:00Z',
    };
    vi.stubGlobal('fetch', vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/projects/demo-book-project') {
        return Promise.resolve({ ok: true, json: async () => project });
      }
      if (url === '/api/projects/demo-book-project/imports') {
        return Promise.resolve({ ok: true, json: async () => [] });
      }
      return Promise.reject(new Error(`Unexpected fetch: ${url}`));
    }));
    renderAt('/projects');

    await user.click(screen.getByRole('link', { name: 'Öppna projekt' }));
    const projectHeading = await screen.findByRole('heading', { name: 'Bokprojekt' });
    expect(projectHeading).toBeInTheDocument();
    expect(projectHeading).toHaveFocus();

    await user.click(screen.getByRole('link', { name: 'Ny import' }));
    expect(screen.getByRole('heading', { name: 'Ladda upp projekt-ZIP' })).toBeInTheDocument();
  });

  it('routes to the stored import result page', async () => {
    const result = { importId: 'i1', repositoryFullName: 'erland/example', baseBranch: 'main',
      branchName: 'zip-github/import-i1', commitSha: 'a'.repeat(40), planDigestSha256: 'b'.repeat(64),
      pullRequestNumber: 7, pullRequestUrl: 'https://github.com/erland/example/pull/7', draft: true,
      state: 'open', status: 'PULL_REQUEST_CREATED', createdAt: '2026-08-06T20:00:00Z' };
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => result }));
    renderAt('/projects/p1/imports/i1/result');
    expect(await screen.findByRole('heading', { name: 'Pull request skapad' })).toBeInTheDocument();
  });

  it('shows a not-found page for unknown routes', () => {
    renderAt('/saknas');
    expect(screen.getByRole('heading', { name: 'Sidan hittades inte' })).toBeInTheDocument();
  });
});
