import type { RunCommand } from '../api/types';
import styles from './LogExcerptPanel.module.css';

interface LogExcerptPanelProps {
  commands?: RunCommand[];
}

export function LogExcerptPanel({ commands = [] }: LogExcerptPanelProps) {
  const commandsWithLogs = commands.filter((command) => command.logExcerpt);

  if (commandsWithLogs.length === 0) {
    return <p className={styles.empty}>No log excerpts are available yet.</p>;
  }

  return (
    <div className={styles.panel}>
      {commandsWithLogs.map((command) => (
        <section key={command.id} aria-label={`${command.commandLabel} log excerpt`}>
          <h3>{command.commandLabel}</h3>
          <pre className={styles.excerpt}>{command.logExcerpt}</pre>
        </section>
      ))}
    </div>
  );
}
