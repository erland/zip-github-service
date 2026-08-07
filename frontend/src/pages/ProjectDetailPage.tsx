import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  createWorkPullRequest,
  getProject,
  getProjectImports,
  getProjectWork,
  getProjectWorkCommits,
  ImportHistoryItem,
  ProjectResponse,
  WorkCommit,
  WorkSessionResponse,
} from '../api/projects';

export default function ProjectDetailPage() {
  const { projectId } = useParams();
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [activeImport, setActiveImport] = useState<ImportHistoryItem | null>(null);
  const [work, setWork] = useState<WorkSessionResponse | null>(null);
  const [commits, setCommits] = useState<WorkCommit[]>([]);
  const [githubHistoryAvailable, setGithubHistoryAvailable] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [finishing, setFinishing] = useState(false);
  const [completedPullRequestUrl, setCompletedPullRequestUrl] = useState('');

  async function load() {
    if (!projectId) { setError('Projekt-ID saknas.'); setLoading(false); return; }
    try {
      const [loadedProject, loadedImports, loadedWork, loadedHistory] = await Promise.all([
        getProject(projectId), getProjectImports(projectId), getProjectWork(projectId), getProjectWorkCommits(projectId),
      ]);
      setProject(loadedProject);
      setWork(loadedWork);
      setCommits(loadedHistory.commits);
      setGithubHistoryAvailable(loadedHistory.githubAvailable);
      setActiveImport(loadedImports.find(item => item.resumeStage !== 'RESULT') ?? null);
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

  const branchUrl = work ? `https://github.com/${project.repositoryFullName}/tree/${encodeURIComponent(work.branchName)}` : null;
  return <section className="page-card" aria-labelledby="project-heading">
    <p><Link className="back-link" to="/projects">← Alla projekt</Link></p>
    <div className="page-heading-row"><div><p className="eyebrow">Projekt</p><h1 id="project-heading">{project.name}</h1><p className="lead">{project.repositoryFullName}</p></div><Link className="button" to={`/projects/${project.id}/imports/new`}>{work ? 'Fortsätt arbete' : 'Starta arbete'}</Link></div>
    {error && <p role="alert" className="status-message status-message--error">{error}</p>}
    {completedPullRequestUrl && <p className="status-message" role="status">Arbetets pull request är skapad. <a href={completedPullRequestUrl} target="_blank" rel="noreferrer">Öppna pull request</a></p>}
    <dl className="detail-grid"><div><dt>Repository</dt><dd>{project.repositoryFullName}</dd></div><div><dt>Standardbranch</dt><dd>{project.defaultBranch}</dd></div><div><dt>Åtkomst</dt><dd>{project.privateRepository?'Privat repository':'Publikt repository'}</dd></div><div><dt>Status</dt><dd>{project.active?'Aktivt':'Inaktivt'}</dd></div></dl>

    <section aria-labelledby="work-heading"><h2 id="work-heading">Pågående arbete</h2>
      {!work ? <div className="empty-state"><p>Inget arbete är startat. Första ZIP-importen skapar automatiskt en arbetsbranch.</p></div> : <div className="work-card">
        <p><strong>Arbetsbranch:</strong> <code>{work.branchName}</code>{branchUrl && <> · <a href={branchUrl} target="_blank" rel="noreferrer">Öppna på GitHub</a></>}</p><p><strong>Bas:</strong> {work.baseBranch}</p>
        {activeImport && <ActiveImportCard projectId={project.id} item={activeImport} />}
        <section aria-labelledby="work-history-heading" className="work-history">
          <div className="review-list-heading"><div><h3 id="work-history-heading">Commits i arbetet</h3><p>Git-historiken på arbetsbranchen är arbetets primära historik.</p></div></div>
          {!githubHistoryAvailable && <p className="status-message" role="status">GitHub-historiken kunde inte läsas just nu. Senaste lokalt kända commit visas.</p>}
          {commits.length === 0 ? <div className="empty-state"><p>Ännu ingen commit i arbetet.</p></div> : <ol className="work-commit-list">{commits.map(commit => <WorkCommitRow key={commit.sha} commit={commit} />)}</ol>}
        </section>
        <div className="result-primary-action"><Link className="button button--secondary" to={`/projects/${project.id}/imports/new`}>Ladda upp nästa ZIP</Link><button className="button" type="button" disabled={!work.headCommitSha || finishing || Boolean(activeImport)} onClick={finishWork}>{finishing?'Skapar pull request…':'Arbetet är klart – skapa pull request'}</button></div>
      </div>}
    </section>
  </section>;
}

function ActiveImportCard({projectId, item}:{projectId:string; item:ImportHistoryItem}) {
  const route=item.resumeStage==='REVIEW'?`/projects/${projectId}/imports/${item.id}/review`:`/projects/${projectId}/imports/new?importId=${encodeURIComponent(item.id)}`;
  const label=item.resumeStage==='REVIEW'?'Fortsätt granska':'Fortsätt import';
  return <aside className="active-import-card" aria-labelledby="active-import-heading">
    <div><p className="eyebrow">Pågående import</p><h3 id="active-import-heading">{item.sourceFilename || 'Import utan ZIP'}</h3><p>Status: <span className="status-badge">{item.status}</span></p></div>
    <Link className="button button--secondary" to={route}>{label}</Link>
  </aside>;
}

function WorkCommitRow({commit}:{commit:WorkCommit}) {
  const firstLine = commit.message.split('\n', 1)[0] || '(utan commitmeddelande)';
  return <li className="work-commit-item">
    <div><strong>{firstLine}</strong><p><code>{commit.sha.slice(0, 12)}</code>{commit.authorName ? ` · ${commit.authorName}` : ''} · {new Date(commit.authoredAt).toLocaleString('sv-SE')}</p></div>
    {commit.htmlUrl && <a className="button button--secondary" href={commit.htmlUrl} target="_blank" rel="noreferrer">Öppna commit</a>}
  </li>;
}
