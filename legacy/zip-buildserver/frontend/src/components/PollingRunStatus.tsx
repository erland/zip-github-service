import { Link } from 'react-router-dom';
import { useRun } from '../api/runs';
import { RunStatusBadge } from './RunStatusBadge';

interface PollingRunStatusProps {
  runId: string;
}

export function PollingRunStatus({ runId }: PollingRunStatusProps) {
  const runQuery = useRun(runId);

  if (runQuery.isLoading) {
    return <span>Loading run…</span>;
  }

  if (runQuery.isError || !runQuery.data) {
    return <span>Run status unavailable.</span>;
  }

  return (
    <span>
      <RunStatusBadge status={runQuery.data.status} />{' '}
      <Link to={`/runs/${runQuery.data.id}`}>View run</Link>
    </span>
  );
}
