import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ProjectDetailPage from './ProjectDetailPage';

afterEach(() => cleanup());

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url.endsWith('/imports')) return new Response(JSON.stringify([
      { id: 'import-result', projectId: 'project-1', baseBranch: 'main', status: 'PUSHED', createdAt: '2026-08-06T20:00:00Z', sourceFilename: 'book.zip', sourceSizeBytes: 100, sourceType: 'STORED_UPLOAD', sourceReference: 'stored-upload:123', planDigestSha256: 'a'.repeat(64), pullRequestNumber: null, pullRequestUrl: null, resumeStage: 'RESULT' },
      { id: 'import-review', projectId: 'project-1', baseBranch: 'main', status: 'READY_FOR_REVIEW', createdAt: '2026-08-06T21:00:00Z', sourceFilename: 'draft.zip', sourceSizeBytes: 90, sourceType: 'WEB_UPLOAD', sourceReference: null, planDigestSha256: 'b'.repeat(64), pullRequestNumber: null, pullRequestUrl: null, resumeStage: 'REVIEW' },
      { id: 'import-old-upload', projectId: 'project-1', baseBranch: 'main', status: 'UPLOADED', createdAt: '2026-08-06T19:00:00Z', sourceFilename: 'older-draft.zip', sourceSizeBytes: 80, sourceType: 'WEB_UPLOAD', sourceReference: null, planDigestSha256: null, pullRequestNumber: null, pullRequestUrl: null, resumeStage: 'UPLOAD' },
    ]), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/work/actions/details')) return new Response(JSON.stringify({ importId: 'import-result', repositoryFullName: 'owner/repo', commitSha: '1234567890abcdef', detailsUrl: 'https://github.com/owner/repo/actions', artifacts: [], failures: [{ workflowRunId: 7, workflowName: 'CI', jobId: 8, jobName: 'backend', stepName: 'test', tool: 'maven', lines: ['BUILD FAILURE', 'Tests failed'], contextLines: ['Running tests', 'BUILD FAILURE', 'Tests failed'], jobLogLines: ['setup', 'Running tests', 'BUILD FAILURE', 'Tests failed'], logTruncated: false, githubUrl: 'https://github.com/owner/repo/actions/runs/7' }], checkedAt: '2026-08-08T15:00:01Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/api/imports/import-result/actions/control')) return new Response(JSON.stringify({ importId: 'import-result', repositoryFullName: 'owner/repo', branchRef: 'zip-github/work-1', commitSha: '1234567890abcdef', currentWork: true, disabledReason: null, workflows: [{ identifier: '.github/workflows/ci.yml', workflowId: 70, name: 'CI', path: '.github/workflows/ci.yml', htmlUrl: 'https://github.com/owner/repo/actions/workflows/ci.yml', dispatchAllowed: true, rerunAllowed: true }] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/work/actions')) return new Response(JSON.stringify({ importId: 'import-result', repositoryFullName: 'owner/repo', commitSha: '1234567890abcdef', state: 'failure', terminal: true, detailsUrl: 'https://github.com/owner/repo/actions', workflows: [{ id: 7, workflowId: 70, workflowPath: '.github/workflows/ci.yml', headBranch: 'zip-github/work-1', headSha: '1234567890abcdef', name: 'CI', state: 'failure', terminal: true, event: 'push', htmlUrl: 'https://github.com/owner/repo/actions/runs/7', createdAt: '2026-08-08T15:00:00Z', updatedAt: '2026-08-08T15:00:01Z', jobs: [{ id: 8, name: 'backend', state: 'failure', terminal: true, htmlUrl: 'https://github.com/owner/repo/actions/runs/7/job/8', startedAt: '2026-08-08T15:00:00Z', completedAt: '2026-08-08T15:00:01Z' }] }], checks: [], checkedAt: '2026-08-08T15:00:01Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/work/commits')) return new Response(JSON.stringify({ githubAvailable: true, commits: [
      { sha: '1234567890abcdef', message: 'Update game board\n\nDetails', authorName: 'Erland', authorEmail: 'e@example.test', authoredAt: '2026-08-06T20:00:00Z', htmlUrl: 'https://github.com/owner/repo/commit/1234567890abcdef', fallback: false },
    ] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/work')) return new Response(JSON.stringify({ id: 'work-1', projectId: 'project-1', baseBranch: 'main', branchName: 'zip-github/work-1', status: 'ACTIVE', headCommitSha: '1234567890abcdef', lastImportId: 'import-result', pullRequestNumber: null, pullRequestUrl: null, createdAt: '2026-08-06T18:30:00Z', updatedAt: '2026-08-06T20:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    return new Response(JSON.stringify({ id: 'project-1', name: 'Bokprojekt', githubInstallationId: 1, githubRepositoryId: 2, repositoryFullName: 'owner/repo', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-06T18:00:00Z', updatedAt: '2026-08-06T18:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  }));
});

describe('ProjectDetailPage work history', () => {
  it('shows Git commits and only the active import in the primary project view', async () => {
    render(<MemoryRouter initialEntries={['/projects/project-1']}><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'repo' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Commits i arbetet' })).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'GitHub Actions' })).toBeInTheDocument();
    expect(await screen.findByText('Fel och jobbloggar')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Uppdatera status' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Kopiera fel med sammanhang' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Kör workflow' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Kör om misslyckade jobb' })).toBeInTheDocument();
    expect(screen.getByText('Update game board')).toBeInTheDocument();
    const history = screen.getByRole('heading', { name: 'Commits i arbetet' }).closest('section');
    expect(history).not.toBeNull();
    expect(within(history as HTMLElement).getByText(/1234567890ab/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Öppna commit' })).toHaveAttribute('href', 'https://github.com/owner/repo/commit/1234567890abcdef');
    expect(screen.getByRole('heading', { name: 'draft.zip' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Fortsätt granska' })).toHaveAttribute('href', '/projects/project-1/imports/import-review/review');
    expect(screen.getByRole('button', { name: 'Avbryt import' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Ladda upp nästa ZIP' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Fortsätt arbete' })).not.toBeInTheDocument();
    expect(screen.queryByText('book.zip')).not.toBeInTheDocument();
    expect(screen.queryByText('older-draft.zip')).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Importhistorik' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Skapa pull request' })).toBeDisabled();
  });

  it('copies commit-correct condensed Actions failures from the revisitable Work view', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    const user = userEvent.setup();
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } });
    render(<MemoryRouter initialEntries={['/projects/project-1']}><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></MemoryRouter>);
    await user.click(await screen.findByRole('button', { name: 'Kopiera fel med sammanhang' }));
    expect(writeText).toHaveBeenCalledTimes(1);
    const copied = String(writeText.mock.calls[0][0]);
    expect(copied).toContain('Repository: owner/repo');
    expect(copied).toContain('Branch: zip-github/work-1');
    expect(copied).toContain('Commit: 1234567890abcdef');
    expect(copied).toContain('Workflow: CI');
    expect(copied).toContain('BUILD FAILURE');
  });
});


describe('ProjectDetailPage degraded Work history', () => {
  it('shows the persisted Work head fallback while keeping the resumable import actionable', async () => {
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith('/imports')) return new Response(JSON.stringify([
        { id: 'import-review', projectId: 'project-1', baseBranch: 'main', status: 'READY_FOR_REVIEW', createdAt: '2026-08-06T21:00:00Z', sourceFilename: 'draft.zip', sourceSizeBytes: 90, sourceType: 'WEB_UPLOAD', sourceReference: null, planDigestSha256: 'b'.repeat(64), pullRequestNumber: null, pullRequestUrl: null, resumeStage: 'REVIEW' },
      ]), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/commits')) return new Response(JSON.stringify({ githubAvailable: false, commits: [
        { sha: 'abcdef0123456789', message: 'Senaste kända Work-commit', authorName: '', authorEmail: '', authoredAt: '2026-08-06T20:00:00Z', htmlUrl: 'https://github.com/owner/repo/commit/abcdef0123456789', fallback: true },
      ] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work')) return new Response(JSON.stringify({ id: 'work-1', projectId: 'project-1', baseBranch: 'main', branchName: 'zip-github/work-1', status: 'ACTIVE', headCommitSha: 'abcdef0123456789', pullRequestNumber: null, pullRequestUrl: null, createdAt: '2026-08-06T18:30:00Z', updatedAt: '2026-08-06T20:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      return new Response(JSON.stringify({ id: 'project-1', name: 'Bokprojekt', githubInstallationId: 1, githubRepositoryId: 2, repositoryFullName: 'owner/repo', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-06T18:00:00Z', updatedAt: '2026-08-06T18:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }));

    render(<MemoryRouter initialEntries={['/projects/project-1']}><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'repo' })).toBeInTheDocument();
    expect(screen.getByText('GitHub-historiken kunde inte läsas just nu. Senaste lokalt kända commit visas.')).toBeInTheDocument();
    expect(screen.getByText('Senaste kända Work-commit')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Fortsätt granska' })).toHaveAttribute('href', '/projects/project-1/imports/import-review/review');
  });
});


describe('ProjectDetailPage state-based Work actions', () => {
  it('shows exactly one next-ZIP action when Work is open without an active import', async () => {
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith('/imports')) return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/commits')) return new Response(JSON.stringify({ githubAvailable: true, commits: [
        { sha: '1234567890abcdef', message: 'Latest commit', authorName: 'Erland', authorEmail: 'e@example.test', authoredAt: '2026-08-06T20:00:00Z', htmlUrl: 'https://github.com/owner/repo/commit/1234567890abcdef', fallback: false },
      ] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work')) return new Response(JSON.stringify({ id: 'work-1', projectId: 'project-1', baseBranch: 'main', branchName: 'zip-github/work-1', status: 'ACTIVE', headCommitSha: '1234567890abcdef', pullRequestNumber: null, pullRequestUrl: null, createdAt: '2026-08-06T18:30:00Z', updatedAt: '2026-08-06T20:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      return new Response(JSON.stringify({ id: 'project-1', name: 'Bokprojekt', githubInstallationId: 1, githubRepositoryId: 2, repositoryFullName: 'owner/repo', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-06T18:00:00Z', updatedAt: '2026-08-06T18:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }));

    render(<MemoryRouter initialEntries={['/projects/project-1']}><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'repo' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Nästa steg' })).toBeInTheDocument();
    expect(screen.getByText('Arbetet är aktivt. Fortsätt med nästa ZIP; när du är klar kan du skapa en pull request.')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Ladda upp nästa ZIP' })).toHaveLength(1);
    expect(screen.queryByRole('link', { name: 'Fortsätt arbete' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Skapa pull request' })).toBeEnabled();
  });
});


  it('guides an active import before exposing any next ZIP action', async () => {
    render(<MemoryRouter initialEntries={['/projects/project-1']}><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Nästa steg' })).toBeInTheDocument();
    expect(screen.getByText('En ZIP-import väntar på att slutföras. Fortsätt den innan du startar nästa import.')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Fortsätt granska' })).toHaveLength(1);
    expect(screen.queryByRole('link', { name: 'Ladda upp nästa ZIP' })).not.toBeInTheDocument();
  });

  it('guides a repository without Work to start one and does not duplicate the action', async () => {
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith('/imports')) return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/commits')) return new Response(JSON.stringify({ githubAvailable: true, commits: [] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/branches')) return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work')) return new Response(null, { status: 204 });
      return new Response(JSON.stringify({ id: 'project-1', name: 'Bokprojekt', githubInstallationId: 1, githubRepositoryId: 2, repositoryFullName: 'owner/repo', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-06T18:00:00Z', updatedAt: '2026-08-06T18:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }));

    render(<MemoryRouter initialEntries={['/projects/project-1']}><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Nästa steg' })).toBeInTheDocument();
    expect(screen.getByText('Inget arbete pågår ännu. Ladda upp den första ZIP-filen så skapar zip-GitHub automatiskt en verifierad Work-branch.')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Ladda upp första ZIP' })).toHaveLength(1);
    expect(screen.queryByRole('button', { name: 'Starta arbete' })).not.toBeInTheDocument();
  });



describe('ProjectDetailPage guided PR_CLOSED action', () => {
  it('prioritizes creating a new pull request and keeps next ZIP secondary', async () => {
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith('/imports')) return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/commits')) return new Response(JSON.stringify({ githubAvailable: true, commits: [
        { sha: '1234567890abcdef', message: 'Latest commit', authorName: 'Erland', authorEmail: 'e@example.test', authoredAt: '2026-08-06T20:00:00Z', htmlUrl: 'https://github.com/owner/repo/commit/1234567890abcdef', fallback: false },
      ] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work')) return new Response(JSON.stringify({ id: 'work-1', projectId: 'project-1', baseBranch: 'main', branchName: 'zip-github/work-1', status: 'PR_CLOSED', headCommitSha: '1234567890abcdef', remoteHeadCommitSha: '1234567890abcdef', branchChangedExternally: false, lastImportId: 'import-result', pullRequestNumber: 41, pullRequestUrl: 'https://github.com/owner/repo/pull/41', createdAt: '2026-08-06T18:30:00Z', updatedAt: '2026-08-06T20:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/actions/details')) return new Response(JSON.stringify({ importId: 'import-result', repositoryFullName: 'owner/repo', commitSha: '1234567890abcdef', detailsUrl: 'https://github.com/owner/repo/actions', artifacts: [], failures: [], checkedAt: '2026-08-06T20:01:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/actions')) return new Response(JSON.stringify({ importId: 'import-result', repositoryFullName: 'owner/repo', commitSha: '1234567890abcdef', state: 'success', terminal: true, detailsUrl: 'https://github.com/owner/repo/actions', workflows: [], checks: [], checkedAt: '2026-08-06T20:01:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/api/imports/import-result/actions/control')) return new Response(JSON.stringify({ importId: 'import-result', repositoryFullName: 'owner/repo', branchRef: 'zip-github/work-1', commitSha: '1234567890abcdef', currentWork: true, disabledReason: null, workflows: [] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      return new Response(JSON.stringify({ id: 'project-1', name: 'Bokprojekt', githubInstallationId: 1, githubRepositoryId: 2, repositoryFullName: 'owner/repo', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-06T18:00:00Z', updatedAt: '2026-08-06T18:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }));

    render(<MemoryRouter initialEntries={['/projects/project-1']}><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Nästa steg' })).toBeInTheDocument();
    expect(screen.getByText('Den tidigare pull requesten är stängd utan merge. Skapa en ny PR när du vill leverera det aktuella arbetet.')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Skapa ny pull request' })).toHaveLength(1);
    expect(screen.getAllByRole('link', { name: 'Ladda upp nästa ZIP' })).toHaveLength(1);
  });
});

describe('ProjectDetailPage cancellation lifecycle', () => {
  it('cancels the active import and exposes exactly one next-ZIP action afterwards', async () => {
    const user = userEvent.setup();
    let cancelled = false;
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith('/api/imports/import-review/cancel') && init?.method === 'POST') {
        cancelled = true;
        return new Response(JSON.stringify({ id: 'import-review', projectId: 'project-1', baseBranch: 'main', status: 'CANCELLED', createdAt: '2026-08-06T21:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      }
      if (url.endsWith('/imports')) return new Response(JSON.stringify(cancelled ? [] : [
        { id: 'import-review', projectId: 'project-1', baseBranch: 'main', status: 'READY_FOR_REVIEW', createdAt: '2026-08-06T21:00:00Z', sourceFilename: 'draft.zip', sourceSizeBytes: 90, sourceType: 'WEB_UPLOAD', sourceReference: null, planDigestSha256: 'b'.repeat(64), pullRequestNumber: null, pullRequestUrl: null, resumeStage: 'REVIEW' },
      ]), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/commits')) return new Response(JSON.stringify({ githubAvailable: true, commits: [
        { sha: '1234567890abcdef', message: 'Latest commit', authorName: 'Erland', authorEmail: 'e@example.test', authoredAt: '2026-08-06T20:00:00Z', htmlUrl: 'https://github.com/owner/repo/commit/1234567890abcdef', fallback: false },
      ] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work')) return new Response(JSON.stringify({ id: 'work-1', projectId: 'project-1', baseBranch: 'main', branchName: 'zip-github/work-1', status: 'ACTIVE', headCommitSha: '1234567890abcdef', pullRequestNumber: null, pullRequestUrl: null, createdAt: '2026-08-06T18:30:00Z', updatedAt: '2026-08-06T20:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      return new Response(JSON.stringify({ id: 'project-1', name: 'Bokprojekt', githubInstallationId: 1, githubRepositoryId: 2, repositoryFullName: 'owner/repo', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-06T18:00:00Z', updatedAt: '2026-08-06T18:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    });
    vi.stubGlobal('fetch', fetchMock);

    render(<MemoryRouter initialEntries={['/projects/project-1']}><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></MemoryRouter>);
    expect(await screen.findByRole('link', { name: 'Fortsätt granska' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Ladda upp nästa ZIP' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Avbryt import' }));
    await user.click(screen.getByRole('button', { name: 'Ja, avbryt import' }));

    expect(await screen.findByRole('link', { name: 'Ladda upp nästa ZIP' })).toHaveAttribute('href', '/projects/project-1/imports/new');
    expect(screen.queryByRole('link', { name: 'Fortsätt granska' })).not.toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Ladda upp nästa ZIP' })).toHaveLength(1);
    expect(fetchMock).toHaveBeenCalledWith('/api/imports/import-review/cancel', expect.objectContaining({ method: 'POST' }));
  });
});

describe('ProjectDetailPage step 9.8 lifecycle', () => {
  it('links the repository and keeps explicit resume of a non-default branch available', async () => {
    const user = userEvent.setup();
    let active = false;
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith('/imports')) return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/commits')) return new Response(JSON.stringify({ githubAvailable: true, commits: [] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/branches')) return new Response(JSON.stringify([{ name: 'old-work', commitSha: 'a'.repeat(40) }]), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work') && init?.method === 'POST') { active = true; return new Response(JSON.stringify({ id: 'work-2', projectId: 'project-1', baseBranch: 'main', branchName: 'zip-github/work-2', status: 'ACTIVE', headCommitSha: null, pullRequestNumber: null, pullRequestUrl: null, createdAt: '2026-08-08T14:00:00Z', updatedAt: '2026-08-08T14:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } }); }
      if (url.endsWith('/work')) return active
        ? new Response(JSON.stringify({ id: 'work-2', projectId: 'project-1', baseBranch: 'main', branchName: 'zip-github/work-2', status: 'ACTIVE', headCommitSha: null, pullRequestNumber: null, pullRequestUrl: null, createdAt: '2026-08-08T14:00:00Z', updatedAt: '2026-08-08T14:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } })
        : new Response(null, { status: 204 });
      return new Response(JSON.stringify({ id: 'project-1', name: 'Bokprojekt', githubInstallationId: 1, githubRepositoryId: 2, repositoryFullName: 'owner/repo', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-06T18:00:00Z', updatedAt: '2026-08-06T18:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    });
    vi.stubGlobal('fetch', fetchMock);

    render(<MemoryRouter initialEntries={['/projects/project-1']}><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></MemoryRouter>);
    const repo = await screen.findByRole('link', { name: 'owner/repo' });
    expect(repo).toHaveAttribute('href', 'https://github.com/owner/repo/tree/main');
    expect(await screen.findByRole('option', { name: 'old-work' })).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Eller fortsätt på befintlig branch'), 'old-work');
    await user.click(screen.getByRole('button', { name: 'Fortsätt på vald branch' }));
    expect(await screen.findByText('zip-github/work-2')).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith('/api/projects/project-1/work', expect.objectContaining({ method: 'POST', body: JSON.stringify({ existingBranch: 'old-work' }) }));
  });
  it('keeps an open pull request as active work and surfaces remote branch changes', async () => {
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith('/imports')) return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/commits')) return new Response(JSON.stringify({ commits: [], githubAvailable: true }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/actions/details')) return new Response(JSON.stringify({ importId: 'import-result', repositoryFullName: 'owner/repo', commitSha: 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', detailsUrl: 'https://github.com/owner/repo/actions', artifacts: [], failures: [], checkedAt: '2026-08-09T10:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/api/imports/import-result/actions/control')) return new Response(JSON.stringify({ importId: 'import-result', repositoryFullName: 'owner/repo', branchRef: 'zip-github/work-1', commitSha: 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', currentWork: true, disabledReason: null, workflows: [] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work/actions')) return new Response(JSON.stringify({ importId: 'import-result', repositoryFullName: 'owner/repo', commitSha: 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', state: 'success', terminal: true, detailsUrl: 'https://github.com/owner/repo/actions', workflows: [], checks: [], checkedAt: '2026-08-09T10:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.endsWith('/work')) return new Response(JSON.stringify({ id: 'work-1', projectId: 'project-1', baseBranch: 'main', branchName: 'zip-github/work-1', status: 'PR_OPEN', headCommitSha: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', remoteHeadCommitSha: 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', branchChangedExternally: true, lastImportId: 'import-result', pullRequestNumber: 42, pullRequestUrl: 'https://github.com/owner/repo/pull/42', createdAt: '2026-08-09T09:00:00Z', updatedAt: '2026-08-09T09:30:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if (url.includes('/api/projects/project-1')) return new Response(JSON.stringify({ id: 'project-1', name: 'repo', githubInstallationId: 10, githubRepositoryId: 20, repositoryFullName: 'owner/repo', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-08T10:00:00Z', updatedAt: '2026-08-08T10:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
      return new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } });
    }));
    render(
      <MemoryRouter initialEntries={['/projects/project-1']}>
        <Routes><Route path="projects/:projectId" element={<ProjectDetailPage />} /></Routes>
      </MemoryRouter>,
    );
    expect(await screen.findByText('Pull request #42')).toBeInTheDocument();
    expect(screen.getByText('Pull requesten är öppen. Nästa ZIP läggs på samma Work-branch och uppdaterar PR:n automatiskt.')).toBeInTheDocument();
    expect(screen.getByLabelText('Work-branchen har ändrats externt')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ladda upp nästa ZIP' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Skapa pull request' })).not.toBeInTheDocument();
  });

});
