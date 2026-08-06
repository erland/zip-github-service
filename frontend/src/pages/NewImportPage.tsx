import { FormEvent, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { createImport, SourceUploadResponse, uploadZip } from '../api/imports';
import { demoProjects } from '../data/demoProjects';

type UploadState = 'idle' | 'creating' | 'uploading' | 'complete' | 'error' | 'cancelled';

export default function NewImportPage() {
  const { projectId } = useParams();
  const project = demoProjects.find((candidate) => candidate.id === projectId);
  const [file, setFile] = useState<File | null>(null);
  const [branch, setBranch] = useState(project?.defaultBranch ?? 'main');
  const [state, setState] = useState<UploadState>('idle');
  const [progress, setProgress] = useState(0);
  const [message, setMessage] = useState('');
  const [result, setResult] = useState<SourceUploadResponse | null>(null);
  const controller = useRef<AbortController | null>(null);

  const busy = state === 'creating' || state === 'uploading';

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
      const importSession = await createImport(project.id, branch);
      setState('uploading');
      const uploaded = await uploadZip(importSession.id, file, setProgress, abortController.signal);
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
      <p className="eyebrow">Ny import</p>
      <h1 id="new-import-heading">Ladda upp projekt-ZIP</h1>
      <p className="lead">{project ? `Importen förbereds för ${project.repository}.` : 'Välj ett giltigt projekt innan importen startas.'}</p>

      <ol className="step-list" aria-label="Importflöde">
        <li className="step-list__current"><span>1</span><strong>Välj ZIP</strong></li>
        <li><span>2</span><strong>Granska förändringar</strong></li>
        <li><span>3</span><strong>Godkänn och skapa PR</strong></li>
      </ol>

      <form className="import-form" onSubmit={submit}>
        <label htmlFor="target-branch">Jämförelsebranch</label>
        <select id="target-branch" value={branch} onChange={(event) => setBranch(event.target.value)} disabled={!project || busy}>
          <option value={project?.defaultBranch ?? 'main'}>{project?.defaultBranch ?? 'main'}</option>
        </select>

        <label htmlFor="zip-file">Projektarkiv</label>
        <input
          id="zip-file"
          name="zip-file"
          type="file"
          accept=".zip,application/zip,application/x-zip-compressed"
          disabled={!project || busy}
          onChange={(event) => {
            setFile(event.target.files?.[0] ?? null);
            setState('idle');
            setMessage('');
            setProgress(0);
          }}
        />
        <p className="field-help">Välj en ZIP från Filer, iCloud Drive eller enhetens lokala lagring. Filen jämförs inte och skrivs inte till GitHub förrän senare steg har granskats och godkänts.</p>

        {file && <p className="selected-file"><strong>Vald fil:</strong> {file.name} · {formatBytes(file.size)}</p>}

        {busy && (
          <div className="upload-progress" aria-live="polite">
            <div className="upload-progress__row"><strong>{state === 'creating' ? 'Skapar import…' : 'Laddar upp…'}</strong><span>{progress}%</span></div>
            <progress value={progress} max="100">{progress}%</progress>
            <button className="button button--secondary" type="button" onClick={() => controller.current?.abort()}>Avbryt</button>
          </div>
        )}

        {message && <p className={`status-message status-message--${state}`} role={state === 'error' ? 'alert' : 'status'}>{message}</p>}
        {result && <p className="upload-result"><strong>SHA-256:</strong> <code>{result.sha256}</code><br /><strong>Retention:</strong> till {new Date(result.retentionDeadline).toLocaleString('sv-SE')}</p>}

        <button className="button" type="submit" disabled={!project || !file || busy || state === 'complete'}>
          {state === 'complete' ? 'Uppladdning klar' : 'Ladda upp ZIP'}
        </button>
      </form>
    </section>
  );
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KiB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MiB`;
}
