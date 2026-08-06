import { Link, useParams } from 'react-router-dom';
import { ArtifactList } from '../components/ArtifactList';
import { CommandResultTable } from '../components/CommandResultTable';
import { FailureSummaryCard } from '../components/FailureSummaryCard';
import { LogExcerptPanel } from '../components/LogExcerptPanel';
import { RunStatusBadge } from '../components/RunStatusBadge';
import { ErrorCard, LoadingCard, MissingResourceCard, PageCard, PageLayout } from '../components/PageState';
import { useRun, useRunSummary } from '../api/runs';
import { formatDuration } from '../utils/format';

export function RunPage() {
  const { runId } = useParams();
  const runQuery = useRun(runId);
  const summaryQuery = useRunSummary(runId);

  if (!runId) {
    return <MissingResourceCard resourceName="run" linkTo="/" linkLabel="Create a new session" />;
  }

  if (runQuery.isLoading) {
    return <LoadingCard message="Loading run…" />;
  }

  if (runQuery.isError || !runQuery.data) {
    return (
      <ErrorCard
        title="Could not load run"
        message="Check that the run exists and that the backend is running."
        linkTo="/"
        linkLabel="Create a new session"
      />
    );
  }

  const run = runQuery.data;
  const commands = run.commands ?? [];
  const summary = summaryQuery.data;

  return (
    <PageLayout>
      <PageCard>
        <RunStatusBadge status={run.status} />
        <h2>Verification run</h2>
        <p>Run ID: <code>{run.id}</code></p>
        <p>Session ID: <Link to={`/sessions/${run.sessionId}`}>{run.sessionId}</Link></p>
        <p>Plan: {run.planId ?? 'Not selected yet'}</p>
        <p>Network mode: {run.networkMode ?? 'Not recorded'}</p>
        <p>Duration: {formatDuration(run.durationMillis)}</p>
        {run.summary ? <p>{run.summary}</p> : null}
      </PageCard>

      {summary ? (
        <PageCard>
          <h2>Summary</h2>
          <p>{summary.summary ?? 'No summary is available yet.'}</p>
          <p>Commands run: {(summary.commandsRun ?? []).length > 0 ? (summary.commandsRun ?? []).join(', ') : 'None yet'}</p>
          <FailureSummaryCard summary={summary} />
        </PageCard>
      ) : null}

      <PageCard>
        <h2>Command results</h2>
        <CommandResultTable commands={commands} />
      </PageCard>

      <PageCard>
        <h2>Log excerpts</h2>
        <LogExcerptPanel commands={commands} />
      </PageCard>

      <PageCard>
        <h2>Artifacts</h2>
        <ArtifactList runId={run.id} />
      </PageCard>
    </PageLayout>
  );
}
