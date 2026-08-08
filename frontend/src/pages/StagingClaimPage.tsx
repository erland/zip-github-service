import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getProjects, type ProjectResponse } from '../api/projects';
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
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState('');
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
        const availableProjects = (await getProjects()).filter(project => project.active);
        if (cancelled) return;
        setClaimed(value);
        setProjects(availableProjects);
        if (availableProjects.length === 1) setSelectedProjectId(availableProjects[0].id);
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
    if (!claimed || !selectedProjectId || promoting) return;
    setPromoting(true);
    setError(null);
    try {
      const result = await promoteStagingImport(claimed.stagingId, selectedProjectId);
      await prepareImportReview(result.importId);
      sessionStorage.removeItem(STAGING_CLAIMED_ID_KEY);
      navigate(`/projects/${result.projectId}/imports/${result.importId}/review`);
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : 'ZIP-filen kunde inte promoveras till projektet.');
      setPromoting(false);
    }
  }

  if (state === 'claiming') {
    return <section className="page-card"><p className="eyebrow">Shortcut-import</p><h1>Kopplar ZIP-filen till ditt konto</h1><p role="status">Verifierar den tillfälliga uppladdningen…</p></section>;
  }

  if (state === 'error' && !claimed) {
    return <section className="page-card"><p className="eyebrow">Shortcut-import</p><h1>ZIP-filen kunde inte hämtas</h1><p className="status-message status-message--error" role="alert">{error}</p><p>Öppna Shortcuten igen och skicka ZIP-filen på nytt.</p><Link className="button button--secondary" to="/projects">Till projekt</Link></section>;
  }

  return <section className="page-card"><p className="eyebrow">Shortcut-import</p><h1>Välj projekt för ZIP-filen</h1><p className="lead">Uppladdningen är privat bunden till din användare. Ingen GitHub-operation görs förrän den vanliga importgranskningen godkänts.</p>{claimed && <dl className="detail-list"><div><dt>Fil</dt><dd>{claimed.originalFilename}</dd></div><div><dt>Storlek</dt><dd>{formatBytes(claimed.sizeBytes)}</dd></div><div><dt>SHA-256</dt><dd><code>{claimed.sha256}</code></dd></div><div><dt>Giltig till</dt><dd>{new Date(claimed.expiresAt).toLocaleString('sv-SE')}</dd></div></dl>}{projects.length > 0 ? <><fieldset className="choice-list"><legend>Projekt / Work</legend>{projects.map(project => <label key={project.id} className="choice-row"><input type="radio" name="staging-project" value={project.id} checked={selectedProjectId === project.id} onChange={() => setSelectedProjectId(project.id)} /><span><strong>{project.name}</strong><small>{project.repositoryFullName} · {project.defaultBranch}</small></span></label>)}</fieldset>{error && <p className="status-message status-message--error" role="alert">{error}</p>}<button className="button button--primary" type="button" disabled={!selectedProjectId || promoting} onClick={() => void promote()}>{promoting ? 'Förbereder granskning…' : 'Fortsätt till granskning'}</button></> : <><p>Du har inget aktivt projekt att välja.</p><Link className="button button--secondary" to="/projects/new">Skapa projekt</Link></>}</section>;
}
