import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import MaintenancePage from './MaintenancePage';
import * as maintenanceApi from '../api/maintenance';

vi.mock('../api/maintenance', async () => {
  const actual = await vi.importActual<typeof import('../api/maintenance')>('../api/maintenance');
  return { ...actual, getWorkBranchCleanupPreview: vi.fn(), cleanupWorkBranches: vi.fn() };
});

const safe = {
  githubInstallationId: 1,
  githubRepositoryId: 2,
  repositoryFullName: 'owner/repo',
  defaultBranch: 'main',
  branchName: 'zip-github/work-123e4567-e89b-12d3-a456-426614174000',
  commitSha: 'a'.repeat(40),
  classification: 'SAFE_TO_DELETE',
  reason: 'Ingen icke-terminal Work eller öppen pull request hittades.',
  deletable: true,
};
const active = { ...safe, branchName: 'zip-github/work-223e4567-e89b-12d3-a456-426614174000', classification: 'ACTIVE_WORK', reason: 'Används.', deletable: false };

beforeEach(() => {
  vi.mocked(maintenanceApi.getWorkBranchCleanupPreview).mockResolvedValue({
    repositoriesChecked: 12, workBranchesFound: 2, safeToDelete: 1, inUseOrProtected: 1, unverifiable: 0,
    candidates: [safe, active], issues: [],
  });
  vi.mocked(maintenanceApi.cleanupWorkBranches).mockResolvedValue({ results: [{ repositoryFullName: 'owner/repo', branchName: safe.branchName, status: 'DELETED', reason: 'Raderad.' }] });
});

afterEach(() => vi.clearAllMocks());

test('shows read-only preview and deletes only explicitly safe candidates', async () => {
  render(<MaintenancePage />);
  expect(await screen.findByText('12')).toBeInTheDocument();
  expect(screen.getByText('Används.')).toBeInTheDocument();
  const button = screen.getByRole('button', { name: 'Ta bort 1 säkert identifierade brancher' });
  expect(button).toBeDisabled();
  fireEvent.click(screen.getByRole('checkbox'));
  expect(button).toBeEnabled();
  fireEvent.click(button);
  await waitFor(() => expect(maintenanceApi.cleanupWorkBranches).toHaveBeenCalledWith([safe]));
  expect(await screen.findByText(/DELETED/)).toBeInTheDocument();
});
