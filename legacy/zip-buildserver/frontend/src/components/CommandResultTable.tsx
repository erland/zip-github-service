import type { RunCommand } from '../api/types';
import { formatDuration } from '../utils/format';
import styles from './CommandResultTable.module.css';

interface CommandResultTableProps {
  commands?: RunCommand[];
}


export function CommandResultTable({ commands = [] }: CommandResultTableProps) {
  if (commands.length === 0) {
    return <p className={styles.empty}>No command results have been recorded yet.</p>;
  }

  return (
    <div className={styles.tableWrap}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>Command</th>
            <th>Status</th>
            <th>Exit</th>
            <th>Duration</th>
            <th>Failure</th>
          </tr>
        </thead>
        <tbody>
          {commands.map((command) => (
            <tr key={command.id}>
              <td>
                <strong>{command.commandLabel}</strong>
                <div className={styles.code}>{command.commandDisplay}</div>
                <div className={styles.code}>{command.workingDirectory}</div>
              </td>
              <td>{command.status}</td>
              <td>{command.exitCode ?? '—'}</td>
              <td>{formatDuration(command.durationMillis)}</td>
              <td>{command.failureMessage ?? command.failureCategory ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
