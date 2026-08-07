import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { approveImportPlan, cancelImport, createImportSelection, deliverImport, findDelivery, getImportPlan, getImportPlanApproval, getImportSelection, ImportPlanApprovalResponse, ImportPlanEntry, ImportPlanResponse, prepareImportWorkspace } from '../api/imports';
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
  const [cancelConfirm, setCancelConfirm] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (!importId) {
      setError('Import-ID saknas.');
      setLoading(false);
      return;
    }
    let active = true;
    async function loadReviewState() {
      try {
        const loadedPlan = await getImportPlan(importId!);
        if (!active) return;
        setPlan(loadedPlan);

        const [existingSelection, existingApproval, existingDelivery] = await Promise.all([
          getImportSelection(importId!),
          getImportPlanApproval(importId!),
          findDelivery(importId!),
        ]);
        if (!active) return;
        if (existingDelivery && projectId) {
          navigate(`/projects/${projectId}/imports/${importId}/result`, { replace: true });
          return;
        }
        if (existingSelection) {
          setSelectedPaths(new Set(existingSelection.selectedPaths));
          setOverridePaths(new Set(existingSelection.overrides.map((item) => item.path)));
          setSelectionDigest(existingSelection.selectionDigestSha256);
        } else {
          setSelectedPaths(defaultSelectedPaths(loadedPlan.entries));
        }
        if (existingApproval) setApproval(existingApproval);
      } catch (reason) {
        if (active) setError(reason instanceof Error ? reason.message : 'Importplanen kunde inte hämtas.');
      } finally {
        if (active) setLoading(false);
      }
    }
    void loadReviewState();
    return () => { active = false; };
  }, [importId, navigate, projectId]);

  const visibleEntries = useMemo(() => plan?.entries.filter((entry) => matchesFilter(entry, filter)) ?? [], [plan, filter]);

  async function approveAndDeliver() {
    if (!importId || !projectId || !plan || approving || delivering || selectedPaths.size === 0) return;
    setApproving(true);
    setDelivering(false);
    setError('');
    try {
      let digest = selectionDigest;
      if (!digest) {
        const selection = await createImportSelection(importId, plan.planDigestSha256, plan.baseCommitSha,
          [...selectedPaths].sort(), [...overridePaths].filter((path) => selectedPaths.has(path)).sort());
        digest = selection.selectionDigestSha256;
        setSelectionDigest(digest);
      }
      let recordedApproval = approval;
      if (!recordedApproval) {
        recordedApproval = await approveImportPlan(importId, plan.planDigestSha256, digest);
        setApproval(recordedApproval);
      }
      setApproving(false);
      setDelivering(true);
      await prepareImportWorkspace(importId);
      await deliverImport(importId);
      navigate(`/projects/${projectId}/imports/${importId}/result`);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Godkännandet eller leveransen till GitHub kunde inte slutföras.');
    } finally {
      setApproving(false);
      setDelivering(false);
    }
  }

  async function retryApprovedDelivery() {
    if (!importId || !projectId || !approval || delivering) return;
    setDelivering(true);
    setError('');
    try {
      const existingDelivery = await findDelivery(importId);
      if (!existingDelivery) {
        await prepareImportWorkspace(importId);
        await deliverImport(importId);
      }
      navigate(`/projects/${projectId}/imports/${importId}/result`);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Leveransen till GitHub kunde inte slutföras.');
    } finally {
      setDelivering(false);
    }
  }

  async function confirmCancelImport() {
    if (!importId || !projectId || cancelling || delivering || approving) return;
    setCancelling(true);
    setError('');
    try {
      await cancelImport(importId);
      navigate(`/projects/${projectId}`, { replace: true });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Importen kunde inte avbrytas.');
      setCancelConfirm(false);
    } finally {
      setCancelling(false);
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
        approving={approving} approval={approval} approveAndDeliver={approveAndDeliver}
        delivering={delivering} retryApprovedDelivery={retryApprovedDelivery}
        selectedPaths={selectedPaths} setSelectedPaths={setSelectedPaths}
        overridePaths={overridePaths} setOverridePaths={setOverridePaths} selectionLocked={Boolean(selectionDigest)}
        cancelConfirm={cancelConfirm} setCancelConfirm={setCancelConfirm} cancelling={cancelling}
        confirmCancelImport={confirmCancelImport} />}
    </section>
  );
}

function ReviewContent({ plan, filter, setFilter, entries, approving, approval, approveAndDeliver, delivering, retryApprovedDelivery,
  selectedPaths, setSelectedPaths, overridePaths, setOverridePaths, selectionLocked, cancelConfirm, setCancelConfirm, cancelling, confirmCancelImport }: {
  plan: ImportPlanResponse;
  filter: ReviewFilter;
  setFilter: (filter: ReviewFilter) => void;
  entries: ImportPlanEntry[];
  approving: boolean;
  approval: ImportPlanApprovalResponse | null;
  approveAndDeliver: () => void;
  delivering: boolean;
  retryApprovedDelivery: () => void;
  selectedPaths: ReadonlySet<string>;
  setSelectedPaths: (paths: Set<string>) => void;
  overridePaths: ReadonlySet<string>;
  setOverridePaths: (paths: Set<string>) => void;
  selectionLocked: boolean;
  cancelConfirm: boolean;
  setCancelConfirm: (value: boolean) => void;
  cancelling: boolean;
  confirmCancelImport: () => void;
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
            <strong>Förändringarna är godkända</strong>
            <p>Urvalet är låst och godkänt. Föregående commit/push slutfördes inte, så du kan säkert försöka leveransen igen.</p>
            <button className="button" type="button" disabled={delivering} onClick={retryApprovedDelivery}>
              {delivering ? 'Försöker skapa commit igen…' : 'Försök skapa commit igen'}
            </button>
          </div>
        ) : (
          <>
            <p>{selectionLocked
              ? 'Urvalet är låst. Samma urval används när godkännandet och committen försöks igen.'
              : selectedPaths.size > 0
                ? 'Ett klick låser urvalet, registrerar godkännandet och skapar sedan commit på arbetsbranchen.'
                : 'Välj minst en förändring innan urvalet kan godkännas.'}</p>
            <button className="button" type="button" disabled={selectedPaths.size === 0 || approving || delivering}
              onClick={approveAndDeliver}>
              {approving ? 'Godkänner…' : delivering ? 'Skapar commit på arbetsbranchen…' : 'Godkänn valda förändringar'}
            </button>
          </>
        )}
        <div className="review-cancel-action">
          {!cancelConfirm ? (
            <button className="button button--secondary" type="button" disabled={approving || delivering || cancelling}
              onClick={() => setCancelConfirm(true)}>Avbryt import</button>
          ) : (
            <div className="approval-confirmation" role="alert">
              <strong>Avbryt importen?</strong>
              <p>Ingen commit skapas. Den här importen stängs och projektet kan ta emot en ny ZIP.</p>
              <div className="result-primary-action">
                <button className="button button--secondary" type="button" disabled={cancelling} onClick={() => setCancelConfirm(false)}>Behåll importen</button>
                <button className="button" type="button" disabled={cancelling} onClick={confirmCancelImport}>
                  {cancelling ? 'Avbryter import…' : 'Ja, avbryt import'}
                </button>
              </div>
            </div>
          )}
        </div>
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
