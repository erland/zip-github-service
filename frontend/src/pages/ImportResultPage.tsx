import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { dispatchImportWorkflow, getDelivery, getImportActions, getImportActionDetails, getImportActionsControlOptions, rerunImportWorkflowFailedJobs, GitDeliveryResponse, ImportActionsControlOptionsResponse, ImportActionsDetailsResponse, ImportActionsStatusResponse } from '../api/imports';
import { createWorkPullRequest } from '../api/projects';

const ACTION_POLL_DELAYS_MS = [0, 8000, 15000, 30000, 60000, 60000, 60000, 60000];

export default function ImportResultPage() {
  const { projectId, importId } = useParams();
  const [result, setResult] = useState<GitDeliveryResponse | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [actions, setActions] = useState<ImportActionsStatusResponse | null>(null);
  const [actionDetails, setActionDetails] = useState<ImportActionsDetailsResponse | null>(null);
  const [actionDetailsUnavailable, setActionDetailsUnavailable] = useState(false);
  const [finishing, setFinishing] = useState(false);
  const [pullRequestUrl, setPullRequestUrl] = useState('');
  const [controlOptions, setControlOptions] = useState<ImportActionsControlOptionsResponse | null>(null);
  const [controlBusy, setControlBusy] = useState('');
  const [controlMessage, setControlMessage] = useState('');
  const [controlError, setControlError] = useState('');
  const operationKeys = useRef(new Map<string, string>());

  useEffect(() => {
    if (!importId) { setError('Import-ID saknas.'); setLoading(false); return; }
    getDelivery(importId).then(setResult).catch((reason) => setError(reason instanceof Error ? reason.message : 'Commitresultatet kunde inte hämtas.')).finally(() => setLoading(false));
  }, [importId]);

  useEffect(() => {
    if (!importId || !result) return;
    let cancelled = false;
    getImportActionsControlOptions(importId)
      .then((options) => { if (!cancelled) setControlOptions(options); })
      .catch(() => { if (!cancelled) setControlOptions(null); });
    return () => { cancelled = true; };
  }, [importId, result]);

  useEffect(() => {
    if (!importId || !result) return;
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let attempt = 0;
    const poll = async () => {
      try {
        const status = await getImportActions(importId);
        if (cancelled) return;
        setActions(status);
        if (status.terminal || attempt >= ACTION_POLL_DELAYS_MS.length - 1) return;
      } catch {
        if (cancelled || attempt >= ACTION_POLL_DELAYS_MS.length - 1) return;
      }
      attempt += 1;
      timer = setTimeout(() => void poll(), ACTION_POLL_DELAYS_MS[attempt]);
    };
    void poll();
    return () => { cancelled = true; if (timer) clearTimeout(timer); };
  }, [importId, result]);

  useEffect(() => {
    if (!importId || !actions || !['success', 'failure', 'cancelled'].includes(actions.state)) return;
    let cancelled = false;
    setActionDetailsUnavailable(false);
    getImportActionDetails(importId)
      .then((details) => { if (!cancelled) setActionDetails(details); })
      .catch(() => { if (!cancelled) setActionDetailsUnavailable(true); });
    return () => { cancelled = true; };
  }, [importId, actions?.state]);

  function idempotencyKey(target: string) {
    const existing = operationKeys.current.get(target);
    if (existing) return existing;
    const key = crypto.randomUUID();
    operationKeys.current.set(target, key);
    return key;
  }

  async function dispatchWorkflow(workflowIdentifier: string, workflowName: string) {
    if (!importId || !controlOptions || controlBusy) return;
    const target = `dispatch:${workflowIdentifier}:${controlOptions.commitSha}`;
    setControlBusy(target); setControlError(''); setControlMessage('');
    try {
      const operation = await dispatchImportWorkflow(importId, workflowIdentifier, controlOptions.branchRef,
        controlOptions.commitSha, idempotencyKey(target));
      if (operation.status !== 'SUCCEEDED') {
        setControlError(operation.status === 'FAILED'
          ? 'Ett tidigare försök med samma idempotensnyckel misslyckades eller fick ett osäkert GitHub-svar. Uppdatera sidan före ett nytt explicit försök.'
          : 'Ett tidigare försök med samma idempotensnyckel är fortfarande registrerat som påbörjat. Uppdatera sidan innan du gör ett nytt explicit försök.');
        return;
      }
      setControlMessage(`${workflowName} startades för ${controlOptions.branchRef} @ ${controlOptions.commitSha.slice(0, 12)}.`);
      const refreshed = await getImportActions(importId).catch(() => null);
      if (refreshed) setActions(refreshed);
    } catch (reason) {
      setControlError(reason instanceof Error ? reason.message : 'Workflow kunde inte startas.');
    } finally { setControlBusy(''); }
  }

  async function rerunWorkflow(runId: number, workflowName: string) {
    if (!importId || !controlOptions || controlBusy) return;
    const target = `rerun:${runId}:${controlOptions.commitSha}`;
    setControlBusy(target); setControlError(''); setControlMessage('');
    try {
      const operation = await rerunImportWorkflowFailedJobs(importId, runId, controlOptions.branchRef,
        controlOptions.commitSha, idempotencyKey(target));
      if (operation.status !== 'SUCCEEDED') {
        setControlError(operation.status === 'FAILED'
          ? 'Ett tidigare omkörningsförsök med samma idempotensnyckel misslyckades eller fick ett osäkert GitHub-svar. Uppdatera sidan före ett nytt explicit försök.'
          : 'Ett tidigare omkörningsförsök med samma idempotensnyckel är fortfarande registrerat som påbörjat. Uppdatera sidan innan du gör ett nytt explicit försök.');
        return;
      }
      setControlMessage(`Misslyckade jobb i ${workflowName} köas om för samma ref och commit.`);
      const refreshed = await getImportActions(importId).catch(() => null);
      if (refreshed) setActions(refreshed);
    } catch (reason) {
      setControlError(reason instanceof Error ? reason.message : 'Workflow kunde inte köras om.');
    } finally { setControlBusy(''); }
  }

  async function finishWork() {
    if (!projectId || finishing || pullRequestUrl) return;
    setFinishing(true); setError('');
    try { const created = await createWorkPullRequest(projectId); setPullRequestUrl(created.pullRequestUrl); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Pull request kunde inte skapas.'); }
    finally { setFinishing(false); }
  }

  const links = useMemo(() => result ? githubLinks(result) : null, [result]);
  return <section className="page-card result-page" aria-labelledby="result-heading">
    <p><Link className="back-link" to={projectId ? `/projects/${projectId}` : '/projects'}>← Till projektet</Link></p>
    <p className="eyebrow">Importresultat</p>
    <h1 id="result-heading">Commit skapad</h1>
    <p className="lead">ZIP-importen är committad på projektets arbetsbranch. Du kan fortsätta med nästa ZIP eller avsluta arbetet och skapa en pull request direkt här.</p>
    {loading && <p role="status">Hämtar resultat…</p>}
    {error && <p role="alert" className="status-message status-message--error">{error}</p>}
    {result && links && <>
      <div className="result-success" role="status"><div><strong>Importen är committad</strong><p>Arbetsbranchen är uppdaterad med den godkända ZIP-filen.</p></div><span className="status-badge">PUSHED</span></div>
      {pullRequestUrl && <p className="status-message" role="status">Arbetets pull request är skapad. <a href={pullRequestUrl} target="_blank" rel="noreferrer">Öppna pull request</a></p>}
      <div className="result-primary-action">
        {projectId && <Link className="button" to={`/projects/${projectId}/imports/new`}>Ladda upp nästa ZIP</Link>}
        {projectId && <button className="button button--secondary" type="button" disabled={finishing || Boolean(pullRequestUrl)} onClick={finishWork}>{pullRequestUrl?'Pull request skapad':finishing?'Skapar pull request…':'Arbetet är klart – skapa pull request'}</button>}
      </div>
      <dl className="result-link-grid">
        <ResultLink label="Repository" value={result.repositoryFullName} href={links.repository} />
        <ResultLink label="Arbetsbranch" value={result.branchName} href={links.branch} />
        <ResultLink label="Commit" value={result.commitSha.slice(0,12)} href={links.commit} />
        <ResultLink label="GitHub Actions" value="Öppna Actions" href={links.actions} />
      </dl>
      <ActionsOverview actions={actions} details={actionDetails} detailsUnavailable={actionDetailsUnavailable} fallbackUrl={links.actions}
        controlOptions={controlOptions} controlBusy={controlBusy} controlMessage={controlMessage} controlError={controlError}
        onDispatch={dispatchWorkflow} onRerun={rerunWorkflow} />
    </>}
  </section>;
}

function ActionsOverview({actions, details, detailsUnavailable, fallbackUrl, controlOptions, controlBusy, controlMessage, controlError, onDispatch, onRerun}:{actions:ImportActionsStatusResponse|null; details:ImportActionsDetailsResponse|null; detailsUnavailable:boolean; fallbackUrl:string; controlOptions:ImportActionsControlOptionsResponse|null; controlBusy:string; controlMessage:string; controlError:string; onDispatch:(identifier:string,name:string)=>void; onRerun:(runId:number,name:string)=>void}) {
  if (!actions) return <section className="actions-overview" aria-labelledby="actions-heading"><h2 id="actions-heading">GitHub Actions</h2><p role="status">Hämtar workflow-status…</p></section>;
  if (actions.state === 'unavailable') return <section className="actions-overview" aria-labelledby="actions-heading"><h2 id="actions-heading">GitHub Actions</h2><p className="status-message">Actions-status kunde inte läsas just nu. Commitresultatet är fortfarande tillgängligt. <a href={fallbackUrl} target="_blank" rel="noreferrer">Öppna Actions på GitHub</a>.</p><ActionsControls options={controlOptions} workflows={actions.workflows} busy={controlBusy} message={controlMessage} error={controlError} onDispatch={onDispatch} onRerun={onRerun} /></section>;
  if (actions.state === 'not_started') return <section className="actions-overview" aria-labelledby="actions-heading"><h2 id="actions-heading">GitHub Actions</h2><p className="status-message">Ingen workflow-körning eller check har registrerats för committen ännu. <a href={fallbackUrl} target="_blank" rel="noreferrer">Öppna Actions på GitHub</a>.</p><ActionsControls options={controlOptions} workflows={actions.workflows} busy={controlBusy} message={controlMessage} error={controlError} onDispatch={onDispatch} onRerun={onRerun} /></section>;
  return <section className="actions-overview" aria-labelledby="actions-heading">
    <div className="review-list-heading"><div><h2 id="actions-heading">GitHub Actions</h2><p>Översikten är begränsad; GitHub är källa för fullständig körningsinformation.</p></div><StateBadge state={actions.state} /></div>
    {actions.workflows.length > 0 && <ol className="actions-list">{actions.workflows.map(workflow => <li key={workflow.id} className="actions-run-card">
      <div className="actions-item-heading"><div><strong>{workflow.name}</strong><p>{workflow.event || 'workflow'} · <a href={workflow.htmlUrl || fallbackUrl} target="_blank" rel="noreferrer">Öppna körning på GitHub</a></p></div><StateBadge state={workflow.state} /></div>
      {workflow.jobs.length > 0 && <ul className="actions-job-list">{workflow.jobs.map(job => <li key={job.id}><span>{job.htmlUrl ? <a href={job.htmlUrl} target="_blank" rel="noreferrer">{job.name}</a> : job.name}</span><StateBadge state={job.state} /></li>)}</ul>}
    </li>)}</ol>}
    {actions.checks.length > 0 && <div className="actions-checks"><h3>Checks</h3><ul className="actions-job-list">{actions.checks.map(check => <li key={check.id}><span>{check.htmlUrl ? <a href={check.htmlUrl} target="_blank" rel="noreferrer">{check.name}</a> : check.name}{check.appName ? ` · ${check.appName}` : ''}</span><StateBadge state={check.state} /></li>)}</ul></div>}
    {detailsUnavailable && <p className="status-message">Artifacts och kondenserade fel kunde inte läsas just nu. <a href={fallbackUrl} target="_blank" rel="noreferrer">Öppna Actions på GitHub</a>.</p>}
    {details && <ActionsDetails details={details} />}
    <ActionsControls options={controlOptions} workflows={actions.workflows} busy={controlBusy} message={controlMessage} error={controlError} onDispatch={onDispatch} onRerun={onRerun} />
  </section>;
}

function ActionsControls({options, workflows, busy, message, error, onDispatch, onRerun}:{options:ImportActionsControlOptionsResponse|null; workflows:ImportActionsStatusResponse['workflows']; busy:string; message:string; error:string; onDispatch:(identifier:string,name:string)=>void; onRerun:(runId:number,name:string)=>void}) {
  if (!options) return null;
  const dispatchable = options.workflows.filter(workflow => workflow.dispatchAllowed);
  const rerunnableIds = new Set(options.workflows.filter(workflow => workflow.rerunAllowed).map(workflow => workflow.workflowId));
  const failedRuns = workflows.filter(workflow => workflow.state === 'failure' && rerunnableIds.has(workflow.workflowId));
  if (dispatchable.length === 0 && failedRuns.length === 0 && options.currentWork) return null;
  return <section className="actions-controls" aria-labelledby="actions-controls-heading">
    <h3 id="actions-controls-heading">Kontrollerade Actions</h3>
    <p>Operationer gäller uttryckligen <code>{options.branchRef}</code> @ <code>{options.commitSha.slice(0,12)}</code>. Endast serverkonfigurerade workflows kan köras.</p>
    {!options.currentWork && <p className="status-message">{options.disabledReason || 'Work har gått vidare. Uppdatera resultatet innan du styr Actions.'}</p>}
    {message && <p className="status-message" role="status">{message}</p>}
    {error && <p className="status-message status-message--error" role="alert">{error}</p>}
    {options.currentWork && dispatchable.length > 0 && <div className="actions-control-group"><h4>Starta workflow manuellt</h4><ul>{dispatchable.map(workflow => <li key={`dispatch-${workflow.workflowId}`}><div><strong>{workflow.name}</strong><span>{workflow.path}</span></div><button className="button button--secondary" type="button" disabled={Boolean(busy)} onClick={() => onDispatch(workflow.identifier, workflow.name)}>{busy.startsWith('dispatch:') ? 'Startar…' : 'Kör workflow'}</button></li>)}</ul></div>}
    {options.currentWork && failedRuns.length > 0 && <div className="actions-control-group"><h4>Kör om misslyckade jobb</h4><ul>{failedRuns.map(workflow => <li key={`rerun-${workflow.id}`}><div><strong>{workflow.name}</strong><span>Run #{workflow.id}</span></div><button className="button button--secondary" type="button" disabled={Boolean(busy)} onClick={() => onRerun(workflow.id, workflow.name)}>{busy.startsWith('rerun:') ? 'Köar om…' : 'Kör om misslyckade jobb'}</button></li>)}</ul></div>}
  </section>;
}

function ActionsDetails({details}:{details:ImportActionsDetailsResponse}) {
  if (details.artifacts.length === 0 && details.failures.length === 0) return null;
  return <div className="actions-details">
    {details.artifacts.length > 0 && <section aria-labelledby="actions-artifacts-heading"><h3 id="actions-artifacts-heading">Artifacts</h3><p>Artifacts lagras inte i zip-github. Öppna den tillhörande körningen på GitHub för hämtning och fullständig information.</p><ul className="actions-artifact-list">{details.artifacts.map(artifact => <li key={artifact.id}><div><strong>{artifact.name}</strong><span>{formatBytes(artifact.sizeBytes)} · {artifact.workflowName}{artifact.expired ? ' · utgången' : ''}</span></div><a href={artifact.githubUrl} target="_blank" rel="noreferrer">Öppna på GitHub</a></li>)}</ul></section>}
    {details.failures.length > 0 && <section aria-labelledby="actions-failures-heading"><h3 id="actions-failures-heading">Kondenserade fel</h3><p>Endast ett begränsat, sanerat utdrag visas här. GitHub är källa för den fullständiga loggen.</p><ol className="actions-failure-list">{details.failures.map(failure => <li key={`${failure.workflowRunId}-${failure.jobId}`}><div className="actions-failure-source"><strong>{failure.workflowName} / {failure.jobName}</strong><span>{failure.stepName} · {failure.tool}</span></div><pre>{failure.lines.join('\n')}</pre><a href={failure.githubUrl} target="_blank" rel="noreferrer">Öppna jobb på GitHub</a></li>)}</ol></section>}
  </div>;
}

function formatBytes(bytes:number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} kB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function StateBadge({state}:{state:string}) { return <span className={`status-badge status-badge--${state}`}>{stateLabel(state)}</span>; }
function stateLabel(state:string) { return ({pending:'Pågår',queued:'Köad',in_progress:'Pågår',success:'Lyckad',failure:'Misslyckad',cancelled:'Avbruten',unavailable:'Ej tillgänglig',not_started:'Inte startad'} as Record<string,string>)[state] ?? state; }
function ResultLink({label,value,href}:{label:string;value:string;href:string}) { return <div><dt>{label}</dt><dd><a href={href} target="_blank" rel="noreferrer">{value}</a></dd></div>; }
function githubLinks(result: GitDeliveryResponse) { const repository=`https://github.com/${result.repositoryFullName}`; return { repository, branch:`${repository}/tree/${encodeURIComponent(result.branchName)}`, commit:`${repository}/commit/${result.commitSha}`, actions:`${repository}/actions?query=${encodeURIComponent(`branch:${result.branchName}`)}` }; }
