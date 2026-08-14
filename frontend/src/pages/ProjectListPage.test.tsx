import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ProjectListPage from './ProjectListPage';

const mocks = vi.hoisted(() => ({
  getRepositories: vi.fn(),
  getProjectWork: vi.fn(),
  getProjectImports: vi.fn(),
  getProjectWorkActions: vi.fn(),
}));

vi.mock('../api/repositories', () => ({ getRepositories: mocks.getRepositories }));
vi.mock('../api/projects', () => ({
  getProjectWork: mocks.getProjectWork,
  getProjectImports: mocks.getProjectImports,
  getProjectWorkActions: mocks.getProjectWorkActions,
}));

const repositories = [
  { githubInstallationId: 1, githubRepositoryId: 11, repositoryFullName: 'owner/review-me', repositoryName: 'review-me', privateRepository: true, defaultBranch: 'main', htmlUrl: 'https://github.com/owner/review-me', projectId: 'project-review' },
  { githubInstallationId: 1, githubRepositoryId: 12, repositoryFullName: 'owner/ongoing', repositoryName: 'ongoing', privateRepository: true, defaultBranch: 'main', htmlUrl: 'https://github.com/owner/ongoing', projectId: 'project-ongoing' },
  { githubInstallationId: 1, githubRepositoryId: 13, repositoryFullName: 'owner/new-repo', repositoryName: 'new-repo', privateRepository: true, defaultBranch: 'main', htmlUrl: 'https://github.com/owner/new-repo', projectId: null },
];

beforeEach(() => {
  mocks.getRepositories.mockReset().mockResolvedValue(repositories);
  mocks.getProjectWork.mockReset().mockImplementation(async (projectId: string) => {
    if (projectId === 'project-review') return { id: 'work-review', projectId, baseBranch: 'main', branchName: 'zip-github/work-review', status: 'ACTIVE', headCommitSha: null, remoteHeadCommitSha: null, branchChangedExternally: false, lastImportId: 'import-review', pullRequestNumber: null, pullRequestUrl: null, createdAt: '2026-08-14T10:00:00Z', updatedAt: '2026-08-14T10:00:00Z' };
    return { id: 'work-ongoing', projectId, baseBranch: 'main', branchName: 'zip-github/work-ongoing', status: 'PR_OPEN', headCommitSha: 'a'.repeat(40), remoteHeadCommitSha: 'a'.repeat(40), branchChangedExternally: false, lastImportId: 'import-result', pullRequestNumber: 42, pullRequestUrl: 'https://github.com/owner/ongoing/pull/42', createdAt: '2026-08-14T10:00:00Z', updatedAt: '2026-08-14T10:00:00Z' };
  });
  mocks.getProjectImports.mockReset().mockImplementation(async (projectId: string) => projectId === 'project-review'
    ? [{ id: 'import-review', projectId, baseBranch: 'main', status: 'PLANNED', createdAt: '2026-08-14T10:01:00Z', sourceFilename: 'project.zip', sourceSizeBytes: 10, planDigestSha256: 'b'.repeat(64), pullRequestNumber: null, pullRequestUrl: null, resumeStage: 'REVIEW' }]
    : []);
  mocks.getProjectWorkActions.mockReset().mockResolvedValue({ importId: 'import-result', repositoryFullName: 'owner/ongoing', commitSha: 'a'.repeat(40), state: 'success', terminal: true, detailsUrl: 'https://github.com/owner/ongoing/actions', workflows: [], checks: [], diagnosticCode: null, diagnosticMessage: null, checkedAt: '2026-08-14T10:02:00Z' });
});

afterEach(() => cleanup());

describe('ProjectListPage attention overview', () => {
  it('groups attention, ongoing and other repositories and routes attention directly to its task', async () => {
    render(<MemoryRouter><ProjectListPage /></MemoryRouter>);

    const attention = await screen.findByRole('heading', { name: 'Behöver din uppmärksamhet' });
    const ongoing = screen.getByRole('heading', { name: 'Pågående' });
    const other = screen.getByRole('heading', { name: 'Övriga repositories' });

    expect(attention.compareDocumentPosition(ongoing) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(ongoing.compareDocumentPosition(other) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();

    const attentionSection = attention.closest('section');
    expect(attentionSection).not.toBeNull();
    expect(within(attentionSection as HTMLElement).getByRole('link', { name: /review-me/ })).toHaveAttribute('href', '/projects/project-review/imports/import-review/review');
    expect(within(attentionSection as HTMLElement).getByText('Import väntar på granskning')).toBeInTheDocument();

    const ongoingSection = ongoing.closest('section');
    expect(within(ongoingSection as HTMLElement).getByText('Pull request är öppen')).toBeInTheDocument();

    const otherSection = other.closest('section');
    expect(within(otherSection as HTMLElement).getByRole('link', { name: /new-repo/ })).toHaveAttribute('href', '/repositories/1/13');
  });

  it('fails closed into attention when project status cannot be verified', async () => {
    mocks.getProjectWork.mockRejectedValueOnce(new Error('GitHub unavailable'));
    render(<MemoryRouter><ProjectListPage /></MemoryRouter>);

    const attention = await screen.findByRole('heading', { name: 'Behöver din uppmärksamhet' });
    const section = attention.closest('section');
    expect(within(section as HTMLElement).getByText('Status kunde inte verifieras')).toBeInTheDocument();
  });

  it('keeps search as a flat repository finder across all groups', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><ProjectListPage /></MemoryRouter>);
    await screen.findByRole('heading', { name: 'Behöver din uppmärksamhet' });

    await user.type(screen.getByRole('searchbox', { name: 'Sök repositories' }), 'new-repo');

    expect(screen.getByRole('heading', { name: 'Sökresultat' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /new-repo/ })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Behöver din uppmärksamhet' })).not.toBeInTheDocument();
  });
});
