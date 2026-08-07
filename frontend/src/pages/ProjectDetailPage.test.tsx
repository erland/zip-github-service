import { cleanup, render, screen } from '@testing-library/react';
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
    if (url.endsWith('/work/commits')) return new Response(JSON.stringify({ githubAvailable: true, commits: [
      { sha: '1234567890abcdef', message: 'Update game board\n\nDetails', authorName: 'Erland', authorEmail: 'e@example.test', authoredAt: '2026-08-06T20:00:00Z', htmlUrl: 'https://github.com/owner/repo/commit/1234567890abcdef', fallback: false },
    ] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/work')) return new Response(JSON.stringify({ id: 'work-1', projectId: 'project-1', baseBranch: 'main', branchName: 'zip-github/work-1', status: 'ACTIVE', headCommitSha: '1234567890abcdef', pullRequestNumber: null, pullRequestUrl: null, createdAt: '2026-08-06T18:30:00Z', updatedAt: '2026-08-06T20:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    return new Response(JSON.stringify({ id: 'project-1', name: 'Bokprojekt', githubInstallationId: 1, githubRepositoryId: 2, repositoryFullName: 'owner/repo', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-06T18:00:00Z', updatedAt: '2026-08-06T18:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  }));
});

describe('ProjectDetailPage work history', () => {
  it('shows Git commits and only the active import in the primary project view', async () => {
    render(<MemoryRouter initialEntries={['/projects/project-1']}><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Bokprojekt' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Commits i arbetet' })).toBeInTheDocument();
    expect(screen.getByText('Update game board')).toBeInTheDocument();
    expect(screen.getByText(/1234567890ab/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Öppna commit' })).toHaveAttribute('href', 'https://github.com/owner/repo/commit/1234567890abcdef');
    expect(screen.getByRole('heading', { name: 'draft.zip' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Fortsätt granska' })).toHaveAttribute('href', '/projects/project-1/imports/import-review/review');
    expect(screen.queryByText('book.zip')).not.toBeInTheDocument();
    expect(screen.queryByText('older-draft.zip')).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Importhistorik' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Arbetet är klart – skapa pull request' })).toBeDisabled();
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
    expect(await screen.findByRole('heading', { name: 'Bokprojekt' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('GitHub-historiken kunde inte läsas');
    expect(screen.getByText('Senaste kända Work-commit')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Fortsätt granska' })).toHaveAttribute('href', '/projects/project-1/imports/import-review/review');
  });
});
