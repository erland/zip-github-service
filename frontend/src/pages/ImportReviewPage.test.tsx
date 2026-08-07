import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ImportReviewPage from './ImportReviewPage';

const plan = {
  id: 'plan-1',
  importId: 'import-1',
  sourceUploadSha256: 'a'.repeat(64),
  baseCommitSha: 'b'.repeat(40),
  policyVersion: 'mvp-2',
  planDigestSha256: 'c'.repeat(64),
  status: 'READY',
  approvable: true,
  added: 1,
  modified: 1,
  unchanged: 1,
  ignored: 1,
  blocked: 1,
  hardBlocked: 0,
  overridableBlocked: 1,
  warnings: 1,
  createdAt: '2026-08-06T19:00:00Z',
  entries: [
    { path: 'README.md', status: 'MODIFIED', comparisonStatus: 'MODIFIED', severity: 'NONE', blockerType: 'NONE', policyCode: null, message: null, archiveSizeBytes: 20, archiveSha256: '1'.repeat(64), repositorySizeBytes: 10, repositorySha256: '2'.repeat(64), textCandidate: true },
    { path: 'docs/new.md', status: 'ADDED', comparisonStatus: 'ADDED', severity: 'WARNING', blockerType: 'NONE', policyCode: 'ENVIRONMENT_FILE_WARNING', message: 'Kontrollera filen.', archiveSizeBytes: 12, archiveSha256: '3'.repeat(64), repositorySizeBytes: null, repositorySha256: null, textCandidate: true },
    { path: '.github/workflows/ci.yml', status: 'BLOCKED', comparisonStatus: 'MODIFIED', severity: 'BLOCKING', blockerType: 'OVERRIDABLE_BLOCKED', policyCode: 'GITHUB_WORKFLOW_PROTECTED', message: 'Changes under .github/** are blocked in the MVP.', archiveSizeBytes: 30, archiveSha256: '4'.repeat(64), repositorySizeBytes: 20, repositorySha256: '5'.repeat(64), textCandidate: true },
    { path: 'src/App.java', status: 'UNCHANGED', comparisonStatus: 'UNCHANGED', severity: 'NONE', blockerType: 'NONE', policyCode: null, message: null, archiveSizeBytes: 50, archiveSha256: '6'.repeat(64), repositorySizeBytes: 50, repositorySha256: '6'.repeat(64), textCandidate: true },
    { path: '__MACOSX/._README.md', status: 'IGNORED', comparisonStatus: null, severity: 'NONE', blockerType: 'NONE', policyCode: 'TRANSPORT_NOISE', message: 'Transport metadata is ignored.', archiveSizeBytes: null, archiveSha256: null, repositorySizeBytes: null, repositorySha256: null, textCandidate: false },
  ],
};

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => plan }));
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/projects/project-1/imports/import-1/review']}>
      <Routes>
        <Route path="projects/:projectId/imports/:importId/review" element={<ImportReviewPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ImportReviewPage', () => {
  it('shows plan summary and changed files by default', async () => {
    renderPage();
    expect(await screen.findByRole('heading', { name: 'Granska förändringar' })).toBeInTheDocument();
    expect(await screen.findByText('Urvalet kan godkännas')).toBeInTheDocument();
    expect(screen.getByText('README.md')).toBeInTheDocument();
    expect(screen.getByTitle('docs/new.md')).toBeInTheDocument();
    expect(screen.queryByTitle('.github/workflows/ci.yml')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Godkänn valda förändringar' })).toBeEnabled();
  });

  it('filters to blocked entries', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText('README.md');
    await user.click(screen.getByRole('button', { name: 'Blockerade' }));
    const list = screen.getByRole('list', { name: 'Filträd' });
    expect(within(list).getByTitle('.github/workflows/ci.yml')).toBeInTheDocument();
    expect(within(list).queryByText('README.md')).not.toBeInTheDocument();
  });

  it('approves the exact digest for an approvable plan', async () => {
    const user = userEvent.setup();
    const readyPlan = { ...plan, status: 'READY', approvable: true, blocked: 0, hardBlocked: 0, overridableBlocked: 0,
      entries: plan.entries.filter((entry) => entry.status !== 'BLOCKED') };
    const selection = { id: 'selection-1', importId: 'import-1', planId: 'plan-1', planDigestSha256: readyPlan.planDigestSha256,
      baseCommitSha: readyPlan.baseCommitSha, selectionVersion: 'selection-1', selectionDigestSha256: 'd'.repeat(64),
      selectedPaths: ['README.md', 'docs/new.md'], excludedPaths: [], overrides: [], createdAt: '2026-08-06T20:29:00Z' };
    const approval = { importId: 'import-1', planId: 'plan-1', planDigestSha256: readyPlan.planDigestSha256,
      selectionDigestSha256: selection.selectionDigestSha256, status: 'APPROVED', approvedAt: '2026-08-06T20:30:00Z' };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => readyPlan })
      .mockResolvedValueOnce({ ok: true, json: async () => selection })
      .mockResolvedValueOnce({ ok: true, json: async () => approval });
    vi.stubGlobal('fetch', fetchMock);

    renderPage();
    const button = await screen.findByRole('button', { name: 'Godkänn valda förändringar' });
    expect(button).toBeEnabled();
    await user.click(button);

    expect(await screen.findByText('Planen är godkänd')).toBeInTheDocument();
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/imports/import-1/selection', expect.objectContaining({ method: 'POST', credentials: 'include' }));
    expect(fetchMock).toHaveBeenLastCalledWith('/api/imports/import-1/plan/approval', expect.objectContaining({
      method: 'POST',
      credentials: 'include',
      body: JSON.stringify({ planDigestSha256: readyPlan.planDigestSha256, selectionDigestSha256: selection.selectionDigestSha256 }),
    }));
  });
  it('allows an explicit partial selection to be approved', async () => {
    const user = userEvent.setup();
    const selection = { id: 'selection-1', importId: 'import-1', planId: 'plan-1', planDigestSha256: plan.planDigestSha256,
      baseCommitSha: plan.baseCommitSha, selectionVersion: 'selection-1', selectionDigestSha256: 'd'.repeat(64),
      selectedPaths: ['docs/new.md'], excludedPaths: ['README.md'], overrides: [], createdAt: '2026-08-06T20:29:00Z' };
    const approval = { importId: 'import-1', planId: 'plan-1', planDigestSha256: plan.planDigestSha256,
      selectionDigestSha256: selection.selectionDigestSha256, status: 'APPROVED', approvedAt: '2026-08-06T20:30:00Z' };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => plan })
      .mockResolvedValueOnce({ ok: true, json: async () => selection })
      .mockResolvedValueOnce({ ok: true, json: async () => approval });
    vi.stubGlobal('fetch', fetchMock);
    renderPage();
    await screen.findByText('README.md');
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera README.md' }));
    const approve = screen.getByRole('button', { name: 'Godkänn valda förändringar' });
    expect(approve).toBeEnabled();
    await user.click(approve);
    expect(await screen.findByText('Planen är godkänd')).toBeInTheDocument();
  });


  it('submits the exact partial selection and explicit override audit to the backend', async () => {
    const user = userEvent.setup();
    const mixedPlan = {
      ...plan,
      blocked: 2,
      hardBlocked: 1,
      overridableBlocked: 1,
      entries: [
        ...plan.entries,
        { path: '.git/config', status: 'BLOCKED', comparisonStatus: 'MODIFIED', severity: 'BLOCKING',
          blockerType: 'HARD_BLOCKED', policyCode: 'GIT_METADATA_PROTECTED', message: 'Never deliver Git metadata.',
          archiveSizeBytes: 15, archiveSha256: '7'.repeat(64), repositorySizeBytes: null, repositorySha256: null, textCandidate: true },
      ],
    };
    const selection = {
      id: 'selection-mixed', importId: 'import-1', planId: 'plan-1', planDigestSha256: mixedPlan.planDigestSha256,
      baseCommitSha: mixedPlan.baseCommitSha, selectionVersion: 'selection-1', selectionDigestSha256: 'e'.repeat(64),
      selectedPaths: ['.github/workflows/ci.yml', 'docs/new.md'],
      excludedPaths: ['.git/config', 'README.md', 'src/App.java', '__MACOSX/._README.md'],
      overrides: [{ path: '.github/workflows/ci.yml', blockerType: 'OVERRIDABLE_BLOCKED',
        policyCode: 'GITHUB_WORKFLOW_PROTECTED', acknowledgement: 'User explicitly approved this policy override in the review UI.' }],
      createdAt: '2026-08-07T15:30:00Z',
    };
    const approval = {
      importId: 'import-1', planId: 'plan-1', planDigestSha256: mixedPlan.planDigestSha256,
      selectionDigestSha256: selection.selectionDigestSha256, status: 'APPROVED', approvedAt: '2026-08-07T15:31:00Z',
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => mixedPlan })
      .mockResolvedValueOnce({ ok: true, json: async () => selection })
      .mockResolvedValueOnce({ ok: true, json: async () => approval });
    vi.stubGlobal('fetch', fetchMock);

    renderPage();
    await screen.findByText('README.md');
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera README.md' }));
    await user.click(screen.getByRole('button', { name: 'Blockerade' }));

    expect(screen.getByRole('checkbox', { name: 'Inkludera .git/config' })).toBeDisabled();
    await user.click(screen.getByRole('checkbox', { name: 'Jag förstår risken och vill ta med denna blockerade förändring' }));
    await user.click(screen.getByRole('button', { name: 'Godkänn valda förändringar' }));

    expect(await screen.findByText('Planen är godkänd')).toBeInTheDocument();
    const selectionCall = fetchMock.mock.calls[1];
    expect(selectionCall[0]).toBe('/api/imports/import-1/selection');
    const request = selectionCall[1] as RequestInit;
    const body = JSON.parse(String(request.body));
    expect(body.selectedPaths).toEqual(['.github/workflows/ci.yml', 'docs/new.md']);
    expect(body.overrides).toEqual([{
      path: '.github/workflows/ci.yml',
      acknowledgement: 'User explicitly approved this policy override in the review UI.',
    }]);
    expect(body.selectedPaths).not.toContain('.git/config');
    expect(body.selectedPaths).not.toContain('README.md');
  });

  it('disables approval when the user deselects every committable change', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText('README.md');
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera README.md' }));
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera docs/new.md' }));
    expect(screen.getByRole('button', { name: 'Godkänn valda förändringar' })).toBeDisabled();
    expect(screen.getByText('Välj minst en förändring')).toBeInTheDocument();
  });

  it('requires explicit override before an overridable blocker is selected', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText('README.md');
    await user.click(screen.getByRole('button', { name: 'Blockerade' }));
    const pathCheckbox = screen.getByRole('checkbox', { name: 'Inkludera .github/workflows/ci.yml' });
    expect(pathCheckbox).toBeDisabled();
    await user.click(screen.getByRole('checkbox', { name: 'Jag förstår risken och vill ta med denna blockerade förändring' }));
    expect(screen.getByRole('checkbox', { name: 'Exkludera .github/workflows/ci.yml' })).toBeChecked();
  });

});
