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
  policyVersion: 'mvp-1',
  planDigestSha256: 'c'.repeat(64),
  status: 'DRAFT',
  approvable: false,
  added: 1,
  modified: 1,
  unchanged: 1,
  ignored: 1,
  blocked: 1,
  warnings: 1,
  createdAt: '2026-08-06T19:00:00Z',
  entries: [
    { path: 'README.md', status: 'MODIFIED', comparisonStatus: 'MODIFIED', severity: 'NONE', policyCode: null, message: null, archiveSizeBytes: 20, archiveSha256: '1'.repeat(64), repositorySizeBytes: 10, repositorySha256: '2'.repeat(64), textCandidate: true },
    { path: 'docs/new.md', status: 'ADDED', comparisonStatus: 'ADDED', severity: 'WARNING', policyCode: 'ENVIRONMENT_FILE_WARNING', message: 'Kontrollera filen.', archiveSizeBytes: 12, archiveSha256: '3'.repeat(64), repositorySizeBytes: null, repositorySha256: null, textCandidate: true },
    { path: '.github/workflows/ci.yml', status: 'BLOCKED', comparisonStatus: 'MODIFIED', severity: 'BLOCKING', policyCode: 'GITHUB_WORKFLOW_PROTECTED', message: 'Changes under .github/** are blocked in the MVP.', archiveSizeBytes: 30, archiveSha256: '4'.repeat(64), repositorySizeBytes: 20, repositorySha256: '5'.repeat(64), textCandidate: true },
    { path: 'src/App.java', status: 'UNCHANGED', comparisonStatus: 'UNCHANGED', severity: 'NONE', policyCode: null, message: null, archiveSizeBytes: 50, archiveSha256: '6'.repeat(64), repositorySizeBytes: 50, repositorySha256: '6'.repeat(64), textCandidate: true },
    { path: '__MACOSX/._README.md', status: 'IGNORED', comparisonStatus: null, severity: 'NONE', policyCode: 'TRANSPORT_NOISE', message: 'Transport metadata is ignored.', archiveSizeBytes: null, archiveSha256: null, repositorySizeBytes: null, repositorySha256: null, textCandidate: false },
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
    expect(await screen.findByText('Planen är blockerad')).toBeInTheDocument();
    expect(screen.getByText('README.md')).toBeInTheDocument();
    expect(screen.getByText('docs/new.md')).toBeInTheDocument();
    expect(screen.queryByText('.github/workflows/ci.yml')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Godkänn exakt plan' })).toBeDisabled();
  });

  it('filters to blocked entries', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText('README.md');
    await user.click(screen.getByRole('button', { name: 'Blockerade' }));
    const list = screen.getByRole('list', { name: 'Filposter' });
    expect(within(list).getByText('.github/workflows/ci.yml')).toBeInTheDocument();
    expect(within(list).queryByText('README.md')).not.toBeInTheDocument();
  });

  it('approves the exact digest for an approvable plan', async () => {
    const user = userEvent.setup();
    const readyPlan = { ...plan, status: 'READY', approvable: true, blocked: 0,
      entries: plan.entries.filter((entry) => entry.status !== 'BLOCKED') };
    const approval = { importId: 'import-1', planId: 'plan-1', planDigestSha256: readyPlan.planDigestSha256,
      status: 'APPROVED', approvedAt: '2026-08-06T20:30:00Z' };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => readyPlan })
      .mockResolvedValueOnce({ ok: true, json: async () => approval });
    vi.stubGlobal('fetch', fetchMock);

    renderPage();
    const button = await screen.findByRole('button', { name: 'Godkänn exakt plan' });
    expect(button).toBeEnabled();
    await user.click(button);

    expect(await screen.findByText('Planen är godkänd')).toBeInTheDocument();
    expect(fetchMock).toHaveBeenLastCalledWith('/api/imports/import-1/plan/approval', expect.objectContaining({
      method: 'POST',
      credentials: 'include',
      body: JSON.stringify({ planDigestSha256: readyPlan.planDigestSha256 }),
    }));
  });
});
