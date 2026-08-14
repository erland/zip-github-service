import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { getRepositories, startRepositoryWork, type RepositoryEntry } from '../api/repositories';

export default function RepositoryDetailPage() {
  const { installationId, repositoryId } = useParams();
  const navigate = useNavigate();
  const [repository, setRepository] = useState<RepositoryEntry | null>(null);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    getRepositories().then((items) => {
      if (cancelled) return;
      const found = items.find((item) => String(item.githubInstallationId) === installationId && String(item.githubRepositoryId) === repositoryId) ?? null;
      if (found?.projectId) { navigate(`/projects/${found.projectId}`, { replace: true }); return; }
      setRepository(found);
    }).catch((reason) => { if (!cancelled) setError(reason instanceof Error ? reason.message : 'Repositoryt kunde inte hämtas.'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [installationId, repositoryId, navigate]);

  async function startFirstImport() {
    if (!repository || starting) return;
    setStarting(true); setError('');
    try {
      const result = await startRepositoryWork(repository.githubInstallationId, repository.githubRepositoryId);
      navigate(`/projects/${result.project.id}/imports/new`, { replace: true });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Repositoryt kunde inte förberedas för den första ZIP-filen.');
      setStarting(false);
    }
  }

  if (loading) return <section className="page-card"><p role="status">Hämtar repository…</p></section>;
  if (!repository) return <section className="page-card"><p><Link className="back-link" to="/projects">← Repositories</Link></p><h1>Repositoryt kunde inte öppnas</h1>{error && <p role="alert" className="status-message status-message--error">{error}</p>}</section>;

  return <section className="page-card" aria-labelledby="repository-heading">
    <p><Link className="back-link" to="/projects">← Repositories</Link></p>
    <p className="eyebrow">Repository</p>
    <h1 id="repository-heading">{repository.repositoryName}</h1>
    <p className="lead">Ladda upp den första ZIP-filen. zip-github skapar projekt och arbetsbranch automatiskt innan uploaden öppnas.</p>
    {error && <p role="alert" className="status-message status-message--error">{error}</p>}
    <div className="result-primary-action"><button className="button" type="button" disabled={starting} onClick={()=>void startFirstImport()}>{starting ? 'Förbereder repository…' : 'Ladda upp första ZIP'}</button></div>
  </section>;
}
