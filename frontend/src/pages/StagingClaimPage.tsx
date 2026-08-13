import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import RepositoryPicker from '../components/RepositoryPicker';
import { getRepositories, type RepositoryEntry } from '../api/repositories';
import { getProjectWork, type WorkSessionResponse } from '../api/projects';
import { prepareImportReview } from '../api/imports';
import { claimStagingImport, getClaimedStagingImport, promoteStagingImport, type ClaimedStagingImport } from '../api/staging';
import { markRepositoryRecent, repositoryKey } from '../repositories/recentRepositories';
import { suggestRepository } from '../repositories/repositorySuggestion';
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
  const [showRepositoryPicker, setShowRepositoryPicker] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedWork, setSelectedWork] = useState<WorkSessionResponse | null>(null);
  const [openPrConfirmed, setOpenPrConfirmed] = useState(false);
  const suggestion = useMemo(() => claimed ? suggestRepository(claimed.originalFilename, repositories) : null, [claimed, repositories]);
  const selectedRepository = useMemo(
    () => repositories.find((repository) => repositoryKey(repository) === selectedRepositoryKey) ?? null,
    [repositories, selectedRepositoryKey],
  );

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
        if (availableRepositories.length === 1) {
          setSelectedRepositoryKey(repositoryKey(availableRepositories[0]));
          setShowRepositoryPicker(false);
        }
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

  useEffect(() => {
    if (suggestion?.confidence === 'high' && !selectedRepositoryKey && repositories.length > 1) setShowRepositoryPicker(false);
  }, [suggestion, selectedRepositoryKey, repositories.length]);

  useEffect(() => {
    let cancelled = false;
    setSelectedWork(null);
    setOpenPrConfirmed(false);
    if (!selectedRepository?.projectId) return () => { cancelled = true; };
    getProjectWork(selectedRepository.projectId)
      .then((work) => { if (!cancelled) { setSelectedWork(work); setOpenPrConfirmed(work?.status !== 'PR_OPEN'); } })
      .catch((reason) => { if (!cancelled) setError(reason instanceof Error ? reason.message : 'Work-status kunde inte hämtas.'); });
    return () => { cancelled = true; };
  }, [selectedRepository?.projectId]);

  async function promote() {
    if (!claimed || !selectedRepository || promoting) return;
    if (selectedWork?.status === 'PR_OPEN' && !openPrConfirmed) {
      setError('Bekräfta först att ZIP-filen ska uppdatera den befintliga pull requesten.');
      return;
    }
    setPromoting(true);
    setError(null);
    try {
      markRepositoryRecent(selectedRepository);
      const result = await promoteStagingImport(claimed.stagingId, selectedRepository.projectId
        ? { projectId: selectedRepository.projectId }
        : { githubInstallationId: selectedRepository.githubInstallationId, githubRepositoryId: selectedRepository.githubRepositoryId }, selectedWork?.status === 'PR_OPEN' && openPrConfirmed);
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

  return <section className="page-card">
    <p className="eyebrow">Shortcut-import</p>
    <h1>Välj repository för ZIP-filen</h1>
    <p className="lead">Uppladdningen är privat bunden till din användare. zip-github förbereder repositoryt automatiskt när du fortsätter.</p>
    {claimed && <dl className="detail-list"><div><dt>Fil</dt><dd>{claimed.originalFilename}</dd></div><div><dt>Storlek</dt><dd>{formatBytes(claimed.sizeBytes)}</dd></div><div><dt>SHA-256</dt><dd><code>{claimed.sha256}</code></dd></div><div><dt>Giltig till</dt><dd>{new Date(claimed.expiresAt).toLocaleString('sv-SE')}</dd></div></dl>}
    {repositories.length > 0 ? <>
      {suggestion?.confidence === 'high' && !selectedRepository && !showRepositoryPicker && <section className="repository-suggestion" aria-labelledby="suggested-repository-heading">
        <p className="eyebrow">Föreslaget repository</p>
        <h2 id="suggested-repository-heading">{suggestion.repository.repositoryName}</h2>
        <p><code>{suggestion.repository.repositoryFullName}</code></p>
        <p>{suggestion.reason}</p>
        <div className="result-primary-action">
          <button className="button button--primary" type="button" onClick={() => setSelectedRepositoryKey(repositoryKey(suggestion.repository))}>Använd detta repository</button>
          <button className="button button--secondary" type="button" onClick={() => setShowRepositoryPicker(true)}>Välj ett annat repository</button>
        </div>
      </section>}
      {(showRepositoryPicker || !suggestion || suggestion.confidence !== 'high') && <RepositoryPicker repositories={repositories} mode="select" selectedRepositoryKey={selectedRepositoryKey} onSelect={(repository) => setSelectedRepositoryKey(repositoryKey(repository))} />}
      {suggestion?.confidence === 'high' && showRepositoryPicker && <button className="button button--secondary repository-suggestion-return" type="button" onClick={() => setShowRepositoryPicker(false)}>Visa föreslaget repository</button>}
      {suggestion?.confidence === 'high' && selectedRepository && !showRepositoryPicker && <button className="button button--secondary repository-suggestion-return" type="button" onClick={() => setShowRepositoryPicker(true)}>Välj ett annat repository</button>}
      <div className="repository-selected-summary" aria-live="polite">
        <span>Valt repository</span>
        {selectedRepository ? <><strong>{selectedRepository.repositoryName}</strong><small>{selectedRepository.repositoryFullName}</small></> : <em>Välj ett repository ovan.</em>}
      </div>
      {error && <p className="status-message status-message--error" role="alert">{error}</p>}
      {selectedWork?.status === 'PR_OPEN' && !openPrConfirmed && (
        <aside className="status-message status-message--warning" role="alert" aria-label="Öppen pull request">
          <strong>Det finns redan en öppen pull request för detta arbete</strong>
          <p>Den här ZIP-filen kommer att läggas på samma Work-branch och uppdatera den befintliga pull requesten.</p>
          {selectedWork.pullRequestUrl && <p><a href={selectedWork.pullRequestUrl} target="_blank" rel="noreferrer">Öppna pull request #{selectedWork.pullRequestNumber} på GitHub</a></p>}
          <button className="button" type="button" onClick={() => setOpenPrConfirmed(true)}>Ja, fortsätt med denna ZIP</button>
        </aside>
      )}
      <button className="button button--primary" type="button" disabled={!selectedRepositoryKey || promoting || (selectedWork?.status === 'PR_OPEN' && !openPrConfirmed)} onClick={() => void promote()}>{promoting ? 'Förbereder granskning…' : 'Fortsätt till granskning'}</button>
    </> : <><p>Inga repositories är tillgängliga för zip-github.</p><Link className="button button--secondary" to="/projects">Visa repositories</Link></>}
  </section>;
}
