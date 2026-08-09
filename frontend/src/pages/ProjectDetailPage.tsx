import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
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
import { cancelImport, dispatchImportWorkflow, getImportActionsControlOptions, ImportActionsControlOptionsResponse, ImportActionsDetailsResponse, ImportActionsStatusResponse, rerunImportWorkflowFailedJobs } from '../api/imports';
import ActionsPanel from '../components/ActionsPanel';
import ActionsControls from '../components/ActionsControls';
import PullRequestComposer from '../components/PullRequestComposer';

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
  const [showPullRequestComposer, setShowPullRequestComposer] = useState(false);
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

  if (loading) return <section className="page-card"><p role="status">Hämtar repository…</p></section>;
  if (error && !project) return <section className="page-card"><h1>Projektet kunde inte öppnas</h1><p role="alert">{error}</p></section>;
  if (!project) return null;

  const repositoryUrl = `https://github.com/${project.repositoryFullName}/tree/${encodeURIComponent(project.defaultBranch)}`;
  const branchUrl = work ? `https://github.com/${project.repositoryFullName}/tree/${encodeURIComponent(work.branchName)}` : null;
  return <section className="page-card" aria-labelledby="project-heading">
    <p><Link className="back-link" to="/projects">← Repositories</Link></p>
    <div className="page-heading-row"><div><p className="eyebrow">Repository</p><h1 id="project-heading">{project.repositoryFullName.split('/').at(-1) ?? project.repositoryFullName}</h1><p className="lead"><a href={repositoryUrl} target="_blank" rel="noreferrer">{project.repositoryFullName}</a></p></div>{!activeImport && work && <Link className="button" to={`/projects/${project.id}/imports/new`}>Ladda upp nästa ZIP</Link>}</div>
    {error && <p role="alert" className="status-message status-message--error">{error}</p>}
    {completedPullRequestUrl && <p className="status-message" role="status">Arbetets pull request är skapad och arbetet fortsätter tills PR:n mergas eller avslutas. <a href={completedPullRequestUrl} target="_blank" rel="noreferrer">Öppna pull request</a></p>}
    <dl className="detail-grid"><div><dt>Repository</dt><dd>{project.repositoryFullName}</dd></div><div><dt>Standardbranch</dt><dd>{project.defaultBranch}</dd></div><div><dt>Åtkomst</dt><dd>{project.privateRepository?'Privat repository':'Publikt repository'}</dd></div><div><dt>Status</dt><dd>{project.active?'Aktivt':'Inaktivt'}</dd></div></dl>

    <section aria-labelledby="work-heading"><h2 id="work-heading">Pågående arbete</h2>
      {!work ? <div className="empty-state"><p>Inget arbete är startat.</p>
        <div className="result-primary-action"><button className="button" type="button" disabled={workBusy} onClick={()=>void startWork(false)}>{workBusy?'Startar…':'Skapa ny Work-branch'}</button></div>
        {branches.length > 0 && <div className="identity-fields"><label htmlFor="existing-work-branch">Eller fortsätt på befintlig branch</label><select id="existing-work-branch" value={existingBranch} onChange={e=>setExistingBranch(e.target.value)}><option value="">Välj branch…</option>{branches.map(branch=><option key={branch.name} value={branch.name}>{branch.name}</option>)}</select><button className="button button--secondary" type="button" disabled={!existingBranch || workBusy} onClick={()=>void startWork(true)}>Fortsätt på vald branch</button></div>}
        <p>När arbetet är startat kan du ladda upp en ZIP. Shortcut-flödet skapar automatiskt en ny verifierad Work-branch om ingen finns.</p>
      </div> : <div className="work-card">
        <p><strong>Arbetsbranch:</strong> <code>{work.branchName}</code>{branchUrl && <> · <a href={branchUrl} target="_blank" rel="noreferrer">Öppna på GitHub</a></>}</p><p><strong>Bas:</strong> {work.baseBranch}</p>
        {work.pullRequestUrl && <aside className="status-message" aria-label="Pull request-status"><strong>Pull request #{work.pullRequestNumber}</strong> · <span className="status-badge">{work.status === 'PR_OPEN' ? 'Öppen' : work.status === 'PR_CLOSED' ? 'Stängd' : work.status}</span> · <a href={work.pullRequestUrl} target="_blank" rel="noreferrer">Öppna på GitHub</a><p>{work.status === 'PR_OPEN' ? 'Du kan fortsätta ladda upp ZIP-filer. Nya commits pushas till samma Work-branch och uppdaterar automatiskt denna PR.' : 'PR:n är stängd utan merge. Du kan fortsätta arbetet och skapa en ny PR, eller avsluta Work.'}</p></aside>}
        {work.branchChangedExternally && <aside className="status-message status-message--warning" aria-label="Work-branchen har ändrats externt"><strong>Work-branchen har ändrats på GitHub.</strong><p>zip-github visar Actions för aktuell remote HEAD. Nästa ZIP granskas mot den aktuella branchen och markerar filer där ZIP:en skulle ersätta senare GitHub-ändringar.</p>{work.headCommitSha && work.remoteHeadCommitSha && <p><code>{work.headCommitSha.slice(0,12)}</code> → <code>{work.remoteHeadCommitSha.slice(0,12)}</code></p>}</aside>}
        {activeImport && <ActiveImportCard projectId={project.id} item={activeImport} onCancelled={load} />}
        {(work.remoteHeadCommitSha || work.headCommitSha) && <WorkActionsPanel projectId={project.id} importId={work.lastImportId} repositoryFullName={project.repositoryFullName} branchName={work.branchName} expectedCommitSha={work.remoteHeadCommitSha || work.headCommitSha!} />}
        <section aria-labelledby="work-history-heading" className="work-history">
          <div className="review-list-heading"><div><h3 id="work-history-heading">Commits i arbetet</h3><p>Git-historiken på arbetsbranchen är arbetets primära historik.</p></div></div>
          {!githubHistoryAvailable && <p className="status-message" role="status">GitHub-historiken kunde inte läsas just nu. Senaste lokalt kända commit visas.</p>}
          {commits.length === 0 ? <div className="empty-state"><p>Ännu ingen commit i arbetet.</p></div> : <ol className="work-commit-list">{commits.map(commit => <WorkCommitRow key={commit.sha} commit={commit} />)}</ol>}
        </section>
        {showPullRequestComposer && projectId && work.status !== 'PR_OPEN' && <PullRequestComposer projectId={projectId}
          onCreated={async created => { setCompletedPullRequestUrl(created.pullRequestUrl); setShowPullRequestComposer(false); await load(); }}
          onCancel={() => setShowPullRequestComposer(false)} />}
        <div className="result-primary-action">{work.status !== 'PR_OPEN' && !showPullRequestComposer && <button className="button" type="button" disabled={!work.headCommitSha || Boolean(activeImport)} onClick={()=>setShowPullRequestComposer(true)}>{work.status === 'PR_CLOSED'?'Skapa ny pull request':'Skapa pull request'}</button>}{!abandonConfirm ? <button className="button button--secondary" type="button" disabled={Boolean(activeImport) || workBusy} onClick={()=>setAbandonConfirm(true)}>{work.status === 'PR_OPEN'?'Avsluta Work':'Avsluta utan PR'}</button> : <div><label className="checkbox-row"><input type="checkbox" checked={deleteWorkBranch} onChange={e=>setDeleteWorkBranch(e.target.checked)} /> Ta även bort Work-branchen från GitHub</label><button className="button" type="button" disabled={workBusy} onClick={()=>void abandonWork()}>{workBusy?'Avslutar…':'Bekräfta avslut'}</button><button className="button button--secondary" type="button" disabled={workBusy} onClick={()=>setAbandonConfirm(false)}>Behåll arbetet</button></div>}</div>
      </div>}
    </section>
    <section aria-labelledby="project-actions-heading"><h2 id="project-actions-heading">Repositoryåtgärder</h2>{!archiveConfirm ? <button className="button button--secondary" type="button" disabled={Boolean(work)} onClick={()=>setArchiveConfirm(true)}>Ta bort från zip-github</button> : <div className="status-message"><p>Repositoryts zip-github-koppling tas bort från den aktiva vyn men historiken behålls. GitHub-repositoryt påverkas inte.</p><button className="button" type="button" disabled={workBusy} onClick={()=>void removeProject()}>Ja, ta bort från zip-github</button><button className="button button--secondary" type="button" onClick={()=>setArchiveConfirm(false)}>Avbryt</button></div>}</section>
  </section>;
}

function WorkActionsPanel({projectId, importId, repositoryFullName, branchName, expectedCommitSha}:{projectId:string; importId:string|null; repositoryFullName:string; branchName:string; expectedCommitSha:string}) {
  const [actions, setActions] = useState<ImportActionsStatusResponse | null>(null);
  const [details, setDetails] = useState<ImportActionsDetailsResponse | null>(null);
  const [detailsUnavailable, setDetailsUnavailable] = useState(false);
  const [busy, setBusy] = useState(false);
  const [controlOptions, setControlOptions] = useState<ImportActionsControlOptionsResponse | null>(null);
  const [controlBusy, setControlBusy] = useState('');
  const [controlMessage, setControlMessage] = useState('');
  const [controlError, setControlError] = useState('');
  const operationKeys = useRef(new Map<string,string>());

  async function refresh() {
    if (busy) return;
    setBusy(true);
    try {
      const next = await getProjectWorkActions(projectId);
      if (next.commitSha !== expectedCommitSha) throw new Error('Actions-statusen hör inte till aktuell Work-commit.');
      setActions(next);
      if (['failure','cancelled','success'].includes(next.state)) {
        setDetailsUnavailable(false);
        const nextDetails = await getProjectWorkActionDetails(projectId).catch(() => null);
        if (nextDetails && nextDetails.commitSha === expectedCommitSha) setDetails(nextDetails); else setDetailsUnavailable(true);
      } else setDetails(null);
    } catch (reason) {
      setActions({ importId: importId ?? '', repositoryFullName, commitSha: expectedCommitSha, state: 'unavailable', terminal: false,
        detailsUrl: `https://github.com/${repositoryFullName}/actions`, workflows: [], checks: [], diagnosticCode: 'WORK_ACTIONS_API_UNAVAILABLE',
        diagnosticMessage: reason instanceof Error ? reason.message : 'Actions-status kunde inte hämtas.', checkedAt: new Date().toISOString() });
    } finally { setBusy(false); }
  }

  useEffect(() => { void refresh(); }, [projectId, expectedCommitSha]);
  useEffect(() => {
    if (!importId) { setControlOptions(null); return; }
    let cancelled = false;
    getImportActionsControlOptions(importId).then(options => { if (!cancelled) setControlOptions(options); }).catch(() => { if (!cancelled) setControlOptions(null); });
    return () => { cancelled = true; };
  }, [importId, expectedCommitSha]);
  useEffect(() => {
    if (!actions || actions.terminal || !['pending','queued','in_progress'].includes(actions.state)) return;
    const timer = window.setTimeout(() => { void refresh(); }, 10000);
    return () => window.clearTimeout(timer);
  }, [actions?.state, actions?.checkedAt]);

  function idempotencyKey(target:string) {
    const existing = operationKeys.current.get(target);
    if (existing) return existing;
    const key = crypto.randomUUID();
    operationKeys.current.set(target, key);
    return key;
  }

  async function dispatchWorkflow(identifier:string, name:string) {
    if (!importId || !controlOptions || controlBusy) return;
    const target = `dispatch:${identifier}:${controlOptions.commitSha}`;
    setControlBusy(target); setControlError(''); setControlMessage('');
    try {
      const operation = await dispatchImportWorkflow(importId, identifier, controlOptions.branchRef, controlOptions.commitSha, idempotencyKey(target));
      if (operation.status !== 'SUCCEEDED') { setControlError('Workflow kunde inte startas säkert. Uppdatera status före ett nytt försök.'); return; }
      setControlMessage(`${name} startades för ${controlOptions.branchRef} @ ${controlOptions.commitSha.slice(0,12)}.`);
      await refresh();
    } catch (reason) { setControlError(reason instanceof Error ? reason.message : 'Workflow kunde inte startas.'); }
    finally { setControlBusy(''); }
  }

  async function rerunWorkflow(runId:number, name:string) {
    if (!importId || !controlOptions || controlBusy) return;
    const target = `rerun:${runId}:${controlOptions.commitSha}`;
    setControlBusy(target); setControlError(''); setControlMessage('');
    try {
      const operation = await rerunImportWorkflowFailedJobs(importId, runId, controlOptions.branchRef, controlOptions.commitSha, idempotencyKey(target));
      if (operation.status !== 'SUCCEEDED') { setControlError('Omkörningen kunde inte startas säkert. Uppdatera status före ett nytt försök.'); return; }
      setControlMessage(`Misslyckade jobb i ${name} köas om för samma Work-commit.`);
      await refresh();
    } catch (reason) { setControlError(reason instanceof Error ? reason.message : 'Workflow kunde inte köras om.'); }
    finally { setControlBusy(''); }
  }

  const fallbackUrl = `https://github.com/${repositoryFullName}/actions?query=${encodeURIComponent(`branch:${branchName}`)}`;
  return <ActionsPanel actions={actions} details={details} detailsUnavailable={detailsUnavailable} fallbackUrl={fallbackUrl}
    repositoryFullName={repositoryFullName} branchName={branchName} commitSha={expectedCommitSha} refreshing={busy} onRefresh={() => void refresh()} headingLevel="h3"
    controls={<ActionsControls options={controlOptions} workflows={actions?.workflows ?? []} busy={controlBusy} message={controlMessage} error={controlError} onDispatch={dispatchWorkflow} onRerun={rerunWorkflow} />} />;
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
