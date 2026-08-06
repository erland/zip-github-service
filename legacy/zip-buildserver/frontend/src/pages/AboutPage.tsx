import styles from './Page.module.css';

export function AboutPage() {
  return (
    <section className={styles.page}>
      <div className={styles.card}>
        <h2>About zip-buildserver</h2>
        <p>
          zip-buildserver is a self-hosted verification service for development-time build and test checks.
          Uploaded source code is treated as untrusted and should run only through approved isolated workers.
        </p>
      </div>
    </section>
  );
}
