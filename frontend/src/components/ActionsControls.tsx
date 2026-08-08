import { ImportActionsControlOptionsResponse, ImportActionsStatusResponse } from '../api/imports';

export default function ActionsControls({options, workflows, busy, message, error, onDispatch, onRerun}:{options:ImportActionsControlOptionsResponse|null; workflows:ImportActionsStatusResponse['workflows']; busy:string; message:string; error:string; onDispatch:(identifier:string,name:string)=>void; onRerun:(runId:number,name:string)=>void}) {
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
