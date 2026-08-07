import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { createWorkPullRequest, getProject, getProjectImports, getProjectWork, ImportHistoryItem, ProjectResponse, WorkSessionResponse } from '../api/projects';

export default function ProjectDetailPage() {
  const { projectId } = useParams();
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [imports, setImports] = useState<ImportHistoryItem[]>([]);
  const [work, setWork] = useState<WorkSessionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [finishing, setFinishing] = useState(false);
  const [completedPullRequestUrl, setCompletedPullRequestUrl] = useState('');

  async function load() {
    if (!projectId) { setError('Projekt-ID saknas.'); setLoading(false); return; }
    try {
      const [loadedProject, loadedImports, loadedWork] = await Promise.all([getProject(projectId), getProjectImports(projectId), getProjectWork(projectId)]);
      setProject(loadedProject); setImports(loadedImports); setWork(loadedWork);
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Projektet kunde inte hämtas.'); }
    finally { setLoading(false); }
  }
  useEffect(() => { void load(); }, [projectId]);

  async function finishWork() {
    if (!projectId || finishing) return;
    setFinishing(true); setError('');
    try { const created = await createWorkPullRequest(projectId); setCompletedPullRequestUrl(created.pullRequestUrl); await load(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Pull request kunde inte skapas.'); }
    finally { setFinishing(false); }
  }

  if (loading) return <section className="page-card"><p role="status">Hämtar projekt…</p></section>;
  if (error && !project) return <section className="page-card"><h1>Projektet kunde inte öppnas</h1><p role="alert">{error}</p></section>;
  if (!project) return null;
  return <section className="page-card" aria-labelledby="project-heading">
    <p><Link className="back-link" to="/projects">← Alla projekt</Link></p>
    <div className="page-heading-row"><div><p className="eyebrow">Projekt</p><h1 id="project-heading">{project.name}</h1><p className="lead">{project.repositoryFullName}</p></div><Link className="button" to={`/projects/${project.id}/imports/new`}>{work ? 'Fortsätt arbete' : 'Starta arbete'}</Link></div>
    {error && <p role="alert" className="status-message status-message--error">{error}</p>}
    {completedPullRequestUrl && <p className="status-message" role="status">Arbetets pull request är skapad. <a href={completedPullRequestUrl} target="_blank" rel="noreferrer">Öppna pull request</a></p>}
    <dl className="detail-grid"><div><dt>Repository</dt><dd>{project.repositoryFullName}</dd></div><div><dt>Standardbranch</dt><dd>{project.defaultBranch}</dd></div><div><dt>Åtkomst</dt><dd>{project.privateRepository?'Privat repository':'Publikt repository'}</dd></div><div><dt>Status</dt><dd>{project.active?'Aktivt':'Inaktivt'}</dd></div></dl>
    <section aria-labelledby="work-heading"><h2 id="work-heading">Pågående arbete</h2>
      {!work ? <div className="empty-state"><p>Inget arbete är startat. Första ZIP-importen skapar automatiskt en arbetsbranch.</p></div> : <div className="work-card">
        <p><strong>Arbetsbranch:</strong> <code>{work.branchName}</code></p><p><strong>Bas:</strong> {work.baseBranch}</p>
        <p>{work.headCommitSha ? `Senaste commit: ${work.headCommitSha.slice(0,12)}` : 'Ännu ingen commit i arbetet.'}</p>
        <div className="result-primary-action"><Link className="button button--secondary" to={`/projects/${project.id}/imports/new`}>Ladda upp nästa ZIP</Link><button className="button" type="button" disabled={!work.headCommitSha || finishing} onClick={finishWork}>{finishing?'Skapar pull request…':'Arbetet är klart – skapa pull request'}</button></div>
      </div>}
    </section>
    <section aria-labelledby="import-history-heading"><h2 id="import-history-heading">Importhistorik</h2>{imports.length===0?<div className="empty-state"><p>Det finns ännu inga importer.</p></div>:<ul className="import-history-list">{imports.map(item=><ImportHistoryRow key={item.id} projectId={project.id} item={item}/>)}</ul>}</section>
  </section>;
}
function ImportHistoryRow({projectId,item}:{projectId:string;item:ImportHistoryItem}) { const route=item.resumeStage==='RESULT'?`/projects/${projectId}/imports/${item.id}/result`:item.resumeStage==='REVIEW'?`/projects/${projectId}/imports/${item.id}/review`:`/projects/${projectId}/imports/new?importId=${encodeURIComponent(item.id)}`; const linkLabel=item.resumeStage==='RESULT'?'Öppna resultat':item.resumeStage==='REVIEW'?'Fortsätt granska':'Fortsätt import'; return <li className="import-history-item"><div><strong>{item.sourceFilename||'Import utan ZIP'}</strong><p>{new Date(item.createdAt).toLocaleString('sv-SE')} · {item.baseBranch}</p></div><span className="status-badge">{item.status}</span><Link className="button button--secondary" to={route}>{linkLabel}</Link></li>; }
