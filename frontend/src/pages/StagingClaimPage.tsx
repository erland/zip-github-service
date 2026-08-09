import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getRepositories, type RepositoryEntry } from '../api/repositories';
import { prepareImportReview } from '../api/imports';
import { claimStagingImport, getClaimedStagingImport, promoteStagingImport, type ClaimedStagingImport } from '../api/staging';
import { STAGING_CLAIM_TOKEN_KEY } from '../staging/claimToken';

const STAGING_CLAIMED_ID_KEY = 'zipgithub.staging.claimed-id';

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KiB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MiB`;
}

export default function StagingClaimPage() {
  const navigate = useNavigate();
  const [state, setState] = useState<'claiming' | 'success' | 'error'>('claiming');
  const [claimed, setClaimed] = useState<ClaimedStagingImport | null>(null);
  const [repositories, setRepositories] = useState<RepositoryEntry[]>([]);
  const [selectedRepositoryKey, setSelectedRepositoryKey] = useState('');
  const [promoting, setPromoting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const token = sessionStorage.getItem(STAGING_CLAIM_TOKEN_KEY);
        const existingId = sessionStorage.getItem(STAGING_CLAIMED_ID_KEY);
        let value: ClaimedStagingImport;
        if (token) {
          value = await claimStagingImport(token);
          sessionStorage.removeItem(STAGING_CLAIM_TOKEN_KEY);
          sessionStorage.setItem(STAGING_CLAIMED_ID_KEY, value.stagingId);
        } else if (existingId) {
          value = await getClaimedStagingImport(existingId);
        } else {
          throw new Error('Claim-länken saknas eller har redan använts. Skicka ZIP-filen från Shortcuten igen.');
        }
        const availableRepositories = await getRepositories();
        if (cancelled) return;
        setClaimed(value);
        setRepositories(availableRepositories);
        if (availableRepositories.length === 1) setSelectedRepositoryKey(`${availableRepositories[0].githubInstallationId}:${availableRepositories[0].githubRepositoryId}`);
        setState('success');
      } catch (reason: unknown) {
        if (cancelled) return;
        setError(reason instanceof Error ? reason.message : 'ZIP-filen kunde inte kopplas till ditt konto.');
        setState('error');
      }
    }
    void load();
    return () => { cancelled = true; };
  }, []);

  async function promote() {
    if (!claimed || !selectedRepositoryKey || promoting) return;
    const selected = repositories.find((repository) => `${repository.githubInstallationId}:${repository.githubRepositoryId}` === selectedRepositoryKey);
    if (!selected) return;
    setPromoting(true);
    setError(null);
    try {
      const result = await promoteStagingImport(claimed.stagingId, selected.projectId
        ? { projectId: selected.projectId }
        : { githubInstallationId: selected.githubInstallationId, githubRepositoryId: selected.githubRepositoryId });
      await prepareImportReview(result.importId);
      sessionStorage.removeItem(STAGING_CLAIMED_ID_KEY);
      navigate(`/projects/${result.projectId}/imports/${result.importId}/review`);
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : 'ZIP-filen kunde inte förberedas för repositoryt.');
      setPromoting(false);
    }
  }

  if (state === 'claiming') {
    return <section className="page-card"><p className="eyebrow">Shortcut-import</p><h1>Kopplar ZIP-filen till ditt konto</h1><p role="status">Verifierar den tillfälliga uppladdningen…</p></section>;
  }

  if (state === 'error' && !claimed) {
    return <section className="page-card"><p className="eyebrow">Shortcut-import</p><h1>ZIP-filen kunde inte hämtas</h1><p className="status-message status-message--error" role="alert">{error}</p><p>Öppna Shortcuten igen och skicka ZIP-filen på nytt.</p><Link className="button button--secondary" to="/projects">Till repositories</Link></section>;
  }

  return <section className="page-card"><p className="eyebrow">Shortcut-import</p><h1>Välj repository för ZIP-filen</h1><p className="lead">Uppladdningen är privat bunden till din användare. zip-github förbereder repositoryt automatiskt när du fortsätter.</p>{claimed && <dl className="detail-list"><div><dt>Fil</dt><dd>{claimed.originalFilename}</dd></div><div><dt>Storlek</dt><dd>{formatBytes(claimed.sizeBytes)}</dd></div><div><dt>SHA-256</dt><dd><code>{claimed.sha256}</code></dd></div><div><dt>Giltig till</dt><dd>{new Date(claimed.expiresAt).toLocaleString('sv-SE')}</dd></div></dl>}{repositories.length > 0 ? <><fieldset className="choice-list"><legend>Repository</legend>{repositories.map(repository => { const key = `${repository.githubInstallationId}:${repository.githubRepositoryId}`; return <label key={key} className="choice-row"><input type="radio" name="staging-repository" value={key} checked={selectedRepositoryKey === key} onChange={() => setSelectedRepositoryKey(key)} /><span><strong>{repository.repositoryName}</strong><small>{repository.repositoryFullName}</small></span></label>; })}</fieldset>{error && <p className="status-message status-message--error" role="alert">{error}</p>}<button className="button button--primary" type="button" disabled={!selectedRepositoryKey || promoting} onClick={() => void promote()}>{promoting ? 'Förbereder granskning…' : 'Fortsätt till granskning'}</button></> : <><p>Inga repositories är tillgängliga för zip-github.</p><Link className="button button--secondary" to="/projects">Visa repositories</Link></>}</section>;
}
