import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { approveImportPlan, createPullRequest, deliverImport, getImportPlan, ImportPlanApprovalResponse, ImportPlanEntry, ImportPlanResponse, prepareImportWorkspace } from '../api/imports';

type ReviewFilter = 'CHANGES' | 'BLOCKED' | 'WARNINGS' | 'UNCHANGED' | 'IGNORED' | 'ALL';

const filters: Array<{ id: ReviewFilter; label: string }> = [
  { id: 'CHANGES', label: 'Förändringar' },
  { id: 'BLOCKED', label: 'Blockerade' },
  { id: 'WARNINGS', label: 'Varningar' },
  { id: 'UNCHANGED', label: 'Oförändrade' },
  { id: 'IGNORED', label: 'Ignorerade' },
  { id: 'ALL', label: 'Alla filer' },
];

export default function ImportReviewPage() {
  const { projectId, importId } = useParams();
  const navigate = useNavigate();
  const [plan, setPlan] = useState<ImportPlanResponse | null>(null);
  const [filter, setFilter] = useState<ReviewFilter>('CHANGES');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [approving, setApproving] = useState(false);
  const [approval, setApproval] = useState<ImportPlanApprovalResponse | null>(null);
  const [delivering, setDelivering] = useState(false);

  useEffect(() => {
    if (!importId) {
      setError('Import-ID saknas.');
      setLoading(false);
      return;
    }
    let active = true;
    getImportPlan(importId)
      .then((loaded) => { if (active) setPlan(loaded); })
      .catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : 'Importplanen kunde inte hämtas.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [importId]);

  const visibleEntries = useMemo(() => plan?.entries.filter((entry) => matchesFilter(entry, filter)) ?? [], [plan, filter]);

  async function approveExactPlan() {
    if (!importId || !plan || !plan.approvable || approving) return;
    setApproving(true);
    setError('');
    try {
      setApproval(await approveImportPlan(importId, plan.planDigestSha256));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Importplanen kunde inte godkännas.');
    } finally {
      setApproving(false);
    }
  }

  async function deliverApprovedPlan() {
    if (!importId || !projectId || !approval || delivering) return;
    setDelivering(true);
    setError('');
    try {
      await prepareImportWorkspace(importId);
      await deliverImport(importId);
      await createPullRequest(importId);
      navigate(`/projects/${projectId}/imports/${importId}/result`);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Leveransen till GitHub kunde inte slutföras.');
      setDelivering(false);
    }
  }

  return (
    <section className="page-card review-page" aria-labelledby="review-heading">
      <p><Link className="back-link" to={projectId ? `/projects/${projectId}` : '/projects'}>← Till projektet</Link></p>
      <p className="eyebrow">Importgranskning</p>
      <h1 id="review-heading">Granska förändringar</h1>
      <p className="lead">Kontrollera exakt vad ZIP-filen skulle ändra jämfört med den låsta GitHub-versionen.</p>

      <ol className="step-list" aria-label="Importflöde">
        <li><span>1</span><strong>Välj ZIP</strong></li>
        <li className="step-list__current"><span>2</span><strong>Granska förändringar</strong></li>
        <li><span>3</span><strong>Godkänn och skapa PR</strong></li>
      </ol>

      {loading && <p className="status-message" role="status">Hämtar den sparade importplanen…</p>}
      {error && <p className="status-message status-message--error" role="alert">{error}</p>}
      {plan && <ReviewContent plan={plan} filter={filter} setFilter={setFilter} entries={visibleEntries}
        approving={approving} approval={approval} approveExactPlan={approveExactPlan}
        delivering={delivering} deliverApprovedPlan={deliverApprovedPlan} />}
    </section>
  );
}

function ReviewContent({ plan, filter, setFilter, entries, approving, approval, approveExactPlan, delivering, deliverApprovedPlan }: {
  plan: ImportPlanResponse;
  filter: ReviewFilter;
  setFilter: (filter: ReviewFilter) => void;
  entries: ImportPlanEntry[];
  approving: boolean;
  approval: ImportPlanApprovalResponse | null;
  approveExactPlan: () => void;
  delivering: boolean;
  deliverApprovedPlan: () => void;
}) {
  return (
    <>
      <div className={`review-decision ${plan.approvable ? 'review-decision--ready' : 'review-decision--blocked'}`} role="status">
        <div>
          <strong>{plan.approvable ? 'Planen kan godkännas' : 'Planen är blockerad'}</strong>
          <p>{plan.approvable ? 'Inga blockerande policyträffar hittades.' : `${plan.blocked} blockerande post${plan.blocked === 1 ? '' : 'er'} måste åtgärdas i en ny ZIP.`}</p>
        </div>
        <span className="status-badge">{plan.status}</span>
      </div>

      <dl className="review-summary" aria-label="Sammanfattning av importplanen">
        <SummaryItem label="Tillagda" value={plan.added} status="added" />
        <SummaryItem label="Ändrade" value={plan.modified} status="modified" />
        <SummaryItem label="Blockerade" value={plan.blocked} status="blocked" />
        <SummaryItem label="Varningar" value={plan.warnings} status="warning" />
        <SummaryItem label="Oförändrade" value={plan.unchanged} status="unchanged" />
        <SummaryItem label="Ignorerade" value={plan.ignored} status="ignored" />
      </dl>

      <details className="plan-identity">
        <summary>Planidentitet och låst GitHub-version</summary>
        <dl>
          <div><dt>Base commit</dt><dd><code>{plan.baseCommitSha}</code></dd></div>
          <div><dt>Plan-digest</dt><dd><code>{plan.planDigestSha256}</code></dd></div>
          <div><dt>ZIP SHA-256</dt><dd><code>{plan.sourceUploadSha256}</code></dd></div>
          <div><dt>Policy</dt><dd>{plan.policyVersion}</dd></div>
        </dl>
      </details>

      <div className="review-toolbar" aria-label="Filfilter">
        {filters.map((candidate) => (
          <button
            key={candidate.id}
            type="button"
            className={filter === candidate.id ? 'filter-button filter-button--active' : 'filter-button'}
            aria-pressed={filter === candidate.id}
            onClick={() => setFilter(candidate.id)}
          >
            {candidate.label}
          </button>
        ))}
      </div>

      <div className="review-list-heading">
        <h2>Filer</h2>
        <span>{entries.length} visas</span>
      </div>
      {entries.length === 0 ? <p className="empty-state">Inga filer matchar det valda filtret.</p> : (
        <ul className="review-file-list" aria-label="Filposter">
          {entries.map((entry) => <ReviewFile key={`${entry.path}-${entry.status}`} entry={entry} />)}
        </ul>
      )}

      <div className="review-actions">
        {approval ? (
          <div className="approval-confirmation" role="status">
            <strong>Planen är godkänd</strong>
            <p>Godkännandet gäller exakt digest <code>{approval.planDigestSha256}</code>.</p>
            <button className="button" type="button" disabled={delivering} onClick={deliverApprovedPlan}>
              {delivering ? 'Skapar branch och PR…' : 'Skapa branch och pull request'}
            </button>
          </div>
        ) : (
          <>
            <p>{plan.approvable
              ? 'Godkännandet låser exakt den plan-digest som visas ovan.'
              : 'Skapa en ny import efter att blockerade filer har tagits bort eller ändrats.'}</p>
            <button className="button" type="button" disabled={!plan.approvable || approving}
              onClick={approveExactPlan}>
              {approving ? 'Godkänner…' : 'Godkänn exakt plan'}
            </button>
          </>
        )}
      </div>
    </>
  );
}

function SummaryItem({ label, value, status }: { label: string; value: number; status: string }) {
  return <div className={`summary-card summary-card--${status}`}><dt>{label}</dt><dd>{value}</dd></div>;
}

function ReviewFile({ entry }: { entry: ImportPlanEntry }) {
  return (
    <li className={`review-file review-file--${entry.status.toLowerCase()}`}>
      <div className="review-file__main">
        <code className="review-file__path">{entry.path}</code>
        <div className="review-file__badges">
          <span className={`file-status file-status--${entry.status.toLowerCase()}`}>{statusLabel(entry.status)}</span>
          {entry.severity === 'WARNING' && <span className="file-status file-status--warning">Varning</span>}
          <span className="file-kind">{entry.textCandidate ? 'Text' : 'Binär'}</span>
        </div>
      </div>
      {entry.message && <p className="review-file__message">{entry.message}</p>}
      <dl className="review-file__meta">
        <div><dt>ZIP</dt><dd>{formatBytes(entry.archiveSizeBytes)}</dd></div>
        <div><dt>Repository</dt><dd>{formatBytes(entry.repositorySizeBytes)}</dd></div>
        {entry.policyCode && <div><dt>Policykod</dt><dd><code>{entry.policyCode}</code></dd></div>}
      </dl>
    </li>
  );
}

function matchesFilter(entry: ImportPlanEntry, filter: ReviewFilter): boolean {
  if (filter === 'ALL') return true;
  if (filter === 'CHANGES') return entry.status === 'ADDED' || entry.status === 'MODIFIED';
  if (filter === 'BLOCKED') return entry.status === 'BLOCKED';
  if (filter === 'WARNINGS') return entry.severity === 'WARNING';
  return entry.status === filter;
}

function statusLabel(status: ImportPlanEntry['status']): string {
  return ({ ADDED: 'Tillagd', MODIFIED: 'Ändrad', UNCHANGED: 'Oförändrad', IGNORED: 'Ignorerad', BLOCKED: 'Blockerad' })[status];
}

function formatBytes(bytes: number | null): string {
  if (bytes === null) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KiB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MiB`;
}
