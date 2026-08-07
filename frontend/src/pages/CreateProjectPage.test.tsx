import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import CreateProjectPage from './CreateProjectPage';

function json(value: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(value), { status, headers: { 'Content-Type': 'application/json' } }));
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('CreateProjectPage', () => {
  it('loads installations and repositories and creates a verified project', async () => {
    const user = userEvent.setup();
    const installations = [{ id: 10, accountId: 1, accountLogin: 'erland', accountType: 'User', repositorySelection: 'selected', htmlUrl: 'https://github.com/settings/installations/10' }];
    const repositories = [{ id: 20, fullName: 'erland/novel', privateRepository: true, defaultBranch: 'main', htmlUrl: 'https://github.com/erland/novel' }];
    const created = { id: 'project-1', name: 'novel', githubInstallationId: 10, githubRepositoryId: 20, repositoryFullName: 'erland/novel', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-07T06:00:00Z', updatedAt: '2026-08-07T06:00:00Z' };
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === '/api/github/installations') return json(installations);
      if (url === '/api/github/installations/10/repositories') return json(repositories);
      if (url === '/api/projects' && init?.method === 'POST') return json(created, 201);
      return Promise.reject(new Error(`Unexpected fetch: ${url}`));
    });
    vi.stubGlobal('fetch', fetchMock);

    render(
      <MemoryRouter initialEntries={['/projects/new']}>
        <Routes>
          <Route path="/projects/new" element={<CreateProjectPage />} />
          <Route path="/projects/:projectId" element={<h1>Projekt skapat</h1>} />
        </Routes>
      </MemoryRouter>,
    );

    await screen.findByRole('option', { name: 'erland (User)' });
    expect(await screen.findByRole('option', { name: 'erland/novel (privat)' })).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Repository'), '20');
    expect(screen.getByLabelText('Projektnamn')).toHaveValue('novel');
    expect(screen.getByLabelText('Standardbranch')).toHaveValue('main');

    await user.click(screen.getByRole('button', { name: 'Skapa projekt' }));
    expect(await screen.findByRole('heading', { name: 'Projekt skapat' })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith('/api/projects', expect.objectContaining({
      method: 'POST',
      credentials: 'include',
      headers: expect.objectContaining({ 'X-Zip-GitHub-Request': '1' }),
    }));
  });
});
