import { StatusBadge } from './StatusBadge';
import type { RunStatus } from '../api/types';

interface RunStatusBadgeProps {
  status: RunStatus;
}

const labels: Record<RunStatus, string> = {
  QUEUED: 'Queued',
  RUNNING: 'Running',
  PASSED: 'Passed',
  FAILED: 'Failed',
  REJECTED: 'Rejected',
  TIMED_OUT: 'Timed out',
  CANCELLED: 'Cancelled',
  INCOMPLETE: 'Incomplete',
  INTERNAL_ERROR: 'Internal error',
};

export function RunStatusBadge({ status }: RunStatusBadgeProps) {
  return <StatusBadge label={labels[status] ?? status} />;
}
