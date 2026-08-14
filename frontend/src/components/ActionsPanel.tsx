import { ReactNode } from 'react';
import { ActionsWorkflowRunResponse, ImportActionsDetailsResponse, ImportActionsStatusResponse } from '../api/imports';

export type ActionsPanelProps = {
  actions: ImportActionsStatusResponse | null;
  details: ImportActionsDetailsResponse | null;
  detailsUnavailable?: boolean;
  fallbackUrl: string;
  repositoryFullName: string;
  branchName: string;
  commitSha: string;
  refreshing?: boolean;
  onRefresh?: () => void;
  controls?: ReactNode;
  headingLevel?: 'h2' | 'h3';
};

export default function ActionsPanel({
  actions,
  details,
  detailsUnavailable = false,
  fallbackUrl,
  repositoryFullName,
  branchName,
  commitSha,
  refreshing = false,
  onRefresh,
  controls,
  headingLevel = 'h2',
}: ActionsPanelProps) {
  const Heading = headingLevel;

  async function copyFailure(failure: ImportActionsDetailsResponse['failures'][number]) {
    const contextLines = failure.contextLines ?? [];
    const lines = contextLines.length > 0 ? contextLines : failure.lines;
    const text = [
      `Repository: ${repositoryFullName}`,
      `Branch: ${branchName}`,
      `Commit: ${commitSha}`,
      `Workflow: ${failure.workflowName}`,
      `Job: ${failure.jobName}`,
      `Step: ${failure.stepName}`,
      failure.tool ? `Tool: ${failure.tool}` : '',
      '',
      ...lines,
      '',
      failure.githubUrl ? `GitHub: ${failure.githubUrl}` : '',
    ].filter(Boolean).join('\n').slice(0, 30000);
    await navigator.clipboard.writeText(text);
  }

  async function copyJobLog(failure: ImportActionsDetailsResponse['failures'][number]) {
    const text = [
      `Repository: ${repositoryFullName}`,
      `Branch: ${branchName}`,
      `Commit: ${commitSha}`,
      `Workflow: ${failure.workflowName}`,
      `Job: ${failure.jobName}`,
      `Step: ${failure.stepName}`,
      failure.logTruncated ? 'Logg: trunkerad av zip-github efter säker storleksgräns' : 'Logg: hämtad jobblogg',
      '',
      ...(failure.jobLogLines ?? []),
      '',
      failure.githubUrl ? `GitHub: ${failure.githubUrl}` : '',
    ].filter(Boolean).join('\n').slice(0, 140000);
    await navigator.clipboard.writeText(text);
  }

  if (!actions) return <section className="actions-overview" aria-labelledby="actions-heading">
    <div className="review-list-heading"><div><Heading id="actions-heading">GitHub Actions</Heading><p>Status för aktuell commit <code>{commitSha.slice(0, 12)}</code>.</p></div>{onRefresh && <button className="button button--secondary" type="button" disabled={refreshing} onClick={onRefresh}>{refreshing ? 'Uppdaterar…' : 'Uppdatera status'}</button>}</div>
    <p role="status">Hämtar workflow-status…</p>
    {controls}
  </section>;

  if (actions.state === 'unavailable') return <section className="actions-overview" aria-labelledby="actions-heading">
    <div className="review-list-heading"><div><Heading id="actions-heading">GitHub Actions</Heading><p>Status för aktuell commit <code>{commitSha.slice(0, 12)}</code>.</p></div>{onRefresh && <button className="button button--secondary" type="button" disabled={refreshing} onClick={onRefresh}>{refreshing ? 'Uppdaterar…' : 'Uppdatera status'}</button>}</div>
    <p className="status-message status-message--error">{actions.diagnosticMessage || 'Actions-status kunde inte läsas just nu.'} <a href={fallbackUrl} target="_blank" rel="noreferrer">Öppna Actions på GitHub</a>.</p>
    {actions.diagnosticCode === 'ACTIONS_PERMISSION_REQUIRED' && <p className="status-message">Kontrollera GitHub App-installationens Repository permissions → Actions. Om App-behörigheten ändrats efter installationen måste repositoryägaren även godkänna ändringen på GitHub.</p>}
    {controls}
  </section>;

  if (actions.state === 'not_started') return <section className="actions-overview" aria-labelledby="actions-heading">
    <div className="review-list-heading"><div><Heading id="actions-heading">GitHub Actions</Heading><p>Status för aktuell commit <code>{commitSha.slice(0, 12)}</code>.</p></div>{onRefresh && <button className="button button--secondary" type="button" disabled={refreshing} onClick={onRefresh}>{refreshing ? 'Uppdaterar…' : 'Uppdatera status'}</button>}</div>
    <p className="status-message">Ingen workflow-körning eller check har registrerats för committen ännu. <a href={fallbackUrl} target="_blank" rel="noreferrer">Öppna Actions på GitHub</a>.</p>
    {controls}
  </section>;

  const targetCommitSha = actions.commitSha || commitSha;
  const representedWorkflowJobNames = new Set(
    (actions.workflows ?? []).flatMap(workflow => {
      if (workflow.headSha && workflow.headSha !== targetCommitSha) return [];
      return (workflow.jobs ?? []).map(job => normalizeCheckName(job.name));
    }),
  );
  const additionalChecks = (actions.checks ?? []).filter(check =>
    !isDuplicateGitHubActionsCheck(check.appName, check.name, representedWorkflowJobNames),
  );
  const workflowGroups = groupWorkflowRuns(actions.workflows ?? [], targetCommitSha);
  const observedItemCount = workflowGroups.length + additionalChecks.length;
  const isSuccess = actions.state === 'success';
  const isPending = ['pending', 'queued', 'in_progress'].includes(actions.state);
  const isAttention = actions.state === 'failure' || actions.state === 'cancelled';

  const detailedContent = <>
    {workflowGroups.length > 0 && <ol className="actions-list">{workflowGroups.map(group => {
      const primary = group.runs[0];
      const workflowCommitSha = primary.headSha || actions.commitSha || commitSha;
      const jobs = primary.jobs ?? [];
      return <li key={group.key} className="actions-run-card">
      <div className="actions-item-heading"><div><strong>{primary.name}</strong><p>{group.runs.length === 1 ? `${primary.event || 'workflow'} · ` : `${group.runs.length} GitHub-körningar · `}<code>{workflowCommitSha.slice(0, 12)}</code>{group.runs.length === 1 && <> · <a href={primary.htmlUrl || fallbackUrl} target="_blank" rel="noreferrer">Öppna körning på GitHub</a></>}</p></div><StateBadge state={group.state} /></div>
      {group.runs.length === 1 && jobs.length > 0 && <ul className="actions-job-list">{jobs.map(job => <li key={job.id}><span>{job.htmlUrl ? <a href={job.htmlUrl} target="_blank" rel="noreferrer">{job.name}</a> : job.name}</span><StateBadge state={job.state} /></li>)}</ul>}
      {group.runs.length > 1 && <details className="actions-run-group"><summary>Visa {group.runs.length} separata körningar</summary><ol className="actions-list">{group.runs.map(run => <li key={run.id} className="actions-run-card"><div className="actions-item-heading"><div><strong>{run.event || 'workflow'}</strong><p><a href={run.htmlUrl || fallbackUrl} target="_blank" rel="noreferrer">Öppna körning på GitHub</a></p></div><StateBadge state={run.state} /></div>{(run.jobs ?? []).length > 0 && <ul className="actions-job-list">{(run.jobs ?? []).map(job => <li key={job.id}><span>{job.htmlUrl ? <a href={job.htmlUrl} target="_blank" rel="noreferrer">{job.name}</a> : job.name}</span><StateBadge state={job.state} /></li>)}</ul>}</li>)}</ol></details>}
    </li>;
    })}</ol>}
    {additionalChecks.length > 0 && <div className="actions-checks"><h3>Övriga kontroller</h3><ul className="actions-job-list">{additionalChecks.map(check => <li key={check.id}><span>{check.htmlUrl ? <a href={check.htmlUrl} target="_blank" rel="noreferrer">{check.name}</a> : check.name}{check.appName ? ` · ${check.appName}` : ''}</span><StateBadge state={check.state} /></li>)}</ul></div>}
    {detailsUnavailable && <p className="status-message">Artifacts och feldiagnostik kunde inte läsas just nu. <a href={fallbackUrl} target="_blank" rel="noreferrer">Öppna Actions på GitHub</a>.</p>}
    {details && <ActionsDetails details={details} copyFailure={copyFailure} copyJobLog={copyJobLog} />}
    {controls}
  </>;

  return <section className={`actions-overview${isAttention ? ' actions-overview--attention' : ''}`} aria-labelledby="actions-heading">
    <div className="review-list-heading"><div><Heading id="actions-heading">GitHub Actions</Heading><p>Commit <code>{commitSha.slice(0, 12)}</code>. GitHub är källa för fullständig körningsinformation.</p></div><div className="actions-heading-actions"><StateBadge state={actions.state} />{onRefresh && <button className="button button--secondary" type="button" disabled={refreshing} onClick={onRefresh}>{refreshing ? 'Uppdaterar…' : 'Uppdatera status'}</button>}</div></div>
    {isSuccess ? <>
      <p className="status-message status-message--success">Alla observerade Actions-kontroller för committen är godkända.</p>
      <details className="actions-success-details">
        <summary>Visa Actions-detaljer{observedItemCount > 0 ? ` (${observedItemCount})` : ''}</summary>
        {detailedContent}
      </details>
    </> : isPending ? <>
      <p className="status-message">GitHub Actions pågår för committen.</p>
      <details className="actions-pending-details">
        <summary>Visa pågående Actions-detaljer{observedItemCount > 0 ? ` (${observedItemCount})` : ''}</summary>
        {detailedContent}
      </details>
    </> : <>
      {isAttention && <p className="status-message status-message--error">{actions.state === 'cancelled' ? 'En observerad Actions-körning har avbrutits.' : 'En eller flera observerade Actions-kontroller har misslyckats.'}</p>}
      {detailedContent}
    </>}
  </section>;
}

function ActionsDetails({details, copyFailure, copyJobLog}:{details:ImportActionsDetailsResponse; copyFailure:(failure:ImportActionsDetailsResponse['failures'][number])=>Promise<void>; copyJobLog:(failure:ImportActionsDetailsResponse['failures'][number])=>Promise<void>}) {
  if (details.artifacts.length === 0 && details.failures.length === 0) return null;
  return <div className="actions-details">
    {details.artifacts.length > 0 && <section aria-labelledby="actions-artifacts-heading"><h3 id="actions-artifacts-heading">Artifacts</h3><p>Artifacts lagras inte i zip-github. Öppna den tillhörande körningen på GitHub för hämtning och fullständig information.</p><ul className="actions-artifact-list">{details.artifacts.map(artifact => <li key={artifact.id}><div><strong>{artifact.name}</strong><span>{formatBytes(artifact.sizeBytes)} · {artifact.workflowName}{artifact.expired ? ' · utgången' : ''}</span></div><a href={artifact.githubUrl} target="_blank" rel="noreferrer">Öppna på GitHub</a></li>)}</ul></section>}
    {details.failures.length > 0 && <section aria-labelledby="actions-failures-heading"><h3 id="actions-failures-heading">Fel och jobbloggar</h3><p>Loggar saneras från kända credentials/tokens och begränsas i storlek. GitHub är alltid källa för den fullständiga originaloggen.</p><ol className="actions-failure-list">{details.failures.map(failure => <li key={`${failure.workflowRunId}-${failure.jobId}`}>
      <div className="actions-failure-source"><strong>{failure.workflowName} / {failure.jobName}</strong><span>{failure.stepName}{failure.tool && failure.tool !== 'Unknown' ? ` · ${failure.tool}` : ''}</span></div>
      {failure.lines.length > 0 && <><h4>Kondenserat fel</h4><pre>{failure.lines.join('\n')}</pre></>}
      {(failure.contextLines ?? []).length > 0 && <details open><summary>Visa sammanhang kring felet</summary><pre>{(failure.contextLines ?? []).join('\n')}</pre></details>}
      {(failure.jobLogLines ?? []).length > 0 && <details><summary>Visa sanerad jobblogg{failure.logTruncated ? ' (trunkerad)' : ''}</summary><pre>{(failure.jobLogLines ?? []).join('\n')}</pre></details>}
      <div className="result-primary-action"><button className="button button--secondary" type="button" onClick={() => void copyFailure(failure)}>Kopiera fel med sammanhang</button>{(failure.jobLogLines ?? []).length > 0 && <button className="button button--secondary" type="button" onClick={() => void copyJobLog(failure)}>Kopiera jobblogg</button>}<a className="button button--secondary" href={failure.githubUrl} target="_blank" rel="noreferrer">Öppna jobb på GitHub</a></div>
    </li>)}</ol></section>}
  </div>;
}

function workflowIdentity(run: ActionsWorkflowRunResponse) {
  const identity = run.workflowId > 0 ? `id:${run.workflowId}` : `path:${run.workflowPath || run.name}`;
  return `${identity}|${run.headSha || ''}`;
}

function groupWorkflowRuns(runs: ActionsWorkflowRunResponse[], targetCommitSha: string) {
  const groups = new Map<string, ActionsWorkflowRunResponse[]>();
  for (const run of runs) {
    const runCommitSha = run.headSha || targetCommitSha;
    const normalizedRun = run.headSha ? run : { ...run, headSha: runCommitSha };
    const key = workflowIdentity(normalizedRun);
    const existing = groups.get(key) ?? [];
    existing.push(normalizedRun);
    groups.set(key, existing);
  }
  return [...groups.entries()].map(([key, groupedRuns]) => ({
    key,
    runs: groupedRuns.slice().sort((a, b) => compareRunRecency(b, a)),
    state: aggregateWorkflowState(groupedRuns),
  }));
}

function compareRunRecency(a: ActionsWorkflowRunResponse, b: ActionsWorkflowRunResponse) {
  const aTime = Date.parse(a.updatedAt || a.createdAt || '') || 0;
  const bTime = Date.parse(b.updatedAt || b.createdAt || '') || 0;
  if (aTime !== bTime) return aTime - bTime;
  return a.id - b.id;
}

function aggregateWorkflowState(runs: ActionsWorkflowRunResponse[]): ActionsWorkflowRunResponse['state'] {
  const states = new Set(runs.map(run => run.state));
  if (states.has('failure')) return 'failure';
  if (states.has('in_progress')) return 'in_progress';
  if (states.has('queued')) return 'queued';
  if (states.has('pending')) return 'pending';
  if (states.has('cancelled')) return 'cancelled';
  return 'success';
}

function formatBytes(bytes:number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} kB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function normalizeCheckName(name:string) {
  return name.trim().replace(/\s+/g, ' ').toLocaleLowerCase('en-US');
}

function isDuplicateGitHubActionsCheck(appName:string | null | undefined, checkName:string, representedWorkflowJobNames:Set<string>) {
  return normalizeCheckName(appName ?? '') === 'github actions'
    && representedWorkflowJobNames.has(normalizeCheckName(checkName));
}

export function StateBadge({state}:{state:string}) { return <span className={`status-badge status-badge--${state}`}>{stateLabel(state)}</span>; }
function stateLabel(state:string) { return ({pending:'Pågår',queued:'Köad',in_progress:'Pågår',success:'Lyckad',failure:'Misslyckad',cancelled:'Avbruten',unavailable:'Ej tillgänglig',not_started:'Inte startad'} as Record<string,string>)[state] ?? state; }
