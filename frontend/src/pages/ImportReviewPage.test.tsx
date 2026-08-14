import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ImportPlanEntry, ImportPlanResponse, ImportSelectionResponse } from '../api/imports';
import ImportReviewPage from './ImportReviewPage';

const plan: ImportPlanResponse = {
  id: 'plan-1', importId: 'import-1', sourceUploadSha256: 'a'.repeat(64), baseCommitSha: 'b'.repeat(40),
  policyVersion: 'mvp-4', planDigestSha256: 'c'.repeat(64), status: 'READY', approvable: true,
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

function planEntry(entry: ImportPlanEntry): ImportPlanEntry {
  return entry;
}

const selection: ImportSelectionResponse = {
  id: 'selection-1', importId: 'import-1', planId: 'plan-1', planDigestSha256: plan.planDigestSha256,
  baseCommitSha: plan.baseCommitSha, selectionVersion: 'selection-2', selectionDigestSha256: 'd'.repeat(64),
  selectedPaths: ['README.md', 'docs/new.md'], excludedPaths: [], overrides: [], blockerDecisions: [], createdAt: '2026-08-06T20:29:00Z',
};
const approval = {
  importId: 'import-1', planId: 'plan-1', planDigestSha256: plan.planDigestSha256,
  selectionDigestSha256: selection.selectionDigestSha256, commitMessage: 'Apply approved ZIP import import-1', status: 'APPROVED', approvedAt: '2026-08-06T20:30:00Z',
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
        <Route path="projects/:projectId" element={<div>Projektvy</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

async function excludeBaseBlocker(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('button', { name: /Blockerade/ }));
  await user.click(screen.getByRole('radio', { name: 'Ta inte med' }));
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
    expect(await screen.findByText('1 blockerande förändring kräver beslut')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Behöver din uppmärksamhet' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Granska blockerade (1)' })).toBeInTheDocument();
    const ordinarySection = screen.getByRole('heading', { name: 'Vanliga ändringar' }).closest('section');
    expect(ordinarySection).not.toBeNull();
    expect(within(ordinarySection as HTMLElement).getByText((_, element) =>
      element?.tagName === 'P' && element.textContent === '2 vanliga filförändringar är valbara enligt ordinarie regler.',
    )).toBeInTheDocument();
    expect(screen.getByText('README.md')).toBeInTheDocument();
    expect(screen.getByTitle('docs/new.md')).toBeInTheDocument();
    expect(screen.queryByTitle('.github/workflows/ci.yml')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Godkänn valda förändringar' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Fortsätt till commit' })).not.toBeInTheDocument();
  });

  it('switches from blocker guidance to continue-to-commit when required decisions are resolved', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByRole('heading', { name: 'Behöver din uppmärksamhet' });

    expect(screen.queryByRole('button', { name: 'Fortsätt till commit' })).not.toBeInTheDocument();
    await excludeBaseBlocker(user);

    const continueButton = screen.getByRole('button', { name: 'Fortsätt till commit' });
    expect(continueButton).toBeInTheDocument();
    expect(screen.getByText(/2 förändringar är valda/)).toBeInTheDocument();

    await user.click(continueButton);
    expect(screen.getByRole('textbox', { name: 'Meddelande' })).toHaveFocus();
    expect(screen.getByRole('button', { name: 'Godkänn valda förändringar' })).toBeDisabled();
  });

  it('lets the attention panel jump directly to blocked decisions', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByRole('heading', { name: 'Behöver din uppmärksamhet' });

    await user.click(screen.getByRole('button', { name: 'Granska blockerade (1)' }));

    const list = screen.getByRole('list', { name: 'Filträd' });
    expect(within(list).getByTitle('.github/workflows/ci.yml')).toBeInTheDocument();
    expect(within(list).queryByText('README.md')).not.toBeInTheDocument();
  });

  it('presents a clean plan as low-attention while keeping full file review available', async () => {
    const cleanPlan: ImportPlanResponse = {
      ...plan,
      blocked: 0,
      hardBlocked: 0,
      overridableBlocked: 0,
      warnings: 0,
      entries: plan.entries
        .filter((entry) => entry.blockerType === 'NONE' && entry.status !== 'IGNORED')
        .map((entry) => ({ ...entry, severity: 'NONE' as const, policyCode: null, message: null })),
    };
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (!init?.method && url.endsWith('/plan')) return response(cleanPlan);
      if (!init?.method && (url.endsWith('/selection') || url.endsWith('/plan/approval') || url.endsWith('/delivery'))) return response({}, 404);
      return response({});
    }));

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Inga särskilda risker hittades' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Granska blockerade/ })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Visa vanliga förändringar/ })).toBeInTheDocument();
    expect(screen.getByText('README.md')).toBeInTheDocument();
  });

  it('warns when the ZIP overlaps changes made on the Work branch after the last zip-github commit', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (!init?.method && url.endsWith('/plan')) return response(plan);
      if (!init?.method && url.endsWith('/external-branch-changes')) return response({ branchChanged: true, previousKnownHeadSha: '1'.repeat(40), reviewBaseHeadSha: '2'.repeat(40), changedPaths: ['README.md', 'server-only.txt'] });
      if (!init?.method && (url.endsWith('/selection') || url.endsWith('/plan/approval') || url.endsWith('/delivery'))) return response({}, 404);
      return response({});
    }));
    renderPage();
    expect(await screen.findByLabelText('GitHub-branchen har ändrats')).toHaveTextContent('1 fil');
    expect(screen.getByText('Ändrad på GitHub')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Godkänn valda förändringar' })).toBeDisabled();
    await user.type(screen.getByRole('textbox', { name: 'Meddelande' }), 'Preserve reviewed external changes');
    expect(screen.getByRole('button', { name: 'Godkänn valda förändringar' })).toBeDisabled();
    await user.click(screen.getByRole('checkbox', { name: /Jag förstår att 1 vald sökväg ersätter ändringar/ }));
    await excludeBaseBlocker(user);
    expect(screen.getByRole('button', { name: 'Godkänn valda förändringar' })).toBeEnabled();
    await user.click(screen.getByRole('button', { name: /Externa ändringar \(1\)/ }));
    expect(screen.getByText('README.md')).toBeInTheDocument();
    expect(screen.queryByTitle('docs/new.md')).not.toBeInTheDocument();
  });
  it('presents gitignored ZIP files as informational warnings and keeps filters as the only buttons', async () => {
    const ignoredPlan = { ...plan, ignored: 2, warnings: 2, entries: [...plan.entries,
      { path: 'shortcut/releases/zip-github.shortcut', status: 'IGNORED', comparisonStatus: 'IGNORED', severity: 'WARNING', blockerType: 'NONE', policyCode: 'GITIGNORE_IGNORED', message: 'Filen matchar repositoryts .gitignore och kommer inte att tas med i Git-committen.', archiveSizeBytes: 23821, archiveSha256: '7'.repeat(64), repositorySizeBytes: null, repositorySha256: null, textCandidate: false }] };
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (!init?.method && url.endsWith('/plan')) return response(ignoredPlan);
      if (!init?.method && (url.endsWith('/selection') || url.endsWith('/plan/approval') || url.endsWith('/delivery'))) return response({}, 404);
      return response({});
    }));
    const user = userEvent.setup();
    renderPage();
    const summary = await screen.findByLabelText('Sammanfattning av importplanen');
    expect(summary).toHaveTextContent('2 ignorerade');
    expect(within(summary).queryByRole('button')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Ignorerade \(2\)/ }));
    expect(screen.getByTitle('shortcut/releases/zip-github.shortcut')).toBeInTheDocument();
    expect(screen.getByText(/matchar repositoryts .gitignore/i)).toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: /shortcut\/releases\/zip-github.shortcut/ })).not.toBeInTheDocument();
  });

  it('filters to blocked entries', async () => {
    const user = userEvent.setup(); renderPage(); await screen.findByText('README.md');
    await user.click(screen.getByRole('button', { name: /Blockerade/ }));
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
    await user.type(await screen.findByRole('textbox', { name: 'Meddelande' }), 'Deliver reviewed changes');
    await user.click(screen.getByRole('button', { name: 'Godkänn valda förändringar' }));
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
    await user.type(screen.getByRole('textbox', { name: 'Meddelande' }), 'Deliver selected documentation change');
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera README.md' }));
    await excludeBaseBlocker(user);
    await user.click(screen.getByRole('button', { name: 'Godkänn valda förändringar' }));
    expect(await screen.findByText('Importresultat')).toBeInTheDocument();
    const selectionCall = fetchMock.mock.calls.find(([url, init]) => String(url).endsWith('/selection') && (init as RequestInit | undefined)?.method === 'POST');
    const body = JSON.parse(String((selectionCall?.[1] as RequestInit).body));
    expect(body.selectedPaths).toEqual(['docs/new.md']);
    expect(body.blockerDecisions).toEqual([{ path: '.github/workflows/ci.yml', decision: 'EXCLUDE' }]);
  });

  it('bulk-approves and selects every overridable entry in the active category without including hard blockers', async () => {
    const user = userEvent.setup();
    const bulkPlan = { ...plan, blocked: 4, hardBlocked: 1, overridableBlocked: 3, entries: [
      ...plan.entries,
      planEntry({ path: 'output/old-a.pdf', status: 'BLOCKED', comparisonStatus: 'WOULD_DELETE', severity: 'BLOCKING', blockerType: 'OVERRIDABLE_BLOCKED', policyCode: 'DELETION_REQUIRES_OVERRIDE', message: 'Deletion requires override.', archiveSizeBytes: null, archiveSha256: null, repositorySizeBytes: 100, repositorySha256: '8'.repeat(64), textCandidate: false }),
      planEntry({ path: 'release/old-b.zip', status: 'BLOCKED', comparisonStatus: 'WOULD_DELETE', severity: 'BLOCKING', blockerType: 'OVERRIDABLE_BLOCKED', policyCode: 'DELETION_REQUIRES_OVERRIDE', message: 'Deletion requires override.', archiveSizeBytes: null, archiveSha256: null, repositorySizeBytes: 100, repositorySha256: '9'.repeat(64), textCandidate: false }),
      planEntry({ path: '.git/config', status: 'BLOCKED', comparisonStatus: 'MODIFIED', severity: 'BLOCKING', blockerType: 'HARD_BLOCKED', policyCode: 'GIT_METADATA_PROTECTED', message: 'Never deliver Git metadata.', archiveSizeBytes: 15, archiveSha256: '7'.repeat(64), repositorySizeBytes: null, repositorySha256: null, textCandidate: true }),
    ] };
    const bulkSelection = { ...selection, selectionDigestSha256: 'e'.repeat(64), selectedPaths: ['.github/workflows/ci.yml', 'README.md', 'docs/new.md', 'output/old-a.pdf', 'release/old-b.zip'], overrides: [
      { path: '.github/workflows/ci.yml', blockerType: 'OVERRIDABLE_BLOCKED', policyCode: 'GITHUB_WORKFLOW_PROTECTED', acknowledgement: 'User explicitly approved this policy override in the review UI.' },
      { path: 'output/old-a.pdf', blockerType: 'OVERRIDABLE_BLOCKED', policyCode: 'DELETION_REQUIRES_OVERRIDE', acknowledgement: 'User explicitly approved this policy override in the review UI.' },
      { path: 'release/old-b.zip', blockerType: 'OVERRIDABLE_BLOCKED', policyCode: 'DELETION_REQUIRES_OVERRIDE', acknowledgement: 'User explicitly approved this policy override in the review UI.' },
    ] };
    const fetchMock = installHappyPath(bulkPlan, bulkSelection);
    renderPage();
    await screen.findByText('README.md');
    await user.click(screen.getByRole('button', { name: /Blockerade \(4\)/ }));
    const bulkOverride = screen.getByRole('checkbox', { name: /godkänna och välja alla 3 överstyrbara förändringar/i });
    await user.click(bulkOverride);
    expect(screen.getByRole('checkbox', { name: 'Exkludera .github/workflows/ci.yml' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Exkludera output/old-a.pdf' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Exkludera release/old-b.zip' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Inkludera .git/config' })).toBeDisabled();
    await user.click(screen.getByRole('checkbox', { name: 'Jag har sett att denna hårt blockerade förändring inte kommer att tas med' }));
    await user.type(screen.getByRole('textbox', { name: 'Meddelande' }), 'Remove generated repository artifacts');
    await user.click(screen.getByRole('button', { name: 'Godkänn valda förändringar' }));
    await screen.findByText('Importresultat');
    const selectionCall = fetchMock.mock.calls.find(([url, init]) => String(url).endsWith('/selection') && (init as RequestInit | undefined)?.method === 'POST');
    const body = JSON.parse(String((selectionCall?.[1] as RequestInit).body));
    expect(body.selectedPaths).toEqual(['.github/workflows/ci.yml', 'README.md', 'docs/new.md', 'output/old-a.pdf', 'release/old-b.zip']);
    expect(body.selectedPaths).not.toContain('.git/config');
    expect(body.blockerDecisions).toContainEqual({ path: '.github/workflows/ci.yml', decision: 'INCLUDE_OVERRIDE' });
    expect(body.blockerDecisions).toContainEqual({ path: '.git/config', decision: 'ACKNOWLEDGE_EXCLUSION' });
    expect(body.overrides).toHaveLength(3);
    expect(body.blockerDecisions).toHaveLength(4);
    expect(body.blockerDecisions).toContainEqual({ path: '.git/config', decision: 'ACKNOWLEDGE_EXCLUSION' });
  });

  it('submits explicit override audit and never includes a hard blocker', async () => {
    const user = userEvent.setup();
    const mixedPlan = { ...plan, blocked: 2, hardBlocked: 1, overridableBlocked: 1, entries: [...plan.entries,
      planEntry({ path: '.git/config', status: 'BLOCKED', comparisonStatus: 'MODIFIED', severity: 'BLOCKING', blockerType: 'HARD_BLOCKED', policyCode: 'GIT_METADATA_PROTECTED', message: 'Never deliver Git metadata.', archiveSizeBytes: 15, archiveSha256: '7'.repeat(64), repositorySizeBytes: null, repositorySha256: null, textCandidate: true })] };
    const mixedSelection = { ...selection, selectionDigestSha256: 'f'.repeat(64), selectedPaths: ['.github/workflows/ci.yml', 'docs/new.md'], overrides: [{ path: '.github/workflows/ci.yml', blockerType: 'OVERRIDABLE_BLOCKED', policyCode: 'GITHUB_WORKFLOW_PROTECTED', acknowledgement: 'User explicitly approved this policy override in the review UI.' }] };
    const fetchMock = installHappyPath(mixedPlan, mixedSelection);
    renderPage(); await screen.findByText('README.md');
    await user.type(screen.getByRole('textbox', { name: 'Meddelande' }), 'Deliver approved workflow override');
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera README.md' }));
    await user.click(screen.getByRole('button', { name: /Blockerade/ }));
    expect(screen.getByRole('checkbox', { name: 'Inkludera .git/config' })).toBeDisabled();
    await user.click(screen.getByRole('radio', { name: 'Jag förstår risken – godkänn och ta med' }));
    await user.click(screen.getByRole('checkbox', { name: 'Jag har sett att denna hårt blockerade förändring inte kommer att tas med' }));
    await user.click(screen.getByRole('button', { name: 'Godkänn valda förändringar' }));
    expect(await screen.findByText('Importresultat')).toBeInTheDocument();
    const selectionCall = fetchMock.mock.calls.find(([url, init]) => String(url).endsWith('/selection') && (init as RequestInit | undefined)?.method === 'POST');
    const body = JSON.parse(String((selectionCall?.[1] as RequestInit).body));
    expect(body.selectedPaths).toEqual(['.github/workflows/ci.yml', 'docs/new.md']);
    expect(body.overrides).toEqual([{ path: '.github/workflows/ci.yml', acknowledgement: 'User explicitly approved this policy override in the review UI.' }]);
    expect(body.selectedPaths).not.toContain('.git/config');
    expect(body.blockerDecisions).toContainEqual({ path: '.github/workflows/ci.yml', decision: 'INCLUDE_OVERRIDE' });
    expect(body.blockerDecisions).toContainEqual({ path: '.git/config', decision: 'ACKNOWLEDGE_EXCLUSION' });
  });

  it('disables approval when the user deselects every committable change', async () => {
    const user = userEvent.setup(); renderPage(); await screen.findByText('README.md');
    await excludeBaseBlocker(user);
    await user.click(screen.getByRole('button', { name: /Förändringar/ }));
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera README.md' }));
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera docs/new.md' }));
    expect(screen.getByRole('button', { name: 'Godkänn valda förändringar' })).toBeDisabled();
    expect(screen.getByText('Välj minst en förändring')).toBeInTheDocument();
  });

  it('requires explicit override before an overridable blocker is selected', async () => {
    const user = userEvent.setup(); renderPage(); await screen.findByText('README.md');
    await user.click(screen.getByRole('button', { name: /Blockerade/ }));
    expect(screen.getByRole('checkbox', { name: 'Inkludera .github/workflows/ci.yml' })).toBeDisabled();
    await user.click(screen.getByRole('radio', { name: 'Jag förstår risken – godkänn och ta med' }));
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

    await user.type(await screen.findByRole('textbox', { name: 'Meddelande' }), 'Retry delivery without duplicate approval');
    await user.click(screen.getByRole('button', { name: 'Godkänn valda förändringar' }));
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
  it('requires the user to enter an explicit commit message and submits it with approval', async () => {
    const user = userEvent.setup();
    const fetchMock = installHappyPath();
    renderPage();
    const field = await screen.findByRole('textbox', { name: 'Meddelande' });
    expect(field).toHaveValue('');
    await user.type(field, 'Preserve executable script modes');
    await excludeBaseBlocker(user);
    await user.click(screen.getByRole('button', { name: 'Godkänn valda förändringar' }));
    await screen.findByText('Importresultat');
    const approvalCall = fetchMock.mock.calls.find(([url, init]) => String(url).endsWith('/plan/approval') && (init as RequestInit | undefined)?.method === 'POST');
    expect(approvalCall).toBeTruthy();
    expect(JSON.parse(String((approvalCall![1] as RequestInit).body))).toMatchObject({ commitMessage: 'Preserve executable script modes' });
  });

  it('does not allow approval with a blank commit message', async () => {
    const user = userEvent.setup(); renderPage();
    const field = await screen.findByRole('textbox', { name: 'Meddelande' });
    await excludeBaseBlocker(user);
    await user.clear(field);
    expect(screen.getByRole('button', { name: 'Godkänn valda förändringar' })).toBeDisabled();
  });

  it('cancels an active review explicitly without creating selection, approval or delivery', async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (!init?.method && url.endsWith('/plan')) return response(plan);
      if (!init?.method && (url.endsWith('/selection') || url.endsWith('/plan/approval') || url.endsWith('/delivery'))) return response({}, 404);
      if (init?.method === 'POST' && url.endsWith('/cancel')) return response({ id: 'import-1', projectId: 'project-1', baseBranch: 'main', status: 'CANCELLED', createdAt: '2026-08-06T19:00:00Z' });
      return response({});
    });
    vi.stubGlobal('fetch', fetchMock);
    renderPage();
    await screen.findByText('README.md');

    await user.click(screen.getByRole('button', { name: 'Avbryt import' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Avbryt importen?');
    await user.click(screen.getByRole('button', { name: 'Ja, avbryt import' }));
    expect(await screen.findByText('Projektvy')).toBeInTheDocument();

    const postUrls = fetchMock.mock.calls.filter(([, init]) => (init as RequestInit | undefined)?.method === 'POST').map(([url]) => String(url));
    expect(postUrls).toEqual(['/api/imports/import-1/cancel']);
  });

});
