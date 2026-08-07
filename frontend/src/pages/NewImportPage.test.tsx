import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import NewImportPage from './NewImportPage';

const mocks = vi.hoisted(() => ({
  createImport: vi.fn(),
  uploadZip: vi.fn(),
  prepareImportReview: vi.fn(),
  cancelImport: vi.fn(),
  getProject: vi.fn(),
  getCurrentUser: vi.fn(),
}));

vi.mock('../api/imports', () => ({
  createImport: mocks.createImport,
  uploadZip: mocks.uploadZip,
  prepareImportReview: mocks.prepareImportReview,
  cancelImport: mocks.cancelImport,
}));
vi.mock('../api/projects', () => ({ getProject: mocks.getProject }));
vi.mock('../api/auth', () => ({ getCurrentUser: mocks.getCurrentUser }));

const project = {
  id: 'project-1', name: 'Zip GitHub', githubInstallationId: 10, githubRepositoryId: 20,
  repositoryFullName: 'erland/zip-github-service', privateRepository: true, defaultBranch: 'main',
  active: true, createdAt: '2026-08-07T10:00:00Z', updatedAt: '2026-08-07T10:00:00Z',
};
const currentUser = { id: 'user-1', githubUserId: 1, login: 'erland', avatarUrl: null,
  gitName: 'Erland', gitEmail: '1+erland@users.noreply.github.com' };
const upload = { id: 'upload-1', importId: 'import-1', originalFilename: 'project.zip', sizeBytes: 42,
  sha256: 'a'.repeat(64), status: 'STORED', createdAt: '2026-08-07T10:00:00Z', retentionDeadline: '2026-08-08T10:00:00Z' };

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/projects/project-1/imports/new']}>
      <Routes>
        <Route path="/projects/:projectId/imports/new" element={<NewImportPage />} />
        <Route path="/projects/:projectId/imports/:importId/review" element={<h1>Granskningen öppnades</h1>} />
        <Route path="/projects/:projectId" element={<h1>Projektet öppnades</h1>} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  mocks.createImport.mockReset().mockResolvedValue({ id: 'import-1', projectId: 'project-1', baseBranch: 'main', status: 'CREATED', createdAt: '2026-08-07T10:00:00Z' });
  mocks.uploadZip.mockReset().mockImplementation(async (_id, _file, onProgress: (value: number) => void) => {
    onProgress(100);
    return upload;
  });
  mocks.prepareImportReview.mockReset().mockResolvedValue({ id: 'plan-1' });
  mocks.cancelImport.mockReset().mockResolvedValue({ id: 'import-1', projectId: 'project-1', baseBranch: 'main', status: 'CANCELLED', createdAt: '2026-08-07T10:00:00Z' });
  mocks.getProject.mockReset().mockResolvedValue(project);
  mocks.getCurrentUser.mockReset().mockResolvedValue(currentUser);
});

afterEach(() => cleanup());

describe('NewImportPage automatic review preparation', () => {
  it('continues from a successful upload directly to the review route', async () => {
    const user = userEvent.setup();
    renderPage();

    const input = await screen.findByLabelText('Projektarkiv');
    await user.upload(input, new File(['zip'], 'project.zip', { type: 'application/zip' }));
    await user.click(screen.getByRole('button', { name: 'Ladda upp ZIP' }));

    expect(await screen.findByRole('heading', { name: 'Granskningen öppnades' })).toBeInTheDocument();
    expect(mocks.uploadZip).toHaveBeenCalledTimes(1);
    expect(mocks.prepareImportReview).toHaveBeenCalledWith('import-1');
    expect(screen.queryByRole('button', { name: 'Skapa granskningsplan' })).not.toBeInTheDocument();
  });


  it('does not allow a slow automatic plan build to trigger duplicate preparation', async () => {
    const user = userEvent.setup();
    let resolvePreparation: ((value: { id: string }) => void) | undefined;
    mocks.prepareImportReview.mockImplementationOnce(() => new Promise((resolve) => { resolvePreparation = resolve; }));
    renderPage();

    const input = await screen.findByLabelText('Projektarkiv');
    await user.upload(input, new File(['zip'], 'project.zip', { type: 'application/zip' }));
    await user.click(screen.getByRole('button', { name: 'Ladda upp ZIP' }));

    expect(await screen.findByText(/Analyserar ZIP-filen/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Förbereder granskning…' })).toBeDisabled();
    expect(mocks.prepareImportReview).toHaveBeenCalledTimes(1);

    resolvePreparation?.({ id: 'plan-1' });
    expect(await screen.findByRole('heading', { name: 'Granskningen öppnades' })).toBeInTheDocument();
    expect(mocks.prepareImportReview).toHaveBeenCalledTimes(1);
  });

  it('offers preparation retry without uploading the ZIP again', async () => {
    const user = userEvent.setup();
    mocks.prepareImportReview.mockRejectedValueOnce(new Error('GitHub kunde inte läsas.'));
    renderPage();

    const input = await screen.findByLabelText('Projektarkiv');
    await user.upload(input, new File(['zip'], 'project.zip', { type: 'application/zip' }));
    await user.click(screen.getByRole('button', { name: 'Ladda upp ZIP' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('GitHub kunde inte läsas.');
    const retry = screen.getByRole('button', { name: 'Försök skapa granskningsplan igen' });
    expect(input).toBeDisabled();
    expect(mocks.uploadZip).toHaveBeenCalledTimes(1);

    mocks.prepareImportReview.mockResolvedValueOnce({ id: 'plan-1' });
    await user.click(retry);

    expect(await screen.findByRole('heading', { name: 'Granskningen öppnades' })).toBeInTheDocument();
    expect(mocks.uploadZip).toHaveBeenCalledTimes(1);
    expect(mocks.prepareImportReview).toHaveBeenCalledTimes(2);
  });
  it('can cancel a resumable import instead of retrying the upload flow', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/projects/project-1/imports/new?importId=import-1']}>
        <Routes>
          <Route path="/projects/:projectId/imports/new" element={<NewImportPage />} />
          <Route path="/projects/:projectId" element={<h1>Projektet öppnades</h1>} />
        </Routes>
      </MemoryRouter>,
    );

    await screen.findByRole('heading', { name: 'Ladda upp projekt-ZIP' });
    await user.click(screen.getByRole('button', { name: 'Avbryt import' }));
    await user.click(screen.getByRole('button', { name: 'Ja, avbryt import' }));

    expect(mocks.cancelImport).toHaveBeenCalledWith('import-1');
    expect(await screen.findByRole('heading', { name: 'Projektet öppnades' })).toBeInTheDocument();
  });

});
