import { FormEvent, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { cancelImport, createImport, prepareImportReview, SourceUploadResponse, uploadZip } from '../api/imports';
import { getProject, ProjectResponse } from '../api/projects';
import { getCurrentUser, type AuthenticatedUser } from '../api/auth';

type UploadState = 'idle' | 'creating' | 'uploading' | 'preparing' | 'error' | 'cancelled';

export default function NewImportPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const existingImportId = searchParams.get('importId');
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [currentUser, setCurrentUser] = useState<AuthenticatedUser | null>(null);
  const [authorMode, setAuthorMode] = useState<'self' | 'other'>('self');
  const [authorName, setAuthorName] = useState('');
  const [authorEmail, setAuthorEmail] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [state, setState] = useState<UploadState>('idle');
  const [progress, setProgress] = useState(0);
  const [message, setMessage] = useState('');
  const [result, setResult] = useState<SourceUploadResponse | null>(null);
  const [currentImportId, setCurrentImportId] = useState<string | null>(existingImportId);
  const [cancelConfirm, setCancelConfirm] = useState(false);
  const [cancellingImport, setCancellingImport] = useState(false);
  const controller = useRef<AbortController | null>(null);

  const busy = state === 'creating' || state === 'uploading' || state === 'preparing';

  useEffect(() => {
    if (!projectId) return;
    let active = true;
    Promise.all([getProject(projectId), getCurrentUser()])
      .then(([loadedProject, loadedUser]) => {
        if (!active) return;
        setProject(loadedProject);
        setCurrentUser(loadedUser);
      })
      .catch((reason) => setMessage(reason instanceof Error ? reason.message : 'Projektet kunde inte hämtas.'));
    return () => { active = false; };
  }, [projectId, existingImportId]);

  async function prepareReview(importId: string) {
    if (!project) return;
    setState('preparing');
    setMessage('Analyserar ZIP-filen, låser GitHub-versionen och skapar granskningsplan…');
    try {
      await prepareImportReview(importId);
      navigate(`/projects/${project.id}/imports/${importId}/review`);
    } catch (error) {
      setState('error');
      setMessage(error instanceof Error ? error.message : 'Granskningsplanen kunde inte skapas. Försök igen utan att ladda upp ZIP-filen på nytt.');
    }
  }

  async function confirmCancelImport() {
    if (!project || !currentImportId || busy || cancellingImport) return;
    setCancellingImport(true);
    setMessage('');
    try {
      await cancelImport(currentImportId);
      navigate(`/projects/${project.id}`, { replace: true });
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Importen kunde inte avbrytas.');
      setState('error');
      setCancelConfirm(false);
    } finally {
      setCancellingImport(false);
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!project || !file || busy) return;
    const abortController = new AbortController();
    controller.current = abortController;
    setMessage('');
    setResult(null);
    setProgress(0);
    try {
      setState('creating');
      const customAuthor = authorMode === 'other' ? { name: authorName.trim(), email: authorEmail.trim() } : undefined;
      const importId = existingImportId || (await createImport(project.id, customAuthor)).id;
      setCurrentImportId(importId);
      setState('uploading');
      const uploaded = await uploadZip(importId, file, setProgress, abortController.signal);
      setResult(uploaded);
      await prepareReview(uploaded.importId);
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        setState('cancelled');
        setMessage('Uppladdningen avbröts. Du kan välja filen och försöka igen.');
      } else {
        setState('error');
        setMessage(error instanceof Error ? error.message : 'Ett oväntat fel uppstod.');
      }
    } finally {
      controller.current = null;
    }
  }

  return (
    <section className="page-card" aria-labelledby="new-import-heading">
      <p><Link className="back-link" to={project ? `/projects/${project.id}` : '/projects'}>← Till projektet</Link></p>
      <p className="eyebrow">{existingImportId ? 'Återöppnad import' : 'Ny import'}</p>
      <h1 id="new-import-heading">Ladda upp projekt-ZIP</h1>
      <p className="lead">{project ? `Importen förbereds för ${project.repositoryFullName}.` : 'Välj ett giltigt projekt innan importen startas.'}</p>

      <ol className="step-list" aria-label="Importflöde">
        <li className="step-list__current" aria-current="step"><span>1</span><strong>Välj ZIP</strong></li>
        <li><span>2</span><strong>Granska förändringar</strong></li>
        <li><span>3</span><strong>Godkänn och skapa commit</strong></li>
      </ol>

      <form className="import-form" onSubmit={submit}>
        <div className="work-target-summary">
          <strong>Arbetsbranch hanteras automatiskt</strong>
          <p>Första importen startar ett arbete från projektets standardbranch. Nästa ZIP jämförs automatiskt mot senaste commit på samma arbetsbranch.</p>
        </div>

        <fieldset className="identity-fieldset" disabled={!project || busy || Boolean(existingImportId)}>
          <legend>Författare till ändringarna</legend>
          <label className="radio-option">
            <input type="radio" name="author-mode" value="self" checked={authorMode === 'self'} onChange={() => setAuthorMode('self')} />
            <span><strong>Jag själv</strong>{currentUser && <small>{currentUser.gitName} &lt;{currentUser.gitEmail}&gt;</small>}</span>
          </label>
          <label className="radio-option">
            <input type="radio" name="author-mode" value="other" checked={authorMode === 'other'} onChange={() => setAuthorMode('other')} />
            <span><strong>Någon annan</strong><small>Använd när ZIP-filen innehåller ändringar skapade av en annan person.</small></span>
          </label>
          {authorMode === 'other' && (
            <div className="identity-fields">
              <label htmlFor="author-name">Namn</label>
              <input id="author-name" value={authorName} required onChange={(event) => setAuthorName(event.target.value)} autoComplete="name" />
              <label htmlFor="author-email">E-post</label>
              <input id="author-email" type="email" value={authorEmail} required onChange={(event) => setAuthorEmail(event.target.value)} autoComplete="email" />
            </div>
          )}
          <p className="field-help">Committer är alltid den inloggade GitHub-användaren som godkänner importen. Author används i bland annat Git history och blame.</p>
        </fieldset>

        <label htmlFor="zip-file">Projektarkiv</label>
        <input
          id="zip-file"
          name="zip-file"
          type="file"
          accept=".zip,application/zip,application/x-zip-compressed"
          disabled={!project || busy || Boolean(result)}
          aria-describedby="zip-file-help"
          onChange={(event) => {
            setFile(event.target.files?.[0] ?? null);
            setState('idle');
            setMessage('');
            setProgress(0);
          }}
        />
        <p className="field-help" id="zip-file-help">Välj en ZIP från Filer, iCloud Drive eller enhetens lokala lagring. Filen jämförs inte och skrivs inte till GitHub förrän senare steg har granskats och godkänts.</p>

        {file && <p className="selected-file"><strong>Vald fil:</strong> {file.name} · {formatBytes(file.size)}</p>}

        {(state === 'creating' || state === 'uploading') && (
          <div className="upload-progress" aria-live="polite">
            <div className="upload-progress__row"><strong>{state === 'creating' ? 'Skapar import…' : 'Laddar upp…'}</strong><span>{progress}%</span></div>
            <progress value={progress} max="100" aria-label="Uppladdningsförlopp">{progress}%</progress>
            <button className="button button--secondary" type="button" onClick={() => controller.current?.abort()}>Avbryt</button>
          </div>
        )}

        {message && <p className={`status-message status-message--${state}`} role={state === 'error' ? 'alert' : 'status'}>{message}</p>}
        {result && <p className="upload-result"><strong>SHA-256:</strong> <code>{result.sha256}</code><br /><strong>Retention:</strong> till {new Date(result.retentionDeadline).toLocaleString('sv-SE')}</p>}

        {result && state === 'error' ? (
          <button className="button" type="button" disabled={busy} onClick={() => prepareReview(result.importId)}>
            Försök skapa granskningsplan igen
          </button>
        ) : (
          <button className="button" type="submit" disabled={!project || !file || busy || Boolean(result) || (authorMode === 'other' && (!authorName.trim() || !authorEmail.trim()))}>
            {state === 'preparing' ? 'Förbereder granskning…' : 'Ladda upp ZIP'}
          </button>
        )}

        {currentImportId && !busy && (
          <div className="review-cancel-action">
            {!cancelConfirm ? (
              <button className="button button--secondary" type="button" disabled={cancellingImport} onClick={() => setCancelConfirm(true)}>Avbryt import</button>
            ) : (
              <div className="approval-confirmation" role="alert">
                <strong>Avbryt importen?</strong>
                <p>Ingen commit skapas. Importen stängs och du kan börja om med en annan ZIP.</p>
                <div className="result-primary-action">
                  <button className="button button--secondary" type="button" disabled={cancellingImport} onClick={() => setCancelConfirm(false)}>Behåll importen</button>
                  <button className="button" type="button" disabled={cancellingImport} onClick={confirmCancelImport}>{cancellingImport ? 'Avbryter import…' : 'Ja, avbryt import'}</button>
                </div>
              </div>
            )}
          </div>
        )}
      </form>
    </section>
  );
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KiB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MiB`;
}
