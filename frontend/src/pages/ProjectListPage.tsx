import { useEffect, useState } from 'react';
import RepositoryPicker from '../components/RepositoryPicker';
import { getRepositories, type RepositoryEntry } from '../api/repositories';

export default function ProjectListPage() {
  const [repositories, setRepositories] = useState<RepositoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getRepositories()
      .then((items) => { if (!cancelled) setRepositories(items); })
      .catch((reason: unknown) => { if (!cancelled) setError(reason instanceof Error ? reason.message : 'Repositorylistan kunde inte hämtas.'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  return (
    <section className="page-card" aria-labelledby="repository-list-heading">
      <div className="page-heading-row">
        <div>
          <p className="eyebrow">GitHub repositories</p>
          <h1 id="repository-list-heading">Repositories</h1>
          <p className="lead">Välj ett repository där zip-github GitHub App är installerad.</p>
        </div>
      </div>

      {loading && <p role="status">Hämtar repositories…</p>}
      {error && <p className="status-message status-message--error" role="alert">{error}</p>}

      {!loading && !error && repositories.length === 0 && (
        <div className="empty-state">
          <h2>Inga repositories tillgängliga</h2>
          <p>Installera zip-github GitHub App på minst ett repository eller ge installationen åtkomst till fler repositories.</p>
          <a className="button" href="https://github.com/settings/installations" target="_blank" rel="noreferrer">Hantera GitHub Apps</a>
        </div>
      )}

      {!loading && !error && repositories.length > 0 && <RepositoryPicker
        repositories={repositories}
        mode="navigate"
        getTarget={(repository) => repository.projectId
          ? `/projects/${repository.projectId}`
          : `/repositories/${repository.githubInstallationId}/${repository.githubRepositoryId}`}
      />}
    </section>
  );
}
