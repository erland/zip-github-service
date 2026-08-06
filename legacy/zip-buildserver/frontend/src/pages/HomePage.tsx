import { SessionCreateForm } from '../components/SessionCreateForm';
import { StatusBadge } from '../components/StatusBadge';
import styles from './Page.module.css';

export function HomePage() {
  return (
    <section className={styles.page}>
      <div className={styles.card}>
        <StatusBadge label="MVP workflow" />
        <h2>Create a verification session</h2>
        <p>
          Start a session, upload a source-code zip package, and let the backend validate the archive
          before later steps add run controls and live verification reports.
        </p>
        <SessionCreateForm />
      </div>
    </section>
  );
}
