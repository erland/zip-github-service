import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ImportSelectionResponse } from '../api/imports';
import ImportReviewPage from './ImportReviewPage';

const plan = {
  id: 'plan-1', importId: 'import-1', sourceUploadSha256: 'a'.repeat(64), baseCommitSha: 'b'.repeat(40),
  policyVersion: 'mvp-3', planDigestSha256: 'c'.repeat(64), status: 'READY', approvable: true,
  added: 1, modified: 1, unchanged: 1, ignored: 1, blocked: 1, hardBlocked: 0, overridableBlocked: 1,
  warnings: 1, createdAt: '2026-08-06T19:00:00Z',
  entries: [
    { path: 'README.md', status: 'MODIFIED', comparisonStatus: 'MODIFIED', severity: 'NONE', blockerType: 'NONE', policyCode: null, message: null, archiveSizeBytes: 20, archiveSha256: '1'.repeat(64), repositorySizeBytes: 10, repositorySha256: '2'.repeat(64), textCandidate: true },
    { path: 'docs/new.md', status: 'ADDED', comparisonStatus: 'ADDED', severity: 'WARNING', blockerType: 'NONE', policyCode: 'ENVIRONMENT_FILE_WARNING', message: 'Kontrollera filen.', archiveSizeBytes: 12, archiveSha256: '3'.repeat(64), repositorySizeBytes: null, repositorySha256: null, textCandidate: true },
    { path: '.github/workflows/ci.yml', status: 'BLOCKED', comparisonStatus: 'MODIFIED', severity: 'BLOCKING', blockerType: 'OVERRIDABLE_BLOCKED', policyCode: 'GITHUB_WORKFLOW_PROTECTED', message: 'Changes under .github/** are blocked in the MVP.', archiveSizeBytes: 30, archiveSha256: '4'.repeat(64), repositorySizeBytes: 20, repositorySha256: '5'.repeat(64), textCandidate: true },
    { path: 'src/App.java', status: 'UNCHANGED', comparisonStatus: 'UNCHANGED', severity: 'NONE', blockerType: 'NONE', policyCode: null, message: null, archiveSizeBytes: 50, archiveSha256: '6'.repeat(64), repositorySizeBytes: 50, repositorySha256: '6'.repeat(64), textCandidate: true },
    { path: '__MACOSX/._README.md', status: 'IGNORED', comparisonStatus: null, severity: 'NONE', blockerType: 'NONE', policyCode: 'TRANSPORT_NOISE', message: 'Transport metadata is ignored.', archiveSizeBytes: null, archiveSha256: null, repositorySizeBytes: null, repositorySha256: null, textCandidate: false },
  ],
};

const selection: ImportSelectionResponse = {
  id: 'selection-1', importId: 'import-1', planId: 'plan-1', planDigestSha256: plan.planDigestSha256,
  baseCommitSha: plan.baseCommitSha, selectionVersion: 'selection-1', selectionDigestSha256: 'd'.repeat(64),
  selectedPaths: ['README.md', 'docs/new.md'], excludedPaths: [], overrides: [], createdAt: '2026-08-06T20:29:00Z',
};
const approval = {
  importId: 'import-1', planId: 'plan-1', planDigestSha256: plan.planDigestSha256,
  selectionDigestSha256: selection.selectionDigestSha256, status: 'APPROVED', approvedAt: '2026-08-06T20:30:00Z',
};
const workspace = { importId: 'import-1', repositoryFullName: 'erland/repo', baseCommitSha: plan.baseCommitSha,
  planDigestSha256: plan.planDigestSha256, selectionDigestSha256: selection.selectionDigestSha256,
  appliedFileCount: 2, appliedPaths: selection.selectedPaths, status: 'FILES_APPLIED', preparedAt: '2026-08-07T17:00:00Z' };
const delivery = { importId: 'import-1', repositoryFullName: 'erland/repo', baseBranch: 'main', branchName: 'zip-github/work-1',
  baseCommitSha: plan.baseCommitSha, commitSha: 'e'.repeat(40), planDigestSha256: plan.planDigestSha256,
  status: 'PUSHED', pushedAt: '2026-08-07T17:01:00Z' };

function response(body: unknown, status = 200) {
  return Promise.resolve({ ok: status >= 200 && status < 300, status, json: async () => body });
}

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (!init?.method && url.endsWith('/plan')) return response(plan);
    if (!init?.method && (url.endsWith('/selection') || url.endsWith('/plan/approval') || url.endsWith('/delivery'))) return response({}, 404);
    return response({});
  }));
});

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/projects/project-1/imports/import-1/review']}>
      <Routes>
        <Route path="projects/:projectId/imports/:importId/review" element={<ImportReviewPage />} />
        <Route path="projects/:projectId/imports/:importId/result" element={<div>Importresultat</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

function installHappyPath(fetchPlan = plan, createdSelection: ImportSelectionResponse = selection) {
  const mock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (!init?.method && url.endsWith('/plan')) return response(fetchPlan);
    if (!init?.method && (url.endsWith('/selection') || url.endsWith('/plan/approval') || url.endsWith('/delivery'))) return response({}, 404);
    if (init?.method === 'POST' && url.endsWith('/selection')) return response(createdSelection, 201);
    if (init?.method === 'POST' && url.endsWith('/plan/approval')) return response({ ...approval, planDigestSha256: fetchPlan.planDigestSha256, selectionDigestSha256: createdSelection.selectionDigestSha256 });
    if (init?.method === 'POST' && url.endsWith('/workspace')) return response(workspace);
    if (init?.method === 'POST' && url.endsWith('/delivery')) return response(delivery);
    return response({});
  });
  vi.stubGlobal('fetch', mock);
  return mock;
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
    const user = userEvent.setup(); renderPage(); await screen.findByText('README.md');
    await user.click(screen.getByRole('button', { name: 'Blockerade' }));
    const list = screen.getByRole('list', { name: 'Filträd' });
    expect(within(list).getByTitle('.github/workflows/ci.yml')).toBeInTheDocument();
    expect(within(list).queryByText('README.md')).not.toBeInTheDocument();
  });

  it('uses one click to lock selection, approve, prepare workspace, deliver and open the result', async () => {
    const user = userEvent.setup();
    const readyPlan = { ...plan, blocked: 0, hardBlocked: 0, overridableBlocked: 0, entries: plan.entries.filter((e) => e.status !== 'BLOCKED') };
    const readySelection = { ...selection, planDigestSha256: readyPlan.planDigestSha256 };
    const fetchMock = installHappyPath(readyPlan, readySelection);
    renderPage();
    await user.click(await screen.findByRole('button', { name: 'Godkänn valda förändringar' }));
    expect(await screen.findByText('Importresultat')).toBeInTheDocument();
    const postUrls = fetchMock.mock.calls.filter(([, init]) => (init as RequestInit | undefined)?.method === 'POST').map(([url]) => String(url));
    expect(postUrls).toEqual([
      '/api/imports/import-1/selection',
      '/api/imports/import-1/plan/approval',
      '/api/imports/import-1/workspace',
      '/api/imports/import-1/delivery',
    ]);
  });

  it('submits the exact partial selection before the same-click delivery', async () => {
    const user = userEvent.setup();
    const partial = { ...selection, selectedPaths: ['docs/new.md'], excludedPaths: ['README.md'] };
    const fetchMock = installHappyPath(plan, partial);
    renderPage(); await screen.findByText('README.md');
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera README.md' }));
    await user.click(screen.getByRole('button', { name: 'Godkänn valda förändringar' }));
    expect(await screen.findByText('Importresultat')).toBeInTheDocument();
    const selectionCall = fetchMock.mock.calls.find(([url, init]) => String(url).endsWith('/selection') && (init as RequestInit | undefined)?.method === 'POST');
    const body = JSON.parse(String((selectionCall?.[1] as RequestInit).body));
    expect(body.selectedPaths).toEqual(['docs/new.md']);
  });

  it('submits explicit override audit and never includes a hard blocker', async () => {
    const user = userEvent.setup();
    const mixedPlan = { ...plan, blocked: 2, hardBlocked: 1, overridableBlocked: 1, entries: [...plan.entries,
      { path: '.git/config', status: 'BLOCKED', comparisonStatus: 'MODIFIED', severity: 'BLOCKING', blockerType: 'HARD_BLOCKED', policyCode: 'GIT_METADATA_PROTECTED', message: 'Never deliver Git metadata.', archiveSizeBytes: 15, archiveSha256: '7'.repeat(64), repositorySizeBytes: null, repositorySha256: null, textCandidate: true }] };
    const mixedSelection = { ...selection, selectionDigestSha256: 'f'.repeat(64), selectedPaths: ['.github/workflows/ci.yml', 'docs/new.md'], overrides: [{ path: '.github/workflows/ci.yml', blockerType: 'OVERRIDABLE_BLOCKED', policyCode: 'GITHUB_WORKFLOW_PROTECTED', acknowledgement: 'User explicitly approved this policy override in the review UI.' }] };
    const fetchMock = installHappyPath(mixedPlan, mixedSelection);
    renderPage(); await screen.findByText('README.md');
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera README.md' }));
    await user.click(screen.getByRole('button', { name: 'Blockerade' }));
    expect(screen.getByRole('checkbox', { name: 'Inkludera .git/config' })).toBeDisabled();
    await user.click(screen.getByRole('checkbox', { name: 'Jag förstår risken och vill ta med denna blockerade förändring' }));
    await user.click(screen.getByRole('button', { name: 'Godkänn valda förändringar' }));
    expect(await screen.findByText('Importresultat')).toBeInTheDocument();
    const selectionCall = fetchMock.mock.calls.find(([url, init]) => String(url).endsWith('/selection') && (init as RequestInit | undefined)?.method === 'POST');
    const body = JSON.parse(String((selectionCall?.[1] as RequestInit).body));
    expect(body.selectedPaths).toEqual(['.github/workflows/ci.yml', 'docs/new.md']);
    expect(body.overrides).toEqual([{ path: '.github/workflows/ci.yml', acknowledgement: 'User explicitly approved this policy override in the review UI.' }]);
    expect(body.selectedPaths).not.toContain('.git/config');
  });

  it('disables approval when the user deselects every committable change', async () => {
    const user = userEvent.setup(); renderPage(); await screen.findByText('README.md');
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera README.md' }));
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera docs/new.md' }));
    expect(screen.getByRole('button', { name: 'Godkänn valda förändringar' })).toBeDisabled();
    expect(screen.getByText('Välj minst en förändring')).toBeInTheDocument();
  });

  it('requires explicit override before an overridable blocker is selected', async () => {
    const user = userEvent.setup(); renderPage(); await screen.findByText('README.md');
    await user.click(screen.getByRole('button', { name: 'Blockerade' }));
    expect(screen.getByRole('checkbox', { name: 'Inkludera .github/workflows/ci.yml' })).toBeDisabled();
    await user.click(screen.getByRole('checkbox', { name: 'Jag förstår risken och vill ta med denna blockerade förändring' }));
    expect(screen.getByRole('checkbox', { name: 'Exkludera .github/workflows/ci.yml' })).toBeChecked();
  });


  it('retries delivery after approval without creating a second selection or approval', async () => {
    const user = userEvent.setup();
    const readyPlan = { ...plan, blocked: 0, hardBlocked: 0, overridableBlocked: 0, entries: plan.entries.filter((e) => e.status !== 'BLOCKED') };
    const readySelection = { ...selection, planDigestSha256: readyPlan.planDigestSha256 };
    let deliveryAttempts = 0;
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (!init?.method && url.endsWith('/plan')) return response(readyPlan);
      if (!init?.method && (url.endsWith('/selection') || url.endsWith('/plan/approval') || url.endsWith('/delivery'))) return response({}, 404);
      if (init?.method === 'POST' && url.endsWith('/selection')) return response(readySelection, 201);
      if (init?.method === 'POST' && url.endsWith('/plan/approval')) return response({ ...approval, planDigestSha256: readyPlan.planDigestSha256, selectionDigestSha256: readySelection.selectionDigestSha256 });
      if (init?.method === 'POST' && url.endsWith('/workspace')) return response(workspace);
      if (init?.method === 'POST' && url.endsWith('/delivery')) {
        deliveryAttempts += 1;
        return deliveryAttempts === 1 ? Promise.reject(new Error('push failed')) : response(delivery);
      }
      return response({});
    });
    vi.stubGlobal('fetch', fetchMock);
    renderPage();

    await user.click(await screen.findByRole('button', { name: 'Godkänn valda förändringar' }));
    expect(await screen.findByText('push failed')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Godkänn valda förändringar' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Försök skapa commit igen' }));
    expect(await screen.findByText('Importresultat')).toBeInTheDocument();

    const postUrls = fetchMock.mock.calls.filter(([, init]) => (init as RequestInit | undefined)?.method === 'POST').map(([url]) => String(url));
    expect(postUrls.filter((url) => url.endsWith('/selection'))).toHaveLength(1);
    expect(postUrls.filter((url) => url.endsWith('/plan/approval'))).toHaveLength(1);
    expect(postUrls.filter((url) => url.endsWith('/delivery'))).toHaveLength(2);
  });

  it('restores an existing approval after refresh and offers only the recovery delivery action', async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (!init?.method && url.endsWith('/plan')) return response(plan);
      if (!init?.method && url.endsWith('/selection')) return response(selection);
      if (!init?.method && url.endsWith('/plan/approval')) return response(approval);
      if (!init?.method && url.endsWith('/delivery')) return response({}, 404);
      if (init?.method === 'POST' && url.endsWith('/workspace')) return response(workspace);
      if (init?.method === 'POST' && url.endsWith('/delivery')) return response(delivery);
      return response({});
    });
    vi.stubGlobal('fetch', fetchMock);
    renderPage();
    expect(await screen.findByText('Förändringarna är godkända')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Godkänn valda förändringar' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Försök skapa commit igen' }));
    expect(await screen.findByText('Importresultat')).toBeInTheDocument();
  });
});
