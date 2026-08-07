import { FormEvent, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { createImport, createImportPlan, createRepositorySnapshot, getImport, SourceUploadResponse, uploadZip } from '../api/imports';
import { getProject, ProjectResponse } from '../api/projects';

type UploadState = 'idle' | 'creating' | 'uploading' | 'complete' | 'preparing' | 'error' | 'cancelled';

export default function NewImportPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const existingImportId = searchParams.get('importId');
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [existingImportBranch, setExistingImportBranch] = useState<string | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [branch, setBranch] = useState('main');
  const [state, setState] = useState<UploadState>('idle');
  const [progress, setProgress] = useState(0);
  const [message, setMessage] = useState('');
  const [result, setResult] = useState<SourceUploadResponse | null>(null);
  const controller = useRef<AbortController | null>(null);

  const busy = state === 'creating' || state === 'uploading' || state === 'preparing';

  useEffect(() => {
    if (!projectId) return;
    let active = true;
    Promise.all([getProject(projectId), existingImportId ? getImport(existingImportId) : Promise.resolve(null)])
      .then(([loadedProject, loadedImport]) => {
        if (!active) return;
        setProject(loadedProject);
        const selectedBranch = loadedImport?.baseBranch || loadedProject.defaultBranch;
        setExistingImportBranch(loadedImport?.baseBranch || null);
        setBranch(selectedBranch);
      })
      .catch((reason) => setMessage(reason instanceof Error ? reason.message : 'Projektet kunde inte hämtas.'));
    return () => { active = false; };
  }, [projectId, existingImportId]);

  async function prepareReview() {
    if (!project || !result || busy) return;
    try {
      setState('preparing');
      setMessage('Låser GitHub-versionen och skapar granskningsplan…');
      await createRepositorySnapshot(result.importId);
      await createImportPlan(result.importId);
      navigate(`/projects/${project.id}/imports/${result.importId}/review`);
    } catch (error) {
      setState('error');
      setMessage(error instanceof Error ? error.message : 'Granskningsplanen kunde inte skapas.');
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
      const importId = existingImportId || (await createImport(project.id, branch)).id;
      setState('uploading');
      const uploaded = await uploadZip(importId, file, setProgress, abortController.signal);
      setResult(uploaded);
      setState('complete');
      setMessage('ZIP-filen är uppladdad och redo för säker inspektion.');
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
        <li><span>3</span><strong>Godkänn och skapa PR</strong></li>
      </ol>

      <form className="import-form" onSubmit={submit}>
        <label htmlFor="target-branch">Jämförelsebranch</label>
        <select id="target-branch" value={branch} onChange={(event) => setBranch(event.target.value)} disabled={!project || busy || Boolean(existingImportBranch)} aria-describedby="branch-help">
          <option value={project?.defaultBranch ?? 'main'}>{project?.defaultBranch ?? 'main'}</option>
        </select>
        <p className="field-help" id="branch-help">Importen jämförs med den här branchen och låses senare till ett exakt commit-SHA.</p>

        <label htmlFor="zip-file">Projektarkiv</label>
        <input
          id="zip-file"
          name="zip-file"
          type="file"
          accept=".zip,application/zip,application/x-zip-compressed"
          disabled={!project || busy}
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

        {result ? (
          <button className="button" type="button" disabled={busy} onClick={prepareReview}>
            {state === 'preparing' ? 'Förbereder granskning…' : 'Skapa granskningsplan'}
          </button>
        ) : (
          <button className="button" type="submit" disabled={!project || !file || busy}>Ladda upp ZIP</button>
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
