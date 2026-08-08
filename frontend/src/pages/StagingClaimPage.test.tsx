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
  getProjects: vi.fn(),
  prepareImportReview: vi.fn(),
}));

vi.mock('../api/staging', () => ({
  claimStagingImport: mocks.claimStagingImport,
  getClaimedStagingImport: mocks.getClaimedStagingImport,
  promoteStagingImport: mocks.promoteStagingImport,
}));
vi.mock('../api/projects', () => ({ getProjects: mocks.getProjects }));
vi.mock('../api/imports', () => ({ prepareImportReview: mocks.prepareImportReview }));

const claimed = {
  stagingId: 'staging-1', originalFilename: 'project.zip', sizeBytes: 2048, sha256: 'a'.repeat(64),
  expiresAt: '2026-08-08T08:00:00Z', claimedAt: '2026-08-08T06:00:00Z',
};
const projects = [
  { id: 'project-1', name: 'First', githubInstallationId: 10, githubRepositoryId: 20, repositoryFullName: 'erland/first', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-08T05:00:00Z', updatedAt: '2026-08-08T05:00:00Z' },
  { id: 'project-2', name: 'Second', githubInstallationId: 11, githubRepositoryId: 21, repositoryFullName: 'erland/second', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-08T05:00:00Z', updatedAt: '2026-08-08T05:00:00Z' },
];

beforeEach(() => {
  sessionStorage.clear();
  sessionStorage.setItem(STAGING_CLAIM_TOKEN_KEY, 'claim-token');
  mocks.claimStagingImport.mockReset().mockResolvedValue(claimed);
  mocks.getClaimedStagingImport.mockReset().mockResolvedValue(claimed);
  mocks.getProjects.mockReset().mockResolvedValue(projects);
  mocks.promoteStagingImport.mockReset().mockResolvedValue({ stagingId: 'staging-1', projectId: 'project-2', importId: 'import-1', status: 'PROMOTED', alreadyPromoted: false });
  mocks.prepareImportReview.mockReset().mockResolvedValue({ id: 'plan-1' });
});

afterEach(() => cleanup());

it('promotes a claimed staging ZIP into the selected project and enters the ordinary review flow', async () => {
  const user = userEvent.setup();
  render(
    <MemoryRouter initialEntries={['/staging/claim']}>
      <Routes>
        <Route path="/staging/claim" element={<StagingClaimPage />} />
        <Route path="/projects/:projectId/imports/:importId/review" element={<h1>Ordinarie granskning</h1>} />
      </Routes>
    </MemoryRouter>,
  );

  expect(await screen.findByRole('heading', { name: 'Välj projekt för ZIP-filen' })).toBeInTheDocument();
  expect(screen.getByText('project.zip')).toBeInTheDocument();
  expect(screen.getByRole('radio', { name: /Second/ })).toBeInTheDocument();
  await user.click(screen.getByRole('radio', { name: /Second/ }));
  await user.click(screen.getByRole('button', { name: 'Fortsätt till granskning' }));

  expect(mocks.promoteStagingImport).toHaveBeenCalledWith('staging-1', 'project-2');
  expect(mocks.prepareImportReview).toHaveBeenCalledWith('import-1');
  expect(await screen.findByRole('heading', { name: 'Ordinarie granskning' })).toBeInTheDocument();
});

it('keeps the claimed ZIP available when a project is blocked by an active import', async () => {
  const user = userEvent.setup();
  mocks.promoteStagingImport.mockRejectedValueOnce(new Error('Det finns redan en aktiv import för projektet.'));
  render(<MemoryRouter initialEntries={['/staging/claim']}><Routes><Route path="/staging/claim" element={<StagingClaimPage />} /></Routes></MemoryRouter>);

  await screen.findByRole('heading', { name: 'Välj projekt för ZIP-filen' });
  await user.click(screen.getByRole('radio', { name: /First/ }));
  await user.click(screen.getByRole('button', { name: 'Fortsätt till granskning' }));

  expect(await screen.findByRole('alert')).toHaveTextContent('Det finns redan en aktiv import för projektet.');
  expect(screen.getByText('project.zip')).toBeInTheDocument();
  expect(screen.getByRole('radio', { name: /Second/ })).toBeEnabled();
  expect(mocks.prepareImportReview).not.toHaveBeenCalled();
});
