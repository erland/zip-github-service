import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App';

function renderApp(initialEntries = ['/']) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={initialEntries}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const sessionResponse = {
  id: 'session-1',
  label: 'Test session',
  status: 'OPEN',
  createdAt: '2026-01-01T00:00:00Z',
  closedAt: null,
  createdBy: null,
  retentionPolicy: null,
};

const runResponse = {
  id: 'run-1',
  sessionId: 'session-1',
  sourcePackageId: 'package-1',
  status: 'PASSED',
  planId: 'node-default',
  requestedPlanId: null,
  networkMode: 'DEPENDENCY',
  summary: 'Verification passed.',
  startedAt: '2026-01-01T00:00:01Z',
  completedAt: '2026-01-01T00:00:02Z',
  durationMillis: 1000,
  commands: [
    {
      id: 'command-1',
      commandLabel: 'npm test',
      workingDirectory: '.',
      commandDisplay: 'npm test -- --runInBand',
      status: 'PASSED',
      exitCode: 0,
      startedAt: '2026-01-01T00:00:01Z',
      completedAt: '2026-01-01T00:00:02Z',
      durationMillis: 1000,
      logExcerpt: 'Tests passed',
      failureCategory: null,
      failureMessage: null,
      stdoutArtifactRef: 'artifact-1',
      stderrArtifactRef: null,
    },
  ],
};

const runSummaryResponse = {
  runId: 'run-1',
  status: 'PASSED',
  summary: 'Verification passed.',
  planId: 'node-default',
  primaryFailure: null,
  commandsRun: ['npm test'],
  suggestedFocus: [],
  partial: false,
};

describe('App', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders the service title and navigation', () => {
    renderApp();

    expect(screen.getByRole('heading', { name: /build and test verification/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Plans' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'About' })).toBeInTheDocument();
  });

  it('creates a session and navigates to package upload', async () => {
    const user = userEvent.setup();

    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(
        new Response(JSON.stringify(sessionResponse), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(sessionResponse), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ runs: [] }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );

    renderApp();

    await user.type(screen.getByLabelText(/session label/i), 'Test session');
    await user.click(screen.getByRole('button', { name: /create session/i }));

    expect(await screen.findByRole('heading', { name: 'Test session' })).toBeInTheDocument();
    expect(screen.getByText(/Package upload/i)).toBeInTheDocument();
  });

  it('uploads a package, starts a run, and opens the run report', async () => {
    const user = userEvent.setup();

    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(
        new Response(JSON.stringify(sessionResponse), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ runs: [] }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            id: 'package-1',
            sessionId: 'session-1',
            originalFilename: 'source.zip',
            checksumSha256: 'abc',
            compressedSizeBytes: 10,
            extractedSizeBytes: 20,
            fileCount: 2,
            topLevelEntries: 'README.md',
            storageReference: 'packages/package-1.zip',
            status: 'ACCEPTED',
            rejectionReason: null,
            createdAt: '2026-01-01T00:00:00Z',
            projectDetection: {
              projects: [],
              supported: false,
              summary: 'No supported project detected.',
            },
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(runResponse), {
          status: 201,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ runs: [runResponse] }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(runResponse), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(runResponse), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(runSummaryResponse), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ artifacts: [] }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );

    renderApp(['/sessions/session-1']);

    const file = new File(['zip-content'], 'source.zip', { type: 'application/zip' });
    await user.upload(await screen.findByLabelText(/upload source-code zip/i), file);
    await user.click(screen.getByRole('button', { name: /upload package/i }));

    expect(await screen.findByRole('heading', { name: /verification run/i })).toBeInTheDocument();
    expect((await screen.findAllByText(/Verification passed/i)).length).toBeGreaterThan(0);
    expect(screen.getByText(/npm test -- --runInBand/i)).toBeInTheDocument();
  });

  it('shows a run report with command results and log excerpts', async () => {
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(
        new Response(JSON.stringify(runResponse), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(runSummaryResponse), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ artifacts: [] }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );

    renderApp(['/runs/run-1']);

    expect(await screen.findByText(/node-default/i)).toBeInTheDocument();
    expect(screen.getByText(/npm test -- --runInBand/i)).toBeInTheDocument();
    expect(screen.getByText(/Tests passed/i)).toBeInTheDocument();
  });
});
