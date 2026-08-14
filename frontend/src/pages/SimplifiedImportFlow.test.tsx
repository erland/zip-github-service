import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import NewImportPage from './NewImportPage';
import ImportReviewPage from './ImportReviewPage';

const mocks = vi.hoisted(() => ({
  createImport: vi.fn(),
  uploadZip: vi.fn(),
  prepareImportReview: vi.fn(),
  getImportPlan: vi.fn(),
  getImportSelection: vi.fn(),
  getImportPlanApproval: vi.fn(),
  getExternalBranchChanges: vi.fn(),
  findDelivery: vi.fn(),
  createImportSelection: vi.fn(),
  approveImportPlan: vi.fn(),
  prepareImportWorkspace: vi.fn(),
  deliverImport: vi.fn(),
  getProject: vi.fn(),
  getProjectWork: vi.fn(),
  startProjectWork: vi.fn(),
  getCurrentUser: vi.fn(),
}));

vi.mock('../api/imports', () => ({
  createImport: mocks.createImport,
  uploadZip: mocks.uploadZip,
  prepareImportReview: mocks.prepareImportReview,
  getImportPlan: mocks.getImportPlan,
  getImportSelection: mocks.getImportSelection,
  getImportPlanApproval: mocks.getImportPlanApproval,
  getExternalBranchChanges: mocks.getExternalBranchChanges,
  findDelivery: mocks.findDelivery,
  createImportSelection: mocks.createImportSelection,
  approveImportPlan: mocks.approveImportPlan,
  prepareImportWorkspace: mocks.prepareImportWorkspace,
  deliverImport: mocks.deliverImport,
}));
vi.mock('../api/projects', () => ({ getProject: mocks.getProject, getProjectWork: mocks.getProjectWork, startProjectWork: mocks.startProjectWork }));
vi.mock('../api/auth', () => ({ getCurrentUser: mocks.getCurrentUser }));

const project = {
  id: 'project-1', name: 'Zip GitHub', githubInstallationId: 10, githubRepositoryId: 20,
  repositoryFullName: 'erland/zip-github-service', privateRepository: true, defaultBranch: 'main',
  active: true, createdAt: '2026-08-07T10:00:00Z', updatedAt: '2026-08-07T10:00:00Z',
};
const currentUser = { id: 'user-1', githubUserId: 1, login: 'erland', avatarUrl: null,
  gitName: 'Erland', gitEmail: '1+erland@users.noreply.github.com' };
const activeWork = { id: 'work-1', projectId: 'project-1', baseBranch: 'main', branchName: 'zip-github/work-work-1', status: 'ACTIVE',
  headCommitSha: null, remoteHeadCommitSha: null, branchChangedExternally: false, lastImportId: null,
  pullRequestNumber: null, pullRequestUrl: null, createdAt: '2026-08-07T17:59:00Z', updatedAt: '2026-08-07T17:59:00Z' };
const upload = { id: 'upload-1', importId: 'import-1', originalFilename: 'project.zip', sizeBytes: 42,
  sha256: 'a'.repeat(64), status: 'STORED', createdAt: '2026-08-07T10:00:00Z', retentionDeadline: '2026-08-08T10:00:00Z' };
const plan = {
  id: 'plan-1', importId: 'import-1', sourceUploadSha256: upload.sha256, baseCommitSha: 'b'.repeat(40),
  policyVersion: 'mvp-3', planDigestSha256: 'c'.repeat(64), status: 'READY', approvable: true,
  added: 0, modified: 2, unchanged: 1, ignored: 0, blocked: 1, hardBlocked: 0, overridableBlocked: 1,
  warnings: 0, createdAt: '2026-08-07T18:00:00Z',
  entries: [
    { path: 'README.md', status: 'MODIFIED', comparisonStatus: 'MODIFIED', severity: 'NONE', blockerType: 'NONE', policyCode: null, message: null, archiveSizeBytes: 20, archiveSha256: '1'.repeat(64), repositorySizeBytes: 10, repositorySha256: '2'.repeat(64), textCandidate: true },
    { path: '.github/workflows/unchanged.yml', status: 'UNCHANGED', comparisonStatus: 'UNCHANGED', severity: 'NONE', blockerType: 'NONE', policyCode: null, message: null, archiveSizeBytes: 30, archiveSha256: '3'.repeat(64), repositorySizeBytes: 30, repositorySha256: '3'.repeat(64), textCandidate: true },
    { path: '.github/workflows/changed.yml', status: 'BLOCKED', comparisonStatus: 'MODIFIED', severity: 'BLOCKING', blockerType: 'OVERRIDABLE_BLOCKED', policyCode: 'GITHUB_WORKFLOW_PROTECTED', message: 'Workflow change requires explicit override.', archiveSizeBytes: 30, archiveSha256: '4'.repeat(64), repositorySizeBytes: 20, repositorySha256: '5'.repeat(64), textCandidate: true },
  ],
};
const selection = {
  id: 'selection-1', importId: 'import-1', planId: 'plan-1', planDigestSha256: plan.planDigestSha256,
  baseCommitSha: plan.baseCommitSha, selectionVersion: 'selection-2', selectionDigestSha256: 'd'.repeat(64),
  selectedPaths: ['.github/workflows/changed.yml'], excludedPaths: ['README.md'],
  overrides: [{ path: '.github/workflows/changed.yml', blockerType: 'OVERRIDABLE_BLOCKED', policyCode: 'GITHUB_WORKFLOW_PROTECTED', acknowledgement: 'User explicitly approved this policy override in the review UI.' }],
  blockerDecisions: [{ path: '.github/workflows/changed.yml', blockerType: 'OVERRIDABLE_BLOCKED', decision: 'INCLUDE_OVERRIDE' as const }],
  createdAt: '2026-08-07T18:01:00Z',
};
const approval = { importId: 'import-1', planId: 'plan-1', planDigestSha256: plan.planDigestSha256,
  selectionDigestSha256: selection.selectionDigestSha256, status: 'APPROVED', approvedAt: '2026-08-07T18:02:00Z' };
const workspace = { importId: 'import-1', repositoryFullName: project.repositoryFullName, baseCommitSha: plan.baseCommitSha,
  planDigestSha256: plan.planDigestSha256, selectionDigestSha256: selection.selectionDigestSha256,
  appliedFileCount: 1, appliedPaths: selection.selectedPaths, status: 'FILES_APPLIED', preparedAt: '2026-08-07T18:03:00Z' };
const delivery = { importId: 'import-1', repositoryFullName: project.repositoryFullName, baseBranch: 'main',
  branchName: 'zip-github/work-work-1', baseCommitSha: plan.baseCommitSha, commitSha: 'e'.repeat(40),
  planDigestSha256: plan.planDigestSha256, status: 'PUSHED', pushedAt: '2026-08-07T18:04:00Z' };

function renderFlow() {
  return render(
    <MemoryRouter initialEntries={['/projects/project-1/imports/new']}>
      <Routes>
        <Route path="/projects/:projectId/imports/new" element={<NewImportPage />} />
        <Route path="/projects/:projectId/imports/:importId/review" element={<ImportReviewPage />} />
        <Route path="/projects/:projectId/imports/:importId/result" element={<h1>Importresultat</h1>} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  mocks.createImport.mockReset();
  mocks.uploadZip.mockReset();
  mocks.prepareImportReview.mockReset();
  mocks.getImportPlan.mockReset();
  mocks.getImportSelection.mockReset();
  mocks.getImportPlanApproval.mockReset();
  mocks.getExternalBranchChanges.mockReset();
  mocks.findDelivery.mockReset();
  mocks.createImportSelection.mockReset();
  mocks.approveImportPlan.mockReset();
  mocks.prepareImportWorkspace.mockReset();
  mocks.deliverImport.mockReset();
  mocks.getProject.mockReset();
  mocks.getProjectWork.mockReset();
  mocks.startProjectWork.mockReset();
  mocks.getCurrentUser.mockReset();
  mocks.getProject.mockResolvedValue(project);
  mocks.getProjectWork.mockResolvedValue(null);
  mocks.startProjectWork.mockResolvedValue(activeWork);
  mocks.getCurrentUser.mockResolvedValue(currentUser);
  mocks.createImport.mockResolvedValue({ id: 'import-1', projectId: project.id, baseBranch: 'main', status: 'CREATED', createdAt: '2026-08-07T18:00:00Z' });
  mocks.uploadZip.mockImplementation(async (_id, _file, onProgress: (value: number) => void) => { onProgress(100); return upload; });
  mocks.prepareImportReview.mockResolvedValue(plan);
  mocks.getImportPlan.mockResolvedValue(plan);
  mocks.getImportSelection.mockResolvedValue(null);
  mocks.getImportPlanApproval.mockResolvedValue(null);
  mocks.getExternalBranchChanges.mockResolvedValue({ branchChanged: false, previousKnownHeadSha: null, reviewBaseHeadSha: plan.baseCommitSha, changedPaths: [] });
  mocks.findDelivery.mockResolvedValue(null);
  mocks.createImportSelection.mockResolvedValue(selection);
  mocks.approveImportPlan.mockResolvedValue(approval);
  mocks.prepareImportWorkspace.mockResolvedValue(workspace);
  mocks.deliverImport.mockResolvedValue(delivery);
});

afterEach(() => cleanup());

describe('simplified import flow E2E regression', () => {
  it('runs upload -> automatic review -> exact selection/override -> one-click delivery without redundant manual steps', async () => {
    const user = userEvent.setup();
    renderFlow();

    await screen.findByRole('heading', { name: 'Ladda upp projekt-ZIP' });
    await user.click(screen.getByRole('radio', { name: /Någon annan/ }));
    await user.type(screen.getByLabelText('Namn'), 'Anna Andersson');
    await user.type(screen.getByLabelText('E-post'), 'anna@example.com');
    await user.upload(screen.getByLabelText('Projektarkiv'), new File(['zip'], 'project.zip', { type: 'application/zip' }));
    await user.click(screen.getByRole('button', { name: 'Ladda upp ZIP' }));

    expect(await screen.findByRole('heading', { name: 'Granska förändringar' })).toBeInTheDocument();
    expect(mocks.startProjectWork).toHaveBeenCalledWith('project-1');
    expect(mocks.createImport).toHaveBeenCalledWith('project-1', { name: 'Anna Andersson', email: 'anna@example.com' }, false);
    expect(mocks.uploadZip).toHaveBeenCalledTimes(1);
    expect(mocks.prepareImportReview).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('button', { name: 'Skapa granskningsplan' })).not.toBeInTheDocument();

    await user.click(await screen.findByRole('checkbox', { name: 'Exkludera README.md' }));
    await user.click(screen.getByRole('button', { name: /Oförändrade \(1\)/ }));
    expect(screen.getByTitle('.github/workflows/unchanged.yml')).toBeInTheDocument();
    expect(screen.queryByText('Jag förstår risken och vill ta med denna blockerade förändring')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Blockerade \(1\)/ }));
    expect(screen.getByTitle('.github/workflows/changed.yml')).toBeInTheDocument();
    await user.click(screen.getByRole('radio', { name: 'Jag förstår risken – godkänn och ta med' }));
    await user.type(screen.getByRole('textbox', { name: 'Meddelande' }), 'Apply reviewed ZIP changes');
    await user.click(screen.getByRole('button', { name: 'Godkänn valda förändringar' }));

    expect(await screen.findByRole('heading', { name: 'Importresultat' })).toBeInTheDocument();
    expect(mocks.createImportSelection).toHaveBeenCalledWith(
      'import-1', plan.planDigestSha256, plan.baseCommitSha,
      ['.github/workflows/changed.yml'], ['.github/workflows/changed.yml'],
      [{ path: '.github/workflows/changed.yml', decision: 'INCLUDE_OVERRIDE' }],
    );
    expect(mocks.approveImportPlan).toHaveBeenCalledTimes(1);
    expect(mocks.prepareImportWorkspace).toHaveBeenCalledTimes(1);
    expect(mocks.deliverImport).toHaveBeenCalledTimes(1);
    expect(delivery.branchName).toMatch(/^zip-github\/work-/);
  });
});
