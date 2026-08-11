import { useState } from 'react';
import { createWorkPullRequest, getProjectWorkCommits } from '../api/projects';
import type { PullRequestResponse } from '../api/imports';

export default function PullRequestComposer({ projectId, onCreated, onCancel }: {
  projectId: string;
  onCreated: (created: PullRequestResponse) => void | Promise<void>;
  onCancel: () => void;
}) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [busy, setBusy] = useState(false);
  const [filling, setFilling] = useState(false);
  const [error, setError] = useState('');

  async function fillFromCommits() {
    if (filling) return;
    setFilling(true); setError('');
    try {
      const history = await getProjectWorkCommits(projectId);
      if (!history.githubAvailable || history.commits.some(commit => commit.fallback)) {
        throw new Error('Commitmeddelandena kunde inte hämtas säkert från GitHub. Skriv PR-beskrivningen manuellt.');
      }
      if (history.commits.length === 0) throw new Error('Det finns inga Work-commits att lägga in i beskrivningen.');
      const commits = history.commits.slice().reverse();
      const text = commits.map(commit => `- ${commit.message.trim().replace(/\n/g, '\n  ')}`).join('\n');
      setDescription(text.slice(0, 65536));
      setTitle(currentTitle => {
        if (currentTitle.trim()) return currentTitle;
        const firstCommitLine = commits[0].message.trim().split('\n', 1)[0]?.trim() ?? '';
        return firstCommitLine.slice(0, 256);
      });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Commitmeddelandena kunde inte hämtas.');
    } finally { setFilling(false); }
  }

  async function create() {
    if (busy || !title.trim() || !description.trim()) return;
    setBusy(true); setError('');
    try { await onCreated(await createWorkPullRequest(projectId, title, description)); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Pull request kunde inte skapas.'); }
    finally { setBusy(false); }
  }

  return <section className="approval-confirmation" aria-labelledby="pull-request-metadata-heading">
    <h3 id="pull-request-metadata-heading">Skapa pull request</h3>
    <p>Titel och beskrivning är obligatoriska och skickas till GitHub först när du väljer att skapa PR:n.</p>
    {error && <p role="alert" className="status-message status-message--error">{error}</p>}
    <label htmlFor="pull-request-title">Titel</label>
    <input id="pull-request-title" type="text" maxLength={256} value={title} disabled={busy}
      onChange={event => setTitle(event.target.value)} />
    <p className="field-hint">{title.length}/256 tecken.</p>
    <label htmlFor="pull-request-description">Beskrivning</label>
    <textarea id="pull-request-description" rows={10} maxLength={65536} value={description} disabled={busy}
      onChange={event => setDescription(event.target.value)} />
    <p className="field-hint">{description.length}/65536 tecken. Beskrivningen kan skrivas manuellt eller fyllas med Workens commitmeddelanden.</p>
    <div className="result-primary-action">
      <button className="button button--secondary" type="button" disabled={busy || filling} onClick={() => void fillFromCommits()}>
        {filling ? 'Hämtar commitmeddelanden…' : 'Fyll från commitmeddelanden'}
      </button>
      <button className="button" type="button" disabled={busy || !title.trim() || !description.trim()} onClick={() => void create()}>
        {busy ? 'Skapar pull request…' : 'Skapa pull request'}
      </button>
      <button className="button button--secondary" type="button" disabled={busy} onClick={onCancel}>Avbryt</button>
    </div>
  </section>;
}
