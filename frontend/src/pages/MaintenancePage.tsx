import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  cleanupWorkBranches,
  getWorkBranchCleanupPreview,
  type WorkBranchCleanupCandidate,
  type WorkBranchCleanupPreview,
  type WorkBranchCleanupResult,
} from '../api/maintenance';

export default function MaintenancePage() {
  const [preview, setPreview] = useState<WorkBranchCleanupPreview | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<WorkBranchCleanupResult | null>(null);
  const [cleanupConfirmed, setCleanupConfirmed] = useState(false);

  const safeCandidates = useMemo(() => preview?.candidates.filter((candidate) => candidate.deletable) ?? [], [preview]);

  async function loadPreview() {
    setLoading(true);
    setError(null);
    setResult(null);
    setCleanupConfirmed(false);
    try {
      setPreview(await getWorkBranchCleanupPreview());
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Underhållsinventeringen misslyckades.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void loadPreview(); }, []);

  async function deleteSafeCandidates(candidates: WorkBranchCleanupCandidate[]) {
    if (candidates.length === 0) return;
    setDeleting(true);
    setError(null);
    try {
      const response = await cleanupWorkBranches(candidates);
      setResult(response);
      await loadPreview();
      setResult(response);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Raderingen kunde inte slutföras.');
    } finally {
      setDeleting(false);
    }
  }

  return (
    <section className="page-card">
      <p className="eyebrow">Underhåll</p>
      <h1>Work-brancher</h1>
      <p className="lead">Inventerar endast brancher i zip-GitHubs egen <code>zip-github/work-…</code>-namespace över repositories du får se via GitHub App-installationerna.</p>
      <p>Ingen branch raderas automatiskt. Backend gör en ny säkerhetskontroll direkt före varje explicit radering.</p>

      {loading && <p role="status">Inventerar repositories och Work-brancher…</p>}
      {error && <p className="status-message status-message--error" role="alert">{error}</p>}

      {preview && !loading && (
        <>
          <div className="summary-grid" aria-label="Sammanfattning">
            <div><strong>{preview.repositoriesChecked}</strong><span> repositories kontrollerade</span></div>
            <div><strong>{preview.workBranchesFound}</strong><span> Work-brancher hittade</span></div>
            <div><strong>{preview.safeToDelete}</strong><span> säkra att radera</span></div>
            <div><strong>{preview.inUseOrProtected}</strong><span> används eller skyddas</span></div>
            <div><strong>{preview.unverifiable}</strong><span> kan inte verifieras säkert</span></div>
          </div>

          {preview.issues.length > 0 && (
            <div className="status-message status-message--warning" role="status">
              <strong>Inventeringen är inte fullständig.</strong> Följande delar kunde inte verifieras och är därför aldrig raderbara:
              <ul>{preview.issues.map((issue, index) => <li key={`${issue.scope}:${index}`}><strong>{issue.scope}</strong>: {issue.reason}</li>)}</ul>
            </div>
          )}

          {preview.candidates.length === 0 ? (
            <p>Inga zip-GitHub Work-brancher hittades.</p>
          ) : (
            <div className="table-scroll">
              <table>
                <thead><tr><th>Repository</th><th>Branch</th><th>PR</th><th>Status</th><th>Bedömning</th></tr></thead>
                <tbody>
                  {preview.candidates.map((candidate) => (
                    <tr key={`${candidate.githubInstallationId}:${candidate.githubRepositoryId}:${candidate.branchName}`}>
                      <td>
                        {candidate.projectId ? (
                          <Link to={`/projects/${candidate.projectId}`}>{candidate.repositoryFullName}</Link>
                        ) : candidate.repositoryFullName}
                      </td>
                      <td>
                        {candidate.branchUrl ? (
                          <a href={candidate.branchUrl} target="_blank" rel="noreferrer"><code>{candidate.branchName}</code></a>
                        ) : <code>{candidate.branchName}</code>}
                      </td>
                      <td>
                        {candidate.pullRequestNumber && candidate.pullRequestUrl ? (
                          <a href={candidate.pullRequestUrl} target="_blank" rel="noreferrer">#{candidate.pullRequestNumber}</a>
                        ) : '–'}
                      </td>
                      <td>{candidate.deletable ? 'Säker att radera' : candidate.classification === 'UNVERIFIED' ? 'Kan inte verifieras säkert' : 'Behåll'}</td>
                      <td>{candidate.reason}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {safeCandidates.length > 0 && (
            <label className="checkbox-row">
              <input type="checkbox" checked={cleanupConfirmed} onChange={(event) => setCleanupConfirmed(event.target.checked)} />
              Jag har granskat förhandsvisningen och vill radera endast de {safeCandidates.length} brancher som fortfarande klarar backendens säkerhetskontroll.
            </label>
          )}
          <div className="button-row">
            <button className="button button--secondary" type="button" onClick={() => void loadPreview()} disabled={loading || deleting}>Inventera igen</button>
            <button className="button button--danger" type="button" disabled={safeCandidates.length === 0 || !cleanupConfirmed || deleting}
              onClick={() => void deleteSafeCandidates(safeCandidates)}>
              {deleting ? 'Kontrollerar och raderar…' : `Ta bort ${safeCandidates.length} säkert identifierade brancher`}
            </button>
          </div>
        </>
      )}

      {result && (
        <section aria-labelledby="cleanup-result-heading">
          <h2 id="cleanup-result-heading">Resultat från senaste radering</h2>
          <ul>
            {result.results.map((item, index) => <li key={`${item.repositoryFullName}:${item.branchName}:${index}`}><strong>{item.status}</strong> – {item.repositoryFullName} / <code>{item.branchName}</code>: {item.reason}</li>)}
          </ul>
        </section>
      )}
    </section>
  );
}
