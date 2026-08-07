import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getProject, getProjectImports, ImportHistoryItem, ProjectResponse } from '../api/projects';

export default function ProjectDetailPage() {
  const { projectId } = useParams();
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [imports, setImports] = useState<ImportHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!projectId) { setError('Projekt-ID saknas.'); setLoading(false); return; }
    let active = true;
    Promise.all([getProject(projectId), getProjectImports(projectId)])
      .then(([loadedProject, loadedImports]) => { if (active) { setProject(loadedProject); setImports(loadedImports); } })
      .catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : 'Projektet kunde inte hämtas.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [projectId]);

  if (loading) return <section className="page-card"><p role="status">Hämtar projekt och importhistorik…</p></section>;
  if (error || !project) return <section className="page-card"><h1>Projektet kunde inte öppnas</h1><p role="alert">{error}</p><Link className="button" to="/projects">Till projektlistan</Link></section>;

  return (
    <section className="page-card" aria-labelledby="project-heading">
      <p><Link className="back-link" to="/projects">← Alla projekt</Link></p>
      <div className="page-heading-row">
        <div><p className="eyebrow">Projekt</p><h1 id="project-heading">{project.name}</h1><p className="lead">{project.repositoryFullName}</p></div>
        <Link className="button" to={`/projects/${project.id}/imports/new`}>Ny import</Link>
      </div>
      <dl className="detail-grid">
        <div><dt>Repository</dt><dd>{project.repositoryFullName}</dd></div>
        <div><dt>Standardbranch</dt><dd>{project.defaultBranch}</dd></div>
        <div><dt>Åtkomst</dt><dd>{project.privateRepository ? 'Privat repository' : 'Publikt repository'}</dd></div>
        <div><dt>Status</dt><dd>{project.active ? 'Aktivt' : 'Inaktivt'}</dd></div>
      </dl>
      <section aria-labelledby="import-history-heading">
        <h2 id="import-history-heading">Importhistorik</h2>
        {imports.length === 0 ? <div className="empty-state"><p>Det finns ännu inga importer för projektet.</p></div> :
          <ul className="import-history-list">{imports.map((item) => <ImportHistoryRow key={item.id} projectId={project.id} item={item} />)}</ul>}
      </section>
    </section>
  );
}

function ImportHistoryRow({ projectId, item }: { projectId: string; item: ImportHistoryItem }) {
  const route = item.resumeStage === 'RESULT'
    ? `/projects/${projectId}/imports/${item.id}/result`
    : item.resumeStage === 'REVIEW'
      ? `/projects/${projectId}/imports/${item.id}/review`
      : `/projects/${projectId}/imports/new?importId=${encodeURIComponent(item.id)}`;
  const action = item.resumeStage === 'RESULT' ? 'Öppna resultat' : item.resumeStage === 'REVIEW' ? 'Fortsätt granska' : 'Fortsätt uppladdning';
  return <li className="import-history-item">
    <div><strong>{item.sourceFilename || 'Import utan uppladdad ZIP'}</strong><p>{formatDate(item.createdAt)} · branch {item.baseBranch}</p></div>
    <div><span className="status-badge">{statusLabel(item.status)}</span>{item.pullRequestNumber && <span>PR #{item.pullRequestNumber}</span>}</div>
    <Link className="button button--secondary" to={route}>{action}</Link>
  </li>;
}

function statusLabel(status: string) {
  const labels: Record<string, string> = { CREATED: 'Skapad', UPLOADING: 'ZIP uppladdad', INSPECTING: 'Analyseras', READY_FOR_REVIEW: 'Klar för granskning', BLOCKED: 'Blockerad', APPROVED: 'Godkänd', FILES_APPLIED: 'Arbetsyta klar', PUSHED: 'Pushad', PULL_REQUEST_CREATED: 'Pull request skapad' };
  return labels[status] || status;
}
function formatDate(value: string) { return new Intl.DateTimeFormat('sv-SE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)); }
