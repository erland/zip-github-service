import { useEffect, useState } from 'react';
import { getShortcutRelease, type ShortcutRelease } from '../api/shortcut';

function formatBytes(value: number | null) {
  if (value == null) return '';
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${Math.round(value / 1024)} KiB`;
  return `${(value / (1024 * 1024)).toFixed(1)} MiB`;
}

export default function ShortcutInstallPage() {
  const [release, setRelease] = useState<ShortcutRelease | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getShortcutRelease().then(value => { if (!cancelled) setRelease(value); })
      .catch(reason => { if (!cancelled) setError(reason instanceof Error ? reason.message : 'Shortcut-information kunde inte hämtas.'); });
    return () => { cancelled = true; };
  }, []);

  return (
    <section className="page-card shortcut-install-page">
      <p className="eyebrow">iPhone och iPad</p>
      <h1>Installera zip-github Shortcut</h1>
      <p className="lead">Shortcuten tar emot en ZIP från delningsbladet, laddar upp den till den kortlivade stagingytan och öppnar webbläsaren där du loggar in och väljer projekt.</p>

      {error && <p className="status-message status-message--error" role="alert">{error}</p>}
      {!release && !error && <p role="status">Kontrollerar aktuell Shortcut-version…</p>}

      {release && release.available && release.downloadUrl && (
        <>
          <div className="summary-grid" aria-label="Shortcut-version">
            <div><span>Version</span><strong>{release.version}</strong></div>
            <div><span>Generation</span><strong>{release.generation}</strong></div>
            <div><span>Storlek</span><strong>{formatBytes(release.sizeBytes)}</strong></div>
          </div>
          <a className="button" href={release.downloadUrl}>Ladda ner aktuell Shortcut</a>
          <p className="supporting-text">Filen är en statiskt publicerad, Apple-signerad releaseartefakt. zip-github genererar eller signerar inte Shortcuts dynamiskt.</p>
        </>
      )}

      {release && !release.available && (
        <p className="status-message status-message--warning" role="status">Ingen signerad Shortcut är publicerad på den här installationen ännu. En administratör måste publicera den försignerade <code>.shortcut</code>-filen.</p>
      )}

      <h2>Så använder du den</h2>
      <ol>
        <li>Ladda ner och öppna den signerade Shortcut-filen på iPhone eller iPad.</li>
        <li>Lägg till Shortcuten i appen Genvägar.</li>
        <li>Dela en ZIP-fil och välj zip-github-Shortcuten.</li>
        <li>Efter uppladdningen öppnas zip-github i webbläsaren för inloggning, claim och projektval.</li>
      </ol>

      <h2>Om en gammal Shortcut slutar fungera</h2>
      <p>En gammal upload credential kan spärras om den behöver roteras. Då ändras inget GitHub-lösenord eller GitHub App-behörighet: logga in här igen, hämta den aktuella Shortcut-versionen och ersätt den gamla installationen.</p>
    </section>
  );
}
