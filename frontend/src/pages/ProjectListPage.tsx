import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getProjects, type ProjectResponse } from '../api/projects';

export default function ProjectListPage() {
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getProjects()
      .then((items) => { if (!cancelled) setProjects(items); })
      .catch((reason: unknown) => { if (!cancelled) setError(reason instanceof Error ? reason.message : 'Projektlistan kunde inte hämtas.'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  return (
    <section className="page-card" aria-labelledby="project-list-heading">
      <div className="page-heading-row">
        <div>
          <p className="eyebrow">Projektöversikt</p>
          <h1 id="project-list-heading">Dina projekt</h1>
          <p className="lead">Koppla ett GitHub-repository och importera sedan en ZIP för granskning och pull request.</p>
        </div>
        <Link className="button button--secondary" to="/projects/new">Skapa projekt</Link>
      </div>

      {loading && <p role="status">Hämtar projekt…</p>}
      {error && <p className="status-message status-message--error" role="alert">{error}</p>}

      {!loading && !error && projects.length === 0 && (
        <div className="empty-state">
          <h2>Inga projekt ännu</h2>
          <p>Skapa ditt första projekt genom att välja en GitHub App-installation och ett repository.</p>
          <Link className="button" to="/projects/new">Skapa första projektet</Link>
        </div>
      )}

      {!loading && !error && projects.length > 0 && (
        <ul className="project-list">
          {projects.map((project) => (
            <li className="project-card" key={project.id}>
              <div>
                <h2><Link to={`/projects/${project.id}`}>{project.name}</Link></h2>
                <p className="repository-name">{project.repositoryFullName}</p>
              </div>
              <dl className="project-meta">
                <div><dt>Standardbranch</dt><dd>{project.defaultBranch}</dd></div>
                <div><dt>Status</dt><dd>{project.active ? 'Aktivt' : 'Inaktivt'}</dd></div>
              </dl>
              <Link className="button" to={`/projects/${project.id}`}>Öppna projekt</Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
