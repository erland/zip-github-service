import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { approveImportPlan, cancelImport, createImportSelection, deliverImport, findDelivery, getExternalBranchChanges, getImportPlan, getImportPlanApproval, getImportSelection, ExternalBranchChangesResponse, ImportPlanApprovalResponse, ImportPlanEntry, ImportPlanResponse, prepareImportWorkspace } from '../api/imports';
import { defaultSelectedPaths, ReviewFileTree } from '../components/ReviewFileTree';

type ReviewFilter = 'CHANGES' | 'EXTERNAL' | 'BLOCKED' | 'WARNINGS' | 'UNCHANGED' | 'IGNORED' | 'ALL';

const filters: Array<{ id: ReviewFilter; label: string }> = [
  { id: 'CHANGES', label: 'Förändringar' },
  { id: 'EXTERNAL', label: 'Externa ändringar' },
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
  const [commitMessage, setCommitMessage] = useState(() => importId ? `Apply approved ZIP import ${importId}` : '');
  const [externalChanges, setExternalChanges] = useState<ExternalBranchChangesResponse | null>(null);
  const [externalChangesAcknowledged, setExternalChangesAcknowledged] = useState(false);

  useEffect(() => {
    if (!importId) {
      setError('Import-ID saknas.');
      setLoading(false);
      return;
    }
    let active = true;
    async function loadReviewState() {
      try {
        const [loadedPlan, loadedExternalChanges] = await Promise.all([getImportPlan(importId!), getExternalBranchChanges(importId!)]);
        if (!active) return;
        setPlan(loadedPlan);
        setExternalChanges(loadedExternalChanges);

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
        if (existingApproval) {
          setApproval(existingApproval);
          setCommitMessage(existingApproval.commitMessage);
        } else if (importId) {
          setCommitMessage(`Apply approved ZIP import ${importId}`);
        }
      } catch (reason) {
        if (active) setError(reason instanceof Error ? reason.message : 'Importplanen kunde inte hämtas.');
      } finally {
        if (active) setLoading(false);
      }
    }
    void loadReviewState();
    return () => { active = false; };
  }, [importId, navigate, projectId]);

  const externalPathSet = useMemo(() => new Set(externalChanges?.changedPaths ?? []), [externalChanges]);
  const overlappingExternalPaths = useMemo(() => new Set((plan?.entries ?? []).filter((entry) => externalPathSet.has(entry.path) && entry.status !== 'UNCHANGED' && entry.status !== 'IGNORED').map((entry) => entry.path)), [plan, externalPathSet]);
  const visibleEntries = useMemo(() => plan?.entries.filter((entry) => matchesFilter(entry, filter, overlappingExternalPaths)) ?? [], [plan, filter, overlappingExternalPaths]);

  async function approveAndDeliver() {
    const selectedExternalOverlap = [...selectedPaths].some((path) => overlappingExternalPaths.has(path));
    if (!importId || !projectId || !plan || approving || delivering || selectedPaths.size === 0 || (selectedExternalOverlap && !externalChangesAcknowledged)) return;
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
        recordedApproval = await approveImportPlan(importId, plan.planDigestSha256, digest, commitMessage);
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
        confirmCancelImport={confirmCancelImport}
        commitMessage={commitMessage} setCommitMessage={setCommitMessage} externalChanges={externalChanges} externalChangedPaths={overlappingExternalPaths} externalChangesAcknowledged={externalChangesAcknowledged} setExternalChangesAcknowledged={setExternalChangesAcknowledged} />}
    </section>
  );
}

function ReviewContent({ plan, filter, setFilter, entries, approving, approval, approveAndDeliver, delivering, retryApprovedDelivery,
  selectedPaths, setSelectedPaths, overridePaths, setOverridePaths, selectionLocked, cancelConfirm, setCancelConfirm, cancelling, confirmCancelImport,
  commitMessage, setCommitMessage, externalChanges, externalChangedPaths, externalChangesAcknowledged, setExternalChangesAcknowledged }: {
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
  commitMessage: string;
  setCommitMessage: (value: string) => void;
  externalChanges: ExternalBranchChangesResponse | null;
  externalChangedPaths: ReadonlySet<string>;
  externalChangesAcknowledged: boolean;
  setExternalChangesAcknowledged: (value: boolean) => void;
}) {
  const selectedExternalOverlap = [...selectedPaths].filter((path) => externalChangedPaths.has(path));
  return (
    <>
      {externalChanges?.branchChanged && <aside className="status-message status-message--warning" aria-label="GitHub-branchen har ändrats">
        <strong>Work-branchen har ändrats på GitHub sedan zip-githubs senast kända commit.</strong>
        <p>{externalChangedPaths.size > 0
          ? `${externalChangedPaths.size} fil${externalChangedPaths.size === 1 ? '' : 'er'} som ändrades externt skulle också ändras eller tas bort av den här ZIP-filen. Kontrollera dem särskilt innan du godkänner.`
          : 'Den här ZIP-filen skriver inte över någon av de externt ändrade sökvägar som GitHub kunde identifiera.'}</p>
        {externalChanges.previousKnownHeadSha && externalChanges.reviewBaseHeadSha && <p><code>{externalChanges.previousKnownHeadSha.slice(0,12)}</code> → <code>{externalChanges.reviewBaseHeadSha.slice(0,12)}</code></p>}
      </aside>}

      <div className={`review-decision ${selectedPaths.size > 0 ? 'review-decision--ready' : 'review-decision--blocked'}`} role="status">
        <div>
          <strong>{selectedPaths.size > 0 ? 'Urvalet kan godkännas' : 'Välj minst en förändring'}</strong>
          <p>{plan.blocked > 0
            ? `${plan.blocked} blockerande post${plan.blocked === 1 ? '' : 'er'} finns i planen. Överstyrbara poster kan tas med efter ett uttryckligt riskgodkännande; hårt blockerade poster kan aldrig levereras.`
            : 'Inga blockerande policyträffar hittades.'}</p>
        </div>
        <span className="status-badge">{plan.status}</span>
      </div>

      <div className="review-summary" aria-label="Sammanfattning av importplanen">
        <span><strong>{plan.added}</strong> tillagda</span>
        <span><strong>{plan.modified}</strong> ändrade</span>
        <span><strong>{plan.hardBlocked}</strong> hårt blockerade</span>
        <span><strong>{plan.overridableBlocked}</strong> överstyrbara</span>
        <span><strong>{plan.warnings}</strong> varningar</span>
        <span><strong>{plan.unchanged}</strong> oförändrade</span>
        <span><strong>{plan.ignored}</strong> ignorerade</span>
      </div>

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
            {candidate.label} ({filterCount(plan, candidate.id, externalChangedPaths)})
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
          overridePaths={overridePaths} onOverridePathsChange={setOverridePaths} externalChangedPaths={externalChangedPaths} locked={selectionLocked} />
      )}


      <section className="commit-message-editor" aria-labelledby="commit-message-heading">
        <h2 id="commit-message-heading">Commitmeddelande</h2>
        <p>Det här meddelandet används för committen på Work-branchen. Du kan ersätta förslaget helt innan du godkänner.</p>
        <label htmlFor="commit-message">Meddelande</label>
        <textarea id="commit-message" rows={3} maxLength={500} value={commitMessage}
          disabled={Boolean(approval) || approving || delivering}
          onChange={(event) => setCommitMessage(event.target.value)} />
        <p className="field-hint">{commitMessage.length}/500 tecken. Tomt meddelande kan inte godkännas.</p>
      </section>

      <div className="review-actions">
        {approval ? (
          <div className="approval-confirmation" role="status">
            <strong>Förändringarna är godkända</strong>
            <p>Urval och commitmeddelande är låsta. Föregående commit/push slutfördes inte, så du kan säkert försöka leveransen igen.</p>
            <dl className="approval-summary">
              <div><dt>Commitmeddelande</dt><dd><code>{approval.commitMessage}</code></dd></div>
              <div><dt>Base ref</dt><dd><code>{plan.baseCommitSha}</code></dd></div>
              <div><dt>Valda filer</dt><dd>{selectedPaths.size}</dd></div>
            </dl>
            <button className="button" type="button" disabled={delivering} onClick={retryApprovedDelivery}>
              {delivering ? 'Försöker skapa commit igen…' : 'Försök skapa commit igen'}
            </button>
          </div>
        ) : (
          <>
            <p>{selectionLocked
              ? 'Urvalet är låst. Du kan fortfarande justera commitmeddelandet innan det slutliga godkännandet.'
              : selectedPaths.size > 0
                ? 'Ett klick låser urvalet, registrerar godkännandet och skapar sedan commit på arbetsbranchen.'
                : 'Välj minst en förändring innan urvalet kan godkännas.'}</p>
            {selectedExternalOverlap.length > 0 && <label className="checkbox-row status-message status-message--warning">
              <input type="checkbox" checked={externalChangesAcknowledged} onChange={(event) => setExternalChangesAcknowledged(event.target.checked)} />
              Jag förstår att {selectedExternalOverlap.length} vald{selectedExternalOverlap.length === 1 ? '' : 'a'} sökväg{selectedExternalOverlap.length === 1 ? '' : 'ar'} ersätter ändringar som tillkommit på GitHub efter zip-githubs senast kända commit.
            </label>}
            <dl className="approval-summary" aria-label="Slutlig commitbekräftelse">
              <div><dt>Commitmeddelande</dt><dd><code>{commitMessage.trim() || '—'}</code></dd></div>
              <div><dt>Base ref</dt><dd><code>{plan.baseCommitSha}</code></dd></div>
              <div><dt>Valda filer</dt><dd>{selectedPaths.size}</dd></div>
            </dl>
            <button className="button" type="button" disabled={selectedPaths.size === 0 || commitMessage.trim().length === 0 || approving || delivering || (selectedExternalOverlap.length > 0 && !externalChangesAcknowledged)}
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

function filterCount(plan: ImportPlanResponse, filter: ReviewFilter, externalChangedPaths: ReadonlySet<string> = new Set()): number {
  if (filter === 'CHANGES') return plan.added + plan.modified;
  if (filter === 'EXTERNAL') return externalChangedPaths.size;
  if (filter === 'BLOCKED') return plan.blocked;
  if (filter === 'WARNINGS') return plan.warnings;
  if (filter === 'UNCHANGED') return plan.unchanged;
  if (filter === 'IGNORED') return plan.ignored;
  return plan.entries.length;
}

function matchesFilter(entry: ImportPlanEntry, filter: ReviewFilter, externalChangedPaths: ReadonlySet<string> = new Set()): boolean {
  if (filter === 'ALL') return true;
  if (filter === 'CHANGES') return entry.status === 'ADDED' || entry.status === 'MODIFIED';
  if (filter === 'EXTERNAL') return externalChangedPaths.has(entry.path);
  if (filter === 'BLOCKED') return entry.status === 'BLOCKED';
  if (filter === 'WARNINGS') return entry.severity === 'WARNING';
  return entry.status === filter;
}
