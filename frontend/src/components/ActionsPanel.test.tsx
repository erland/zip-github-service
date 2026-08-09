import { cleanup, render, screen } from '@testing-library/react';
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


it('falls back to the panel commit when a workflow payload has no headSha', () => {
  const actions = {
    importId: 'import-legacy', repositoryFullName: 'erland/example', commitSha: 'f'.repeat(40), state: 'success', terminal: true,
    detailsUrl: 'https://github.com/erland/example/actions', checkedAt: '2026-08-08T18:00:00Z',
    workflows: [{ id: 10, name: 'CI', state: 'success', terminal: true, event: 'push', htmlUrl: 'https://github.com/erland/example/actions/runs/10', jobs: [] }],
    checks: [],
  } as unknown as ImportActionsStatusResponse;

  render(<ActionsPanel actions={actions} details={null} fallbackUrl="https://github.com/erland/example/actions" repositoryFullName="erland/example" branchName="zip-github/work-1" commitSha={'f'.repeat(40)} />);

  expect(screen.getByText('CI')).toBeInTheDocument();
  expect(screen.getAllByText('ffffffffffff').length).toBeGreaterThan(0);
});
