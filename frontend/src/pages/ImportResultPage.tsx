import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { dispatchImportWorkflow, getDelivery, getImportActions, getImportActionDetails, getImportActionsControlOptions, rerunImportWorkflowFailedJobs, GitDeliveryResponse, ImportActionsControlOptionsResponse, ImportActionsDetailsResponse, ImportActionsStatusResponse } from '../api/imports';
import { getProjectWork, WorkSessionResponse } from '../api/projects';
import ActionsPanel from '../components/ActionsPanel';
import ActionsControls from '../components/ActionsControls';
import PullRequestComposer from '../components/PullRequestComposer';

const ACTION_POLL_DELAYS_MS = [0, 8000, 15000, 30000, 60000, 60000, 60000, 60000];

export default function ImportResultPage() {
  const { projectId, importId } = useParams();
  const [result, setResult] = useState<GitDeliveryResponse | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [actions, setActions] = useState<ImportActionsStatusResponse | null>(null);
  const [actionDetails, setActionDetails] = useState<ImportActionsDetailsResponse | null>(null);
  const [actionDetailsUnavailable, setActionDetailsUnavailable] = useState(false);
  const [showPullRequestComposer, setShowPullRequestComposer] = useState(false);
  const [pullRequestUrl, setPullRequestUrl] = useState('');
  const [work, setWork] = useState<WorkSessionResponse | null>(null);
  const [workStatusLoading, setWorkStatusLoading] = useState(false);
  const [workStatusError, setWorkStatusError] = useState('');
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
    if (!projectId || !result) return;
    let cancelled = false;
    setWorkStatusLoading(true);
    setWorkStatusError('');
    getProjectWork(projectId)
      .then((currentWork) => { if (!cancelled) setWork(currentWork); })
      .catch(() => { if (!cancelled) { setWork(null); setWorkStatusError('Aktuell Work-/PR-status kunde inte hämtas. Öppna projektet och försök igen.'); } })
      .finally(() => { if (!cancelled) setWorkStatusLoading(false); });
    return () => { cancelled = true; };
  }, [projectId, result]);

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

  async function refreshActions() {
    if (!importId) return;
    try {
      const status = await getImportActions(importId);
      setActions(status);
      if (['success', 'failure', 'cancelled'].includes(status.state)) {
        setActionDetailsUnavailable(false);
        const nextDetails = await getImportActionDetails(importId).catch(() => null);
        if (nextDetails) setActionDetails(nextDetails); else setActionDetailsUnavailable(true);
      } else {
        setActionDetails(null);
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Actions-status kunde inte hämtas.');
    }
  }

  const links = useMemo(() => result ? githubLinks(result) : null, [result]);
  return <section className="page-card result-page" aria-labelledby="result-heading">
    <p><Link className="back-link" to={projectId ? `/projects/${projectId}` : '/projects'}>← Till projektet</Link></p>
    <p className="eyebrow">Importresultat</p>
    <h1 id="result-heading">Commit skapad</h1>
    <p className="lead">ZIP-importen är committad på projektets arbetsbranch. {work?.status === 'PR_OPEN' ? 'Den befintliga pull requesten uppdateras automatiskt med committen.' : 'Du kan fortsätta med nästa ZIP eller skapa en pull request med en egen titel och beskrivning.'}</p>
    {loading && <p role="status">Hämtar resultat…</p>}
    {error && <p role="alert" className="status-message status-message--error">{error}</p>}
    {result && links && <>
      <div className="result-success" role="status"><div><strong>Importen är committad</strong><p>Arbetsbranchen är uppdaterad med den godkända ZIP-filen.</p></div><span className="status-badge">PUSHED</span></div>
      {workStatusLoading && <p className="status-message" role="status">Kontrollerar aktuell Work- och pull request-status…</p>}
      {workStatusError && <p className="status-message status-message--error" role="alert">{workStatusError}</p>}
      {work?.status === 'PR_OPEN' && work.pullRequestUrl && <p className="status-message" role="status">Den befintliga pull requesten har uppdaterats med denna commit. <a href={work.pullRequestUrl} target="_blank" rel="noreferrer">Öppna pull request</a></p>}
      {pullRequestUrl && <p className="status-message" role="status">Arbetets pull request är skapad. <a href={pullRequestUrl} target="_blank" rel="noreferrer">Öppna pull request</a></p>}
      {projectId && showPullRequestComposer && !pullRequestUrl && work?.status !== 'PR_OPEN' && <PullRequestComposer projectId={projectId}
        onCreated={created => { setPullRequestUrl(created.pullRequestUrl); setShowPullRequestComposer(false); }}
        onCancel={() => setShowPullRequestComposer(false)} />}
      <div className="result-primary-action">
        {projectId && <Link className="button" to={`/projects/${projectId}/imports/new`}>Ladda upp nästa ZIP</Link>}
        {projectId && !workStatusLoading && !workStatusError && !pullRequestUrl && !showPullRequestComposer && work && work.status !== 'PR_OPEN' && <button className="button button--secondary" type="button" onClick={()=>setShowPullRequestComposer(true)}>{work.status === 'PR_CLOSED' ? 'Skapa ny pull request' : 'Skapa pull request'}</button>}
        {projectId && pullRequestUrl && <button className="button button--secondary" type="button" disabled>Pull request skapad</button>}
      </div>
      <dl className="result-link-grid">
        <ResultLink label="Repository" value={result.repositoryFullName} href={links.repository} />
        <ResultLink label="Arbetsbranch" value={result.branchName} href={links.branch} />
        <ResultLink label="Commit" value={result.commitSha.slice(0,12)} href={links.commit} />
        <ResultLink label="GitHub Actions" value="Öppna Actions" href={links.actions} />
      </dl>
      <ActionsPanel actions={actions} details={actionDetails} detailsUnavailable={actionDetailsUnavailable} fallbackUrl={links.actions}
        repositoryFullName={result.repositoryFullName} branchName={result.branchName} commitSha={result.commitSha} onRefresh={() => void refreshActions()}
        controls={<ActionsControls options={controlOptions} workflows={actions?.workflows ?? []} busy={controlBusy} message={controlMessage} error={controlError} onDispatch={dispatchWorkflow} onRerun={rerunWorkflow} />} />
    </>}
  </section>;
}

function ResultLink({label,value,href}:{label:string;value:string;href:string}) { return <div><dt>{label}</dt><dd><a href={href} target="_blank" rel="noreferrer">{value}</a></dd></div>; }
function githubLinks(result: GitDeliveryResponse) { const repository=`https://github.com/${result.repositoryFullName}`; return { repository, branch:`${repository}/tree/${encodeURIComponent(result.branchName)}`, commit:`${repository}/commit/${result.commitSha}`, actions:`${repository}/actions?query=${encodeURIComponent(`branch:${result.branchName}`)}` }; }
