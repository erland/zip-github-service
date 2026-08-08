import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  createWorkPullRequest,
  abandonProjectWork, archiveProject, getAvailableWorkBranches, startProjectWork, WorkBranch,
  getProject,
  getProjectImports,
  getProjectWork,
  getProjectWorkCommits, getProjectWorkActions, getProjectWorkActionDetails,
  ImportHistoryItem,
  ProjectResponse,
  WorkCommit,
  WorkSessionResponse,
} from '../api/projects';
import { cancelImport, ImportActionsDetailsResponse, ImportActionsStatusResponse } from '../api/imports';

export default function ProjectDetailPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [activeImport, setActiveImport] = useState<ImportHistoryItem | null>(null);
  const [work, setWork] = useState<WorkSessionResponse | null>(null);
  const [commits, setCommits] = useState<WorkCommit[]>([]);
  const [githubHistoryAvailable, setGithubHistoryAvailable] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [finishing, setFinishing] = useState(false);
  const [completedPullRequestUrl, setCompletedPullRequestUrl] = useState('');
  const [branches, setBranches] = useState<WorkBranch[]>([]);
  const [existingBranch, setExistingBranch] = useState('');
  const [workBusy, setWorkBusy] = useState(false);
  const [abandonConfirm, setAbandonConfirm] = useState(false);
  const [deleteWorkBranch, setDeleteWorkBranch] = useState(false);
  const [archiveConfirm, setArchiveConfirm] = useState(false);

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
      if (!loadedWork) { try { setBranches(await getAvailableWorkBranches(projectId)); } catch { setBranches([]); } } else setBranches([]);
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


  async function startWork(resume: boolean) {
    if (!projectId || workBusy) return;
    setWorkBusy(true); setError('');
    try { await startProjectWork(projectId, resume ? existingBranch : undefined); await load(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Arbetet kunde inte startas.'); }
    finally { setWorkBusy(false); }
  }

  async function abandonWork() {
    if (!projectId || workBusy) return;
    setWorkBusy(true); setError('');
    try { await abandonProjectWork(projectId, deleteWorkBranch); setAbandonConfirm(false); setDeleteWorkBranch(false); await load(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Arbetet kunde inte avslutas.'); }
    finally { setWorkBusy(false); }
  }

  async function removeProject() {
    if (!projectId || workBusy) return;
    setWorkBusy(true); setError('');
    try { await archiveProject(projectId); navigate('/projects', { replace: true }); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Projektet kunde inte tas bort.'); setWorkBusy(false); }
  }

  if (loading) return <section className="page-card"><p role="status">Hämtar projekt…</p></section>;
  if (error && !project) return <section className="page-card"><h1>Projektet kunde inte öppnas</h1><p role="alert">{error}</p></section>;
  if (!project) return null;

  const repositoryUrl = `https://github.com/${project.repositoryFullName}/tree/${encodeURIComponent(project.defaultBranch)}`;
  const branchUrl = work ? `https://github.com/${project.repositoryFullName}/tree/${encodeURIComponent(work.branchName)}` : null;
  return <section className="page-card" aria-labelledby="project-heading">
    <p><Link className="back-link" to="/projects">← Alla projekt</Link></p>
    <div className="page-heading-row"><div><p className="eyebrow">Projekt</p><h1 id="project-heading">{project.name}</h1><p className="lead"><a href={repositoryUrl} target="_blank" rel="noreferrer">{project.repositoryFullName}</a></p></div>{!activeImport && work && <Link className="button" to={`/projects/${project.id}/imports/new`}>Ladda upp nästa ZIP</Link>}</div>
    {error && <p role="alert" className="status-message status-message--error">{error}</p>}
    {completedPullRequestUrl && <p className="status-message" role="status">Arbetets pull request är skapad. <a href={completedPullRequestUrl} target="_blank" rel="noreferrer">Öppna pull request</a></p>}
    <dl className="detail-grid"><div><dt>Repository</dt><dd>{project.repositoryFullName}</dd></div><div><dt>Standardbranch</dt><dd>{project.defaultBranch}</dd></div><div><dt>Åtkomst</dt><dd>{project.privateRepository?'Privat repository':'Publikt repository'}</dd></div><div><dt>Status</dt><dd>{project.active?'Aktivt':'Inaktivt'}</dd></div></dl>

    <section aria-labelledby="work-heading"><h2 id="work-heading">Pågående arbete</h2>
      {!work ? <div className="empty-state"><p>Inget arbete är startat.</p>
        <div className="result-primary-action"><button className="button" type="button" disabled={workBusy} onClick={()=>void startWork(false)}>{workBusy?'Startar…':'Skapa ny Work-branch'}</button></div>
        {branches.length > 0 && <div className="identity-fields"><label htmlFor="existing-work-branch">Eller fortsätt på befintlig branch</label><select id="existing-work-branch" value={existingBranch} onChange={e=>setExistingBranch(e.target.value)}><option value="">Välj branch…</option>{branches.map(branch=><option key={branch.name} value={branch.name}>{branch.name}</option>)}</select><button className="button button--secondary" type="button" disabled={!existingBranch || workBusy} onClick={()=>void startWork(true)}>Fortsätt på vald branch</button></div>}
        <p>När arbetet är startat kan du ladda upp en ZIP. Shortcut-flödet skapar automatiskt en ny verifierad Work-branch om ingen finns.</p>
      </div> : <div className="work-card">
        <p><strong>Arbetsbranch:</strong> <code>{work.branchName}</code>{branchUrl && <> · <a href={branchUrl} target="_blank" rel="noreferrer">Öppna på GitHub</a></>}</p><p><strong>Bas:</strong> {work.baseBranch}</p>
        {activeImport && <ActiveImportCard projectId={project.id} item={activeImport} onCancelled={load} />}
        {work.headCommitSha && <WorkActionsPanel projectId={project.id} repositoryFullName={project.repositoryFullName} branchName={work.branchName} expectedCommitSha={work.headCommitSha} />}
        <section aria-labelledby="work-history-heading" className="work-history">
          <div className="review-list-heading"><div><h3 id="work-history-heading">Commits i arbetet</h3><p>Git-historiken på arbetsbranchen är arbetets primära historik.</p></div></div>
          {!githubHistoryAvailable && <p className="status-message" role="status">GitHub-historiken kunde inte läsas just nu. Senaste lokalt kända commit visas.</p>}
          {commits.length === 0 ? <div className="empty-state"><p>Ännu ingen commit i arbetet.</p></div> : <ol className="work-commit-list">{commits.map(commit => <WorkCommitRow key={commit.sha} commit={commit} />)}</ol>}
        </section>
        <div className="result-primary-action"><button className="button" type="button" disabled={!work.headCommitSha || finishing || Boolean(activeImport)} onClick={finishWork}>{finishing?'Skapar pull request…':'Arbetet är klart – skapa pull request'}</button>{!abandonConfirm ? <button className="button button--secondary" type="button" disabled={Boolean(activeImport) || workBusy} onClick={()=>setAbandonConfirm(true)}>Avsluta utan PR</button> : <div><label className="checkbox-row"><input type="checkbox" checked={deleteWorkBranch} onChange={e=>setDeleteWorkBranch(e.target.checked)} /> Ta även bort Work-branchen från GitHub</label><button className="button" type="button" disabled={workBusy} onClick={()=>void abandonWork()}>{workBusy?'Avslutar…':'Bekräfta avslut'}</button><button className="button button--secondary" type="button" disabled={workBusy} onClick={()=>setAbandonConfirm(false)}>Behåll arbetet</button></div>}</div>
      </div>}
    </section>
    <section aria-labelledby="project-actions-heading"><h2 id="project-actions-heading">Projektåtgärder</h2>{!archiveConfirm ? <button className="button button--secondary" type="button" disabled={Boolean(work)} onClick={()=>setArchiveConfirm(true)}>Ta bort projekt</button> : <div className="status-message"><p>Projektet tas bort från den normala listan men historiken behålls. GitHub-repositoryt påverkas inte.</p><button className="button" type="button" disabled={workBusy} onClick={()=>void removeProject()}>Ja, ta bort projektet</button><button className="button button--secondary" type="button" onClick={()=>setArchiveConfirm(false)}>Avbryt</button></div>}</section>
  </section>;
}

function WorkActionsPanel({projectId, repositoryFullName, branchName, expectedCommitSha}:{projectId:string; repositoryFullName:string; branchName:string; expectedCommitSha:string}) {
  const [actions, setActions] = useState<ImportActionsStatusResponse | null>(null);
  const [details, setDetails] = useState<ImportActionsDetailsResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  async function refresh() {
    if (busy) return;
    setBusy(true); setError('');
    try {
      const next = await getProjectWorkActions(projectId);
      if (next.commitSha !== expectedCommitSha) throw new Error('Actions-statusen hör inte till aktuell Work-commit.');
      setActions(next);
      if (['failure','cancelled','success'].includes(next.state)) {
        const nextDetails = await getProjectWorkActionDetails(projectId).catch(() => null);
        if (nextDetails && nextDetails.commitSha === expectedCommitSha) setDetails(nextDetails);
      } else setDetails(null);
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Actions-status kunde inte hämtas.'); }
    finally { setBusy(false); }
  }

  useEffect(() => { void refresh(); }, [projectId, expectedCommitSha]);
  useEffect(() => {
    if (!actions || actions.terminal || !['pending','queued','in_progress'].includes(actions.state)) return;
    const timer = window.setTimeout(() => { void refresh(); }, 10000);
    return () => window.clearTimeout(timer);
  }, [actions?.state, actions?.checkedAt]);

  async function copyFailures() {
    if (!details || details.failures.length === 0) return;
    const blocks = details.failures.map(failure => [
      `Workflow: ${failure.workflowName}`,
      `Job: ${failure.jobName}`,
      `Step: ${failure.stepName}`,
      failure.tool ? `Tool: ${failure.tool}` : '',
      ...failure.lines.slice(0, 80),
      failure.githubUrl ? `GitHub: ${failure.githubUrl}` : '',
    ].filter(Boolean).join('\n'));
    const text = [
      `Repository: ${repositoryFullName}`,
      `Branch: ${branchName}`,
      `Commit: ${expectedCommitSha}`,
      '',
      ...blocks,
    ].join('\n\n').slice(0, 24000);
    try { await navigator.clipboard.writeText(text); setMessage('Felinformation kopierad.'); }
    catch { setError('Felinformationen kunde inte kopieras.'); }
  }

  const fallbackUrl = `https://github.com/${repositoryFullName}/actions?query=branch%3A${encodeURIComponent(branchName)}`;
  return <section className="actions-overview" aria-labelledby="work-actions-heading">
    <div className="review-list-heading"><div><h3 id="work-actions-heading">GitHub Actions</h3><p>Status för aktuell Work-commit <code>{expectedCommitSha.slice(0,12)}</code>.</p></div><button className="button button--secondary" type="button" disabled={busy} onClick={()=>void refresh()}>{busy?'Uppdaterar…':'Uppdatera status'}</button></div>
    {error && <p role="alert" className="status-message status-message--error">{error} <a href={fallbackUrl} target="_blank" rel="noreferrer">Öppna Actions på GitHub</a></p>}
    {!actions && !error && <p role="status">Hämtar workflow-status…</p>}
    {actions && <>
      <p><strong>Status:</strong> <span className="status-badge">{actions.state}</span> · <a href={actions.detailsUrl || fallbackUrl} target="_blank" rel="noreferrer">Öppna Actions på GitHub</a></p>
      {actions.workflows.length === 0 ? <p>Ingen workflow-körning har registrerats för den här committen ännu.</p> : <ol className="actions-list">{actions.workflows.map(workflow => <li key={workflow.id} className="actions-run-card"><div className="actions-item-heading"><div><strong>{workflow.name}</strong><p><code>{workflow.headSha.slice(0,12)}</code> · {workflow.htmlUrl ? <a href={workflow.htmlUrl} target="_blank" rel="noreferrer">Öppna körning</a> : 'GitHub Actions'}</p></div><span className="status-badge">{workflow.state}</span></div>{workflow.jobs.length>0 && <ul className="actions-job-list">{workflow.jobs.map(job=><li key={job.id}>{job.htmlUrl?<a href={job.htmlUrl} target="_blank" rel="noreferrer">{job.name}</a>:job.name} <span className="status-badge">{job.state}</span></li>)}</ul>}</li>)}</ol>}
      {details && details.failures.length > 0 && <div className="status-message status-message--error"><p><strong>Kondenserade fel</strong></p>{details.failures.map(failure=><details key={`${failure.workflowRunId}-${failure.jobId}-${failure.stepName}`}><summary>{failure.workflowName} / {failure.jobName} / {failure.stepName}</summary><pre>{failure.lines.join('\n')}</pre></details>)}<button className="button button--secondary" type="button" onClick={()=>void copyFailures()}>Kopiera fel</button></div>}
      {message && <p role="status" className="status-message">{message}</p>}
    </>}
  </section>;
}

function ActiveImportCard({projectId, item, onCancelled}:{projectId:string; item:ImportHistoryItem; onCancelled:()=>Promise<void>}) {
  const [confirming, setConfirming] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState('');
  const route=item.resumeStage==='REVIEW'?`/projects/${projectId}/imports/${item.id}/review`:`/projects/${projectId}/imports/new?importId=${encodeURIComponent(item.id)}`;
  const label=item.resumeStage==='REVIEW'?'Fortsätt granska':'Fortsätt import';
  async function cancel() {
    if (cancelling) return;
    setCancelling(true); setCancelError('');
    try { await cancelImport(item.id); await onCancelled(); }
    catch (reason) { setCancelError(reason instanceof Error ? reason.message : 'Importen kunde inte avbrytas.'); }
    finally { setCancelling(false); }
  }
  return <aside className="active-import-card" aria-labelledby="active-import-heading">
    <div><p className="eyebrow">Pågående import</p><h3 id="active-import-heading">{item.sourceFilename || 'Import utan ZIP'}</h3><p>Status: <span className="status-badge">{item.status}</span></p>{cancelError && <p role="alert" className="status-message status-message--error">{cancelError}</p>}</div>
    <div className="result-primary-action"><Link className="button button--secondary" to={route}>{label}</Link>{!confirming ? <button className="button button--secondary" type="button" onClick={()=>setConfirming(true)}>Avbryt import</button> : <><button className="button" type="button" disabled={cancelling} onClick={cancel}>{cancelling?'Avbryter…':'Ja, avbryt import'}</button><button className="button button--secondary" type="button" disabled={cancelling} onClick={()=>setConfirming(false)}>Behåll import</button></>}</div>
  </aside>;
}

function WorkCommitRow({commit}:{commit:WorkCommit}) {
  const firstLine = commit.message.split('\n', 1)[0] || '(utan commitmeddelande)';
  return <li className="work-commit-item">
    <div><strong>{firstLine}</strong><p><code>{commit.sha.slice(0, 12)}</code>{commit.authorName ? ` · ${commit.authorName}` : ''} · {new Date(commit.authoredAt).toLocaleString('sv-SE')}</p></div>
    {commit.htmlUrl && <a className="button button--secondary" href={commit.htmlUrl} target="_blank" rel="noreferrer">Öppna commit</a>}
  </li>;
}
