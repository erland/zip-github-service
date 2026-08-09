import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { getRepositories, type RepositoryEntry } from '../api/repositories';

export default function ProjectListPage() {
  const [repositories, setRepositories] = useState<RepositoryEntry[]>([]);
  const [query, setQuery] = useState('');
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

  const duplicateNames = useMemo(() => {
    const counts = new Map<string, number>();
    repositories.forEach((repository) => counts.set(repository.repositoryName.toLowerCase(), (counts.get(repository.repositoryName.toLowerCase()) ?? 0) + 1));
    return counts;
  }, [repositories]);

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return repositories;
    return repositories.filter((repository) => repository.repositoryName.toLowerCase().includes(normalized)
      || repository.repositoryFullName.toLowerCase().includes(normalized));
  }, [repositories, query]);

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

      {!loading && !error && repositories.length > 0 && <>
        <label className="repository-search" htmlFor="repository-filter">
          <span>Sök repositories</span>
          <input id="repository-filter" type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Skriv en del av namnet…" autoComplete="off" />
        </label>
        {filtered.length === 0 ? <p className="empty-filter-result">Inga repositories matchar “{query.trim()}”.</p> : (
          <ul className="repository-list">
            {filtered.map((repository) => {
              const target = repository.projectId
                ? `/projects/${repository.projectId}`
                : `/repositories/${repository.githubInstallationId}/${repository.githubRepositoryId}`;
              const duplicate = (duplicateNames.get(repository.repositoryName.toLowerCase()) ?? 0) > 1;
              return <li key={`${repository.githubInstallationId}:${repository.githubRepositoryId}`}>
                <Link className="repository-list-link" to={target}>
                  <strong>{repository.repositoryName}</strong>
                  {duplicate && <span>{repository.repositoryFullName}</span>}
                </Link>
              </li>;
            })}
          </ul>
        )}
      </>}
    </section>
  );
}
