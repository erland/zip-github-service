import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import RepositoryPicker from './RepositoryPicker';
import { markRepositoryRecent, repositoryKey } from '../repositories/recentRepositories';

const repositories = [
  { githubInstallationId: 10, githubRepositoryId: 20, repositoryFullName: 'erland/alpha', repositoryName: 'alpha', privateRepository: true, defaultBranch: 'main', htmlUrl: 'https://github.com/erland/alpha', projectId: 'project-1' },
  { githubInstallationId: 11, githubRepositoryId: 21, repositoryFullName: 'erland/beta', repositoryName: 'beta', privateRepository: true, defaultBranch: 'main', htmlUrl: 'https://github.com/erland/beta', projectId: null },
  { githubInstallationId: 12, githubRepositoryId: 22, repositoryFullName: 'other/gamma', repositoryName: 'gamma', privateRepository: false, defaultBranch: 'main', htmlUrl: 'https://github.com/other/gamma', projectId: null },
];

beforeEach(() => localStorage.clear());
afterEach(() => cleanup());

it('filters repositories by name or full name in selection mode', async () => {
  const user = userEvent.setup();
  render(<MemoryRouter><RepositoryPicker repositories={repositories} mode="select" /></MemoryRouter>);
  await user.type(screen.getByRole('searchbox', { name: 'Sök repositories' }), 'other');
  expect(screen.getByRole('radio', { name: /gamma/i })).toBeInTheDocument();
  expect(screen.queryByRole('radio', { name: /alpha/i })).not.toBeInTheDocument();
});

it('shows recent repositories above the unchanged alphabetical repository list', async () => {
  markRepositoryRecent(repositories[1]);
  const onSelect = vi.fn();
  render(<MemoryRouter><RepositoryPicker repositories={repositories} mode="select" selectedRepositoryKey="" onSelect={onSelect} /></MemoryRouter>);

  expect(screen.getByRole('heading', { name: 'Senast använda' })).toBeInTheDocument();
  await userEvent.setup().click(screen.getByRole('button', { name: /beta/i }));
  expect(onSelect).toHaveBeenCalledWith(repositories[1]);
  expect(screen.getByRole('radio', { name: /alpha/i })).toBeInTheDocument();
  expect(screen.getByRole('radio', { name: /beta/i })).toBeInTheDocument();
  expect(repositoryKey(repositories[1])).toBe('11:21');
});
