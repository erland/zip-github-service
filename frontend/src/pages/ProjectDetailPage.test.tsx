import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ProjectDetailPage from './ProjectDetailPage';

afterEach(() => cleanup());

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url.endsWith('/imports')) return new Response(JSON.stringify([
      { id: 'import-result', projectId: 'project-1', baseBranch: 'main', status: 'PULL_REQUEST_CREATED', createdAt: '2026-08-06T20:00:00Z', sourceFilename: 'book.zip', sourceSizeBytes: 100, planDigestSha256: 'a'.repeat(64), pullRequestNumber: 42, pullRequestUrl: 'https://github.com/owner/repo/pull/42', resumeStage: 'RESULT' },
      { id: 'import-review', projectId: 'project-1', baseBranch: 'main', status: 'READY_FOR_REVIEW', createdAt: '2026-08-06T19:00:00Z', sourceFilename: 'draft.zip', sourceSizeBytes: 90, planDigestSha256: 'b'.repeat(64), pullRequestNumber: null, pullRequestUrl: null, resumeStage: 'REVIEW' },
    ]), { status: 200, headers: { 'Content-Type': 'application/json' } });
    return new Response(JSON.stringify({ id: 'project-1', name: 'Bokprojekt', githubInstallationId: 1, githubRepositoryId: 2, repositoryFullName: 'owner/repo', privateRepository: true, defaultBranch: 'main', active: true, createdAt: '2026-08-06T18:00:00Z', updatedAt: '2026-08-06T18:00:00Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  }));
});

describe('ProjectDetailPage import history', () => {
  it('shows newest imports and links to the correct reopen stage', async () => {
    render(<MemoryRouter initialEntries={['/projects/project-1']}><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Bokprojekt' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Öppna resultat' })).toHaveAttribute('href', '/projects/project-1/imports/import-result/result');
    expect(screen.getByRole('link', { name: 'Fortsätt granska' })).toHaveAttribute('href', '/projects/project-1/imports/import-review/review');
  });
});
