import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import StagingClaimPage from './StagingClaimPage';
import { STAGING_CLAIM_TOKEN_KEY } from '../staging/claimToken';

const mocks = vi.hoisted(() => ({
  claimStagingImport: vi.fn(),
  getClaimedStagingImport: vi.fn(),
  promoteStagingImport: vi.fn(),
  getRepositories: vi.fn(),
  prepareImportReview: vi.fn(),
  getProjectWork: vi.fn(),
}));

vi.mock('../api/staging', () => ({
  claimStagingImport: mocks.claimStagingImport,
  getClaimedStagingImport: mocks.getClaimedStagingImport,
  promoteStagingImport: mocks.promoteStagingImport,
}));
vi.mock('../api/repositories', () => ({ getRepositories: mocks.getRepositories }));
vi.mock('../api/imports', () => ({ prepareImportReview: mocks.prepareImportReview }));
vi.mock('../api/projects', () => ({ getProjectWork: mocks.getProjectWork }));

const claimed = {
  stagingId: 'staging-1', originalFilename: 'project.zip', sizeBytes: 2048, sha256: 'a'.repeat(64),
  expiresAt: '2026-08-08T08:00:00Z', claimedAt: '2026-08-08T06:00:00Z',
};
const repositories = [
  { githubInstallationId: 10, githubRepositoryId: 20, repositoryFullName: 'erland/first', repositoryName: 'first', privateRepository: true, defaultBranch: 'main', htmlUrl: 'https://github.com/erland/first', projectId: 'project-1' },
  { githubInstallationId: 11, githubRepositoryId: 21, repositoryFullName: 'erland/second', repositoryName: 'second', privateRepository: true, defaultBranch: 'main', htmlUrl: 'https://github.com/erland/second', projectId: null },
];

beforeEach(() => {
  sessionStorage.clear();
  localStorage.clear();
  sessionStorage.setItem(STAGING_CLAIM_TOKEN_KEY, 'claim-token');
  mocks.claimStagingImport.mockReset().mockResolvedValue(claimed);
  mocks.getClaimedStagingImport.mockReset().mockResolvedValue(claimed);
  mocks.getRepositories.mockReset().mockResolvedValue(repositories);
  mocks.promoteStagingImport.mockReset().mockResolvedValue({ stagingId: 'staging-1', projectId: 'project-2', importId: 'import-1', status: 'PROMOTED', alreadyPromoted: false });
  mocks.prepareImportReview.mockReset().mockResolvedValue({ id: 'plan-1' });
  mocks.getProjectWork.mockReset().mockResolvedValue(null);
});

afterEach(() => cleanup());

it('promotes a claimed staging ZIP into a repository and enters the ordinary review flow', async () => {
  const user = userEvent.setup();
  render(
    <MemoryRouter initialEntries={['/staging/claim']}>
      <Routes>
        <Route path="/staging/claim" element={<StagingClaimPage />} />
        <Route path="/projects/:projectId/imports/:importId/review" element={<h1>Ordinarie granskning</h1>} />
      </Routes>
    </MemoryRouter>,
  );

  expect(await screen.findByRole('heading', { name: 'Välj repository för ZIP-filen' })).toBeInTheDocument();
  expect(screen.getByText('project.zip')).toBeInTheDocument();
  await user.click(screen.getByRole('radio', { name: /second/i }));
  await user.click(screen.getByRole('button', { name: 'Fortsätt till granskning' }));

  expect(mocks.promoteStagingImport).toHaveBeenCalledWith('staging-1', { githubInstallationId: 11, githubRepositoryId: 21 }, false);
  expect(mocks.prepareImportReview).toHaveBeenCalledWith('import-1');
  expect(await screen.findByRole('heading', { name: 'Ordinarie granskning' })).toBeInTheDocument();
});

it('reuses an existing internal project when the selected repository already has one', async () => {
  const user = userEvent.setup();
  render(<MemoryRouter initialEntries={['/staging/claim']}><Routes><Route path="/staging/claim" element={<StagingClaimPage />} /></Routes></MemoryRouter>);

  await screen.findByRole('heading', { name: 'Välj repository för ZIP-filen' });
  await user.click(screen.getByRole('radio', { name: /first/i }));
  await user.click(screen.getByRole('button', { name: 'Fortsätt till granskning' }));
  expect(mocks.promoteStagingImport).toHaveBeenCalledWith('staging-1', { projectId: 'project-1' }, false);
});


it('searches repositories in the Shortcut flow and keeps the selected repository visible beside the continue action', async () => {
  const user = userEvent.setup();
  render(<MemoryRouter initialEntries={['/staging/claim']}><Routes><Route path="/staging/claim" element={<StagingClaimPage />} /></Routes></MemoryRouter>);

  await screen.findByRole('heading', { name: 'Välj repository för ZIP-filen' });
  const search = screen.getByRole('searchbox', { name: 'Sök repositories' });
  await user.type(search, 'second');
  expect(screen.queryByRole('radio', { name: /first/i })).not.toBeInTheDocument();
  await user.click(screen.getByRole('radio', { name: /second/i }));

  const summary = screen.getByText('Valt repository').closest('.repository-selected-summary');
  expect(summary).toHaveTextContent('second');
  expect(summary).toHaveTextContent('erland/second');
  expect(screen.getByRole('button', { name: 'Fortsätt till granskning' })).toBeEnabled();
});


it('shows a high-confidence Shortcut repository suggestion without implicitly selecting or promoting it', async () => {
  mocks.claimStagingImport.mockResolvedValue({ ...claimed, originalFilename: 'fyrens-vaktare-v0.8.8.zip' });
  mocks.getRepositories.mockResolvedValue([
    repositories[0],
    { ...repositories[1], repositoryName: 'bradspel-fyrens-vaktare', repositoryFullName: 'erland/bradspel-fyrens-vaktare' },
  ]);
  const user = userEvent.setup();
  render(<MemoryRouter initialEntries={['/staging/claim']}><Routes><Route path="/staging/claim" element={<StagingClaimPage />} /></Routes></MemoryRouter>);

  expect(await screen.findByText('Föreslaget repository')).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'bradspel-fyrens-vaktare' })).toBeInTheDocument();
  expect(screen.queryByRole('radio', { name: /bradspel-fyrens-vaktare/i })).not.toBeInTheDocument();
  expect(mocks.promoteStagingImport).not.toHaveBeenCalled();
  await user.click(screen.getByRole('button', { name: 'Använd detta repository' }));
  expect(screen.getByText('Valt repository').closest('.repository-selected-summary')).toHaveTextContent('bradspel-fyrens-vaktare');
  expect(mocks.promoteStagingImport).not.toHaveBeenCalled();
});

it('falls back to the searchable picker when the Shortcut filename is ambiguous', async () => {
  mocks.claimStagingImport.mockResolvedValue({ ...claimed, originalFilename: 'granslinjen-r0042.zip' });
  mocks.getRepositories.mockResolvedValue([
    { ...repositories[0], repositoryName: 'roman-granslinjen', repositoryFullName: 'erland/roman-granslinjen' },
    { ...repositories[1], repositoryName: 'bradspel-granslinjen', repositoryFullName: 'erland/bradspel-granslinjen' },
  ]);
  render(<MemoryRouter initialEntries={['/staging/claim']}><Routes><Route path="/staging/claim" element={<StagingClaimPage />} /></Routes></MemoryRouter>);

  expect(await screen.findByRole('searchbox', { name: 'Sök repositories' })).toBeInTheDocument();
  expect(screen.queryByText('Föreslaget repository')).not.toBeInTheDocument();
});

it('requires confirmation before Shortcut promotion updates an existing open PR', async () => {
  const user = userEvent.setup();
  mocks.getProjectWork.mockResolvedValue({ id: 'work-1', projectId: 'project-1', baseBranch: 'main', branchName: 'zip-github/work-1', status: 'PR_OPEN', headCommitSha: 'a'.repeat(40), remoteHeadCommitSha: 'a'.repeat(40), branchChangedExternally: false, lastImportId: 'old', pullRequestNumber: 42, pullRequestUrl: 'https://github.com/erland/first/pull/42', createdAt: '2026-08-13T10:00:00Z', updatedAt: '2026-08-13T11:00:00Z' });
  render(<MemoryRouter initialEntries={['/staging/claim']}><Routes><Route path="/staging/claim" element={<StagingClaimPage />} /></Routes></MemoryRouter>);

  await screen.findByRole('heading', { name: 'Välj repository för ZIP-filen' });
  await user.click(screen.getByRole('radio', { name: /first/i }));
  expect(await screen.findByRole('alert', { name: 'Öppen pull request' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Fortsätt till granskning' })).toBeDisabled();
  await user.click(screen.getByRole('button', { name: 'Ja, fortsätt med denna ZIP' }));
  await user.click(screen.getByRole('button', { name: 'Fortsätt till granskning' }));
  expect(mocks.promoteStagingImport).toHaveBeenCalledWith('staging-1', { projectId: 'project-1' }, true);
});
