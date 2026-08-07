import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { approveImportPlan, createImportSelection, deliverImport, getImportPlan, ImportPlanApprovalResponse, ImportPlanEntry, ImportPlanResponse, prepareImportWorkspace } from '../api/imports';
import { defaultSelectedPaths, ReviewFileTree } from '../components/ReviewFileTree';

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
  const [selectionDigest, setSelectionDigest] = useState<string | null>(null);
  const [delivering, setDelivering] = useState(false);
  const [selectedPaths, setSelectedPaths] = useState<Set<string>>(new Set());
  const [overridePaths, setOverridePaths] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (!importId) {
      setError('Import-ID saknas.');
      setLoading(false);
      return;
    }
    let active = true;
    getImportPlan(importId)
      .then((loaded) => {
        if (active) {
          setPlan(loaded);
          setSelectedPaths(defaultSelectedPaths(loaded.entries));
        }
      })
      .catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : 'Importplanen kunde inte hämtas.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [importId]);

  const visibleEntries = useMemo(() => plan?.entries.filter((entry) => matchesFilter(entry, filter)) ?? [], [plan, filter]);

  async function approveExactPlan() {
    if (!importId || !plan || approving || selectedPaths.size === 0) return;
    setApproving(true);
    setError('');
    try {
      let digest = selectionDigest;
      if (!digest) {
        const selection = await createImportSelection(importId, plan.planDigestSha256, plan.baseCommitSha,
          [...selectedPaths].sort(), [...overridePaths].filter((path) => selectedPaths.has(path)).sort());
        digest = selection.selectionDigestSha256;
        setSelectionDigest(digest);
      }
      setApproval(await approveImportPlan(importId, plan.planDigestSha256, digest));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'De valda förändringarna kunde inte godkännas.');
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
        <li><span>3</span><strong>Godkänn och skapa commit</strong></li>
      </ol>

      {loading && <p className="status-message" role="status">Hämtar den sparade importplanen…</p>}
      {error && <p className="status-message status-message--error" role="alert">{error}</p>}
      {plan && <ReviewContent plan={plan} filter={filter} setFilter={setFilter} entries={visibleEntries}
        approving={approving} approval={approval} approveExactPlan={approveExactPlan}
        delivering={delivering} deliverApprovedPlan={deliverApprovedPlan}
        selectedPaths={selectedPaths} setSelectedPaths={setSelectedPaths}
        overridePaths={overridePaths} setOverridePaths={setOverridePaths} selectionLocked={Boolean(selectionDigest)} />}
    </section>
  );
}

function ReviewContent({ plan, filter, setFilter, entries, approving, approval, approveExactPlan, delivering, deliverApprovedPlan,
  selectedPaths, setSelectedPaths, overridePaths, setOverridePaths, selectionLocked }: {
  plan: ImportPlanResponse;
  filter: ReviewFilter;
  setFilter: (filter: ReviewFilter) => void;
  entries: ImportPlanEntry[];
  approving: boolean;
  approval: ImportPlanApprovalResponse | null;
  approveExactPlan: () => void;
  delivering: boolean;
  deliverApprovedPlan: () => void;
  selectedPaths: ReadonlySet<string>;
  setSelectedPaths: (paths: Set<string>) => void;
  overridePaths: ReadonlySet<string>;
  setOverridePaths: (paths: Set<string>) => void;
  selectionLocked: boolean;
}) {
  return (
    <>
      <div className={`review-decision ${selectedPaths.size > 0 ? 'review-decision--ready' : 'review-decision--blocked'}`} role="status">
        <div>
          <strong>{selectedPaths.size > 0 ? 'Urvalet kan godkännas' : 'Välj minst en förändring'}</strong>
          <p>{plan.blocked > 0
            ? `${plan.blocked} blockerande post${plan.blocked === 1 ? '' : 'er'} finns i planen. Överstyrbara poster kan tas med efter ett uttryckligt riskgodkännande; hårt blockerade poster kan aldrig levereras.`
            : 'Inga blockerande policyträffar hittades.'}</p>
        </div>
        <span className="status-badge">{plan.status}</span>
      </div>

      <dl className="review-summary" aria-label="Sammanfattning av importplanen">
        <SummaryItem label="Tillagda" value={plan.added} status="added" />
        <SummaryItem label="Ändrade" value={plan.modified} status="modified" />
        <SummaryItem label="Hårt blockerade" value={plan.hardBlocked} status="blocked" />
        <SummaryItem label="Överstyrbara" value={plan.overridableBlocked} status="blocked" />
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
        <div>
          <h2>Filer och kataloger</h2>
          <p className="review-selection-summary">{selectedPaths.size} förändring{selectedPaths.size === 1 ? '' : 'ar'} valda för commit.</p>
        </div>
        <span>{entries.length} filposter visas</span>
      </div>
      {entries.length === 0 ? <p className="empty-state">Inga filer matchar det valda filtret.</p> : (
        <ReviewFileTree entries={entries} selectedPaths={selectedPaths} onSelectedPathsChange={setSelectedPaths}
          overridePaths={overridePaths} onOverridePathsChange={setOverridePaths} locked={selectionLocked} />
      )}


      <div className="review-actions">
        {approval ? (
          <div className="approval-confirmation" role="status">
            <strong>Planen är godkänd</strong>
            <p>Godkännandet gäller plan <code>{approval.planDigestSha256}</code> och urval <code>{approval.selectionDigestSha256}</code>.</p>
            <button className="button" type="button" disabled={delivering} onClick={deliverApprovedPlan}>
              {delivering ? 'Skapar commit på arbetsbranchen…' : 'Skapa commit på arbetsbranchen'}
            </button>
          </div>
        ) : (
          <>
            <p>{selectionLocked
              ? 'Urvalet är låst. Försök godkänna igen om föregående approval-anrop avbröts.'
              : selectedPaths.size > 0
                ? 'Godkännandet låser exakt de valda paths och eventuella explicita overrides som visas ovan.'
                : 'Välj minst en förändring innan urvalet kan godkännas.'}</p>
            <button className="button" type="button" disabled={selectedPaths.size === 0 || approving}
              onClick={approveExactPlan}>
              {approving ? 'Godkänner…' : 'Godkänn valda förändringar'}
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

function matchesFilter(entry: ImportPlanEntry, filter: ReviewFilter): boolean {
  if (filter === 'ALL') return true;
  if (filter === 'CHANGES') return entry.status === 'ADDED' || entry.status === 'MODIFIED';
  if (filter === 'BLOCKED') return entry.status === 'BLOCKED';
  if (filter === 'WARNINGS') return entry.severity === 'WARNING';
  return entry.status === filter;
}
