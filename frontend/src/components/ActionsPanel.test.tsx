import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, expect, it } from 'vitest';
import ActionsPanel from './ActionsPanel';
import { ImportActionsStatusResponse } from '../api/imports';

afterEach(cleanup);

it('distinguishes a GitHub Actions permission failure from not-started', () => {
  render(<ActionsPanel repositoryFullName="erland/got-test-repo" branchName="zip-github/work-1" commitSha={'f'.repeat(40)}
    fallbackUrl="https://github.com/erland/got-test-repo/actions" details={null}
    actions={{ importId:'i1', repositoryFullName:'erland/got-test-repo', commitSha:'f'.repeat(40), state:'unavailable', terminal:false,
      detailsUrl:'https://github.com/erland/got-test-repo/actions', workflows:[], checks:[], diagnosticCode:'ACTIONS_PERMISSION_REQUIRED',
      diagnosticMessage:'GitHub App-installationen saknar behörighet att läsa Actions för repositoryt.', checkedAt:'2026-08-08T17:00:00Z' }} />);
  expect(screen.getByText(/saknar behörighet att läsa Actions/)).toBeInTheDocument();
  expect(screen.getByText(/Repository permissions/)).toBeInTheDocument();
  expect(screen.queryByText(/Ingen workflow-körning/)).not.toBeInTheDocument();
});

it('shows condensed failure, surrounding context and an expandable bounded job log', () => {
  const actions = { importId:'i1', repositoryFullName:'erland/got-test-repo', commitSha:'f'.repeat(40), state:'failure' as const, terminal:true,
    detailsUrl:'https://github.com/erland/got-test-repo/actions', diagnosticCode:null, diagnosticMessage:null, checkedAt:'2026-08-08T17:00:00Z', checks:[],
    workflows:[{ id:31258714926, workflowId:329441754, workflowPath:'.github/workflows/test-action.yml', headBranch:'zip-github/work-1', headSha:'f'.repeat(40), name:'Test Action 4', state:'failure' as const, terminal:true, event:'push', htmlUrl:'https://github.com/erland/got-test-repo/actions/runs/31258714926', createdAt:null, updatedAt:null, jobs:[] }] };
  const details = { importId:'i1', repositoryFullName:'erland/got-test-repo', commitSha:'f'.repeat(40), detailsUrl:actions.detailsUrl, checkedAt:'2026-08-08T17:00:01Z', artifacts:[],
    failures:[{ workflowRunId:31258714926, workflowName:'Test Action 4', jobId:1, jobName:'build', stepName:'npm test', tool:'npm/Vite', lines:['npm error test failed'], contextLines:['starting tests','npm error test failed','cleanup'], jobLogLines:['setup','starting tests','npm error test failed','cleanup'], logTruncated:false, githubUrl:'https://github.com/erland/got-test-repo/actions/runs/31258714926' }] };
  render(<ActionsPanel repositoryFullName="erland/got-test-repo" branchName="zip-github/work-1" commitSha={'f'.repeat(40)} fallbackUrl={actions.detailsUrl} actions={actions} details={details} />);
  expect(screen.getByText('Fel och jobbloggar')).toBeInTheDocument();
  expect(screen.getByText('Visa sammanhang kring felet')).toBeInTheDocument();
  expect(screen.getByText('Visa sanerad jobblogg')).toBeInTheDocument();
  expect(screen.getByRole('button', { name:'Kopiera fel med sammanhang' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name:'Kopiera jobblogg' })).toBeInTheDocument();
});


it('falls back to the panel commit when a workflow payload has no headSha', async () => {
  const user = userEvent.setup();
  const actions = {
    importId: 'import-legacy', repositoryFullName: 'erland/example', commitSha: 'f'.repeat(40), state: 'success', terminal: true,
    detailsUrl: 'https://github.com/erland/example/actions', checkedAt: '2026-08-08T18:00:00Z',
    workflows: [{ id: 10, name: 'CI', state: 'success', terminal: true, event: 'push', htmlUrl: 'https://github.com/erland/example/actions/runs/10', jobs: [] }],
    checks: [],
  } as unknown as ImportActionsStatusResponse;

  render(<ActionsPanel actions={actions} details={null} fallbackUrl="https://github.com/erland/example/actions" repositoryFullName="erland/example" branchName="zip-github/work-1" commitSha={'f'.repeat(40)} />);
  expect(screen.getByText(/Alla observerade Actions-kontroller/)).toBeInTheDocument();
  await user.click(screen.getByText(/Visa Actions-detaljer/));

  expect(screen.getByText('CI')).toBeInTheDocument();
  expect(screen.getAllByText('ffffffffffff').length).toBeGreaterThan(0);
});

it('deduplicates GitHub Actions checks already represented by workflow jobs and keeps other checks', async () => {
  const user = userEvent.setup();
  const commitSha = 'a'.repeat(40);
  const actions: ImportActionsStatusResponse = {
    importId:'i-dedup', repositoryFullName:'erland/example', commitSha, state:'success', terminal:true,
    detailsUrl:'https://github.com/erland/example/actions', diagnosticCode:null, diagnosticMessage:null, checkedAt:'2026-08-09T18:00:00Z',
    workflows:[{ id:10, workflowId:20, workflowPath:'.github/workflows/ci.yml', headBranch:'zip-github/work-1', headSha:commitSha,
      name:'CI', state:'success', terminal:true, event:'push', htmlUrl:'https://github.com/erland/example/actions/runs/10', createdAt:null, updatedAt:null,
      jobs:[{ id:11, name:'Frontend tests and build', state:'success', terminal:true, htmlUrl:'https://github.com/erland/example/actions/runs/10/job/11', startedAt:null, completedAt:null }] }],
    checks:[
      { id:101, name:'Frontend tests and build', state:'success', terminal:true, htmlUrl:'https://github.com/erland/example/actions/runs/10/job/11', appName:'GitHub Actions', startedAt:null, completedAt:null },
      { id:102, name:'CodeQL', state:'success', terminal:true, htmlUrl:'https://github.com/erland/example/security/code-scanning', appName:'GitHub Advanced Security', startedAt:null, completedAt:null },
      { id:103, name:'Dependency review', state:'success', terminal:true, htmlUrl:'https://github.com/erland/example/runs/103', appName:'GitHub Actions', startedAt:null, completedAt:null },
    ],
  };

  render(<ActionsPanel actions={actions} details={null} fallbackUrl={actions.detailsUrl} repositoryFullName="erland/example" branchName="zip-github/work-1" commitSha={commitSha} />);
  await user.click(screen.getByText(/Visa Actions-detaljer/));

  expect(screen.getAllByText('Frontend tests and build')).toHaveLength(1);
  expect(screen.getByRole('heading', { name:'Övriga kontroller' })).toBeInTheDocument();
  const codeQlLink = screen.getByRole('link', { name:'CodeQL' });
  expect(codeQlLink).toBeInTheDocument();
  expect(codeQlLink.closest('li')).toHaveTextContent('GitHub Advanced Security');
  const dependencyReviewLink = screen.getByRole('link', { name:'Dependency review' });
  expect(dependencyReviewLink).toBeInTheDocument();
  expect(dependencyReviewLink.closest('li')).toHaveTextContent('GitHub Actions');
  expect(screen.queryByRole('heading', { name:'Checks' })).not.toBeInTheDocument();
});

it('omits the extra-check section when every check is the GitHub Actions representation of a shown job', async () => {
  const user = userEvent.setup();
  const commitSha = 'b'.repeat(40);
  const actions: ImportActionsStatusResponse = {
    importId:'i-dedup-only', repositoryFullName:'erland/example', commitSha, state:'success', terminal:true,
    detailsUrl:'https://github.com/erland/example/actions', diagnosticCode:null, diagnosticMessage:null, checkedAt:'2026-08-09T18:01:00Z',
    workflows:[{ id:20, workflowId:30, workflowPath:'.github/workflows/ci.yml', headBranch:'zip-github/work-2', headSha:commitSha,
      name:'CI', state:'success', terminal:true, event:'push', htmlUrl:'https://github.com/erland/example/actions/runs/20', createdAt:null, updatedAt:null,
      jobs:[{ id:21, name:'Backend tests and package', state:'success', terminal:true, htmlUrl:null, startedAt:null, completedAt:null }] }],
    checks:[{ id:201, name:'Backend tests and package', state:'success', terminal:true, htmlUrl:null, appName:'GitHub Actions', startedAt:null, completedAt:null }],
  };

  render(<ActionsPanel actions={actions} details={null} fallbackUrl={actions.detailsUrl} repositoryFullName="erland/example" branchName="zip-github/work-2" commitSha={commitSha} />);
  await user.click(screen.getByText(/Visa Actions-detaljer/));

  expect(screen.getAllByText('Backend tests and package')).toHaveLength(1);
  expect(screen.queryByRole('heading', { name:'Övriga kontroller' })).not.toBeInTheDocument();
});


it('keeps successful Actions compact until details are requested', async () => {
  const user = userEvent.setup();
  const commitSha = 'e'.repeat(40);
  const actions: ImportActionsStatusResponse = {
    importId:'i-success-compact', repositoryFullName:'erland/example', commitSha, state:'success', terminal:true,
    detailsUrl:'https://github.com/erland/example/actions', diagnosticCode:null, diagnosticMessage:null, checkedAt:'2026-08-14T20:00:00Z', checks:[],
    workflows:[{ id:501, workflowId:61, workflowPath:'.github/workflows/ci.yml', headBranch:'zip-github/work-5', headSha:commitSha,
      name:'CI', state:'success', terminal:true, event:'push', htmlUrl:null, createdAt:null, updatedAt:null, jobs:[] }],
  };
  render(<ActionsPanel actions={actions} details={null} fallbackUrl={actions.detailsUrl} repositoryFullName="erland/example" branchName="zip-github/work-5" commitSha={commitSha} />);

  expect(screen.getByText(/Alla observerade Actions-kontroller/)).toBeInTheDocument();
  const details = screen.getByText(/Visa Actions-detaljer/).closest('details');
  expect(details).not.toBeNull();
  expect(details).not.toHaveAttribute('open');

  await user.click(screen.getByText(/Visa Actions-detaljer/));
  expect(details).toHaveAttribute('open');
  expect(screen.getByText('CI')).toBeInTheDocument();
});

it('keeps failed Actions prominent without requiring expansion', () => {
  const commitSha = 'f'.repeat(40);
  const actions: ImportActionsStatusResponse = {
    importId:'i-failure-prominent', repositoryFullName:'erland/example', commitSha, state:'failure', terminal:true,
    detailsUrl:'https://github.com/erland/example/actions', diagnosticCode:null, diagnosticMessage:null, checkedAt:'2026-08-14T20:01:00Z', checks:[],
    workflows:[{ id:601, workflowId:71, workflowPath:'.github/workflows/ci.yml', headBranch:'zip-github/work-6', headSha:commitSha,
      name:'CI failure', state:'failure', terminal:true, event:'push', htmlUrl:null, createdAt:null, updatedAt:null, jobs:[] }],
  };
  render(<ActionsPanel actions={actions} details={null} fallbackUrl={actions.detailsUrl} repositoryFullName="erland/example" branchName="zip-github/work-6" commitSha={commitSha} />);

  expect(screen.getByText(/observerade Actions-kontroller har misslyckats/)).toBeInTheDocument();
  expect(screen.getByText('CI failure')).toBeInTheDocument();
  expect(screen.queryByText(/Visa Actions-detaljer/)).not.toBeInTheDocument();
});

it('groups push and pull_request runs for the same workflow and commit without losing their individual status', () => {
  const commitSha = 'c'.repeat(40);
  const actions: ImportActionsStatusResponse = {
    importId:'i-grouped-runs', repositoryFullName:'erland/example', commitSha, state:'failure', terminal:true,
    detailsUrl:'https://github.com/erland/example/actions', diagnosticCode:null, diagnosticMessage:null, checkedAt:'2026-08-13T18:00:00Z', checks:[],
    workflows:[
      { id:301, workflowId:44, workflowPath:'.github/workflows/ci.yml', headBranch:'zip-github/work-3', headSha:commitSha,
        name:'CI', state:'success', terminal:true, event:'push', htmlUrl:'https://github.com/erland/example/actions/runs/301', createdAt:'2026-08-13T17:58:00Z', updatedAt:'2026-08-13T17:59:00Z',
        jobs:[{ id:311, name:'Backend tests', state:'success', terminal:true, htmlUrl:null, startedAt:null, completedAt:null }] },
      { id:302, workflowId:44, workflowPath:'.github/workflows/ci.yml', headBranch:'zip-github/work-3', headSha:commitSha,
        name:'CI', state:'failure', terminal:true, event:'pull_request', htmlUrl:'https://github.com/erland/example/actions/runs/302', createdAt:'2026-08-13T17:59:00Z', updatedAt:'2026-08-13T18:00:00Z',
        jobs:[{ id:312, name:'Backend tests', state:'failure', terminal:true, htmlUrl:null, startedAt:null, completedAt:null }] },
    ],
  };

  render(<ActionsPanel actions={actions} details={null} fallbackUrl={actions.detailsUrl} repositoryFullName="erland/example" branchName="zip-github/work-3" commitSha={commitSha} />);

  expect(screen.getAllByText('CI')).toHaveLength(1);
  expect(screen.getByText('2 GitHub-körningar', { exact:false })).toBeInTheDocument();
  expect(screen.getAllByText('Misslyckad').length).toBeGreaterThan(0);
  expect(screen.getByText('Visa 2 separata körningar')).toBeInTheDocument();
  expect(screen.getByText('push')).toBeInTheDocument();
  expect(screen.getByText('pull_request')).toBeInTheDocument();
});

it('does not group different workflows that happen to share the same display name', async () => {
  const user = userEvent.setup();
  const commitSha = 'd'.repeat(40);
  const actions: ImportActionsStatusResponse = {
    importId:'i-distinct-workflows', repositoryFullName:'erland/example', commitSha, state:'success', terminal:true,
    detailsUrl:'https://github.com/erland/example/actions', diagnosticCode:null, diagnosticMessage:null, checkedAt:'2026-08-13T18:01:00Z', checks:[],
    workflows:[
      { id:401, workflowId:51, workflowPath:'.github/workflows/ci-a.yml', headBranch:'zip-github/work-4', headSha:commitSha,
        name:'CI', state:'success', terminal:true, event:'push', htmlUrl:null, createdAt:null, updatedAt:null, jobs:[] },
      { id:402, workflowId:52, workflowPath:'.github/workflows/ci-b.yml', headBranch:'zip-github/work-4', headSha:commitSha,
        name:'CI', state:'success', terminal:true, event:'push', htmlUrl:null, createdAt:null, updatedAt:null, jobs:[] },
    ],
  };

  render(<ActionsPanel actions={actions} details={null} fallbackUrl={actions.detailsUrl} repositoryFullName="erland/example" branchName="zip-github/work-4" commitSha={commitSha} />);
  await user.click(screen.getByText(/Visa Actions-detaljer/));

  expect(screen.getAllByText('CI')).toHaveLength(2);
  expect(screen.queryByText(/GitHub-körningar/)).not.toBeInTheDocument();
});
