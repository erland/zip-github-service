import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, expect, it, vi } from 'vitest';
import PullRequestComposer from './PullRequestComposer';

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

function commitHistoryResponse() {
  return new Response(JSON.stringify({ githubAvailable: true, commits: [
    { sha: 'b'.repeat(40), message: 'Second commit\nwith body', authorName: 'Erland', authorEmail: 'e@example.test', authoredAt: '2026-08-11T05:00:00Z', htmlUrl: null, fallback: false },
    { sha: 'a'.repeat(40), message: 'First commit title\nfirst body line', authorName: 'Erland', authorEmail: 'e@example.test', authoredAt: '2026-08-11T04:00:00Z', htmlUrl: null, fallback: false },
  ] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
}

it('fills a plain chronological commit list and uses the first commit subject as an empty PR title', async () => {
  const user = userEvent.setup();
  vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
    if (String(input).endsWith('/api/projects/p1/work/commits')) return commitHistoryResponse();
    throw new Error(`Unexpected fetch: ${String(input)}`);
  }));

  render(<PullRequestComposer projectId="p1" onCreated={() => undefined} onCancel={() => undefined} />);
  await user.click(screen.getByRole('button', { name: 'Fyll från commitmeddelanden' }));

  expect(await screen.findByDisplayValue(/First commit title/)).toHaveValue('- First commit title\n  first body line\n- Second commit\n  with body');
  expect(screen.getByLabelText('Titel')).toHaveValue('First commit title');
});

it('does not replace a PR title already entered by the user when filling from commits', async () => {
  const user = userEvent.setup();
  vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
    if (String(input).endsWith('/api/projects/p1/work/commits')) return commitHistoryResponse();
    throw new Error(`Unexpected fetch: ${String(input)}`);
  }));

  render(<PullRequestComposer projectId="p1" onCreated={() => undefined} onCancel={() => undefined} />);
  await user.type(screen.getByLabelText('Titel'), 'My custom PR title');
  await user.click(screen.getByRole('button', { name: 'Fyll från commitmeddelanden' }));

  expect(await screen.findByDisplayValue(/Second commit/)).toHaveValue('- First commit title\n  first body line\n- Second commit\n  with body');
  expect(screen.getByLabelText('Titel')).toHaveValue('My custom PR title');
});
