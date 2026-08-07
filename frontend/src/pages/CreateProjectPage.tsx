import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getGitHubInstallations, getInstallationRepositories, type GitHubInstallation, type GitHubRepository } from '../api/github';
import { createProject } from '../api/projects';

export default function CreateProjectPage() {
  const navigate = useNavigate();
  const [installations, setInstallations] = useState<GitHubInstallation[]>([]);
  const [repositories, setRepositories] = useState<GitHubRepository[]>([]);
  const [installationId, setInstallationId] = useState('');
  const [repositoryId, setRepositoryId] = useState('');
  const [name, setName] = useState('');
  const [branch, setBranch] = useState('');
  const [loadingInstallations, setLoadingInstallations] = useState(true);
  const [loadingRepositories, setLoadingRepositories] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getGitHubInstallations()
      .then((items) => {
        if (cancelled) return;
        setInstallations(items);
        if (items.length === 1) setInstallationId(String(items[0].id));
      })
      .catch((reason: unknown) => { if (!cancelled) setError(reason instanceof Error ? reason.message : 'GitHub-installationerna kunde inte hämtas.'); })
      .finally(() => { if (!cancelled) setLoadingInstallations(false); });
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (!installationId) {
      setRepositories([]);
      setRepositoryId('');
      return;
    }
    let cancelled = false;
    setLoadingRepositories(true);
    setError(null);
    setRepositories([]);
    setRepositoryId('');
    getInstallationRepositories(Number(installationId))
      .then((items) => { if (!cancelled) setRepositories(items); })
      .catch((reason: unknown) => { if (!cancelled) setError(reason instanceof Error ? reason.message : 'Repositorylistan kunde inte hämtas.'); })
      .finally(() => { if (!cancelled) setLoadingRepositories(false); });
    return () => { cancelled = true; };
  }, [installationId]);

  const selectedRepository = useMemo(
    () => repositories.find((repository) => String(repository.id) === repositoryId) ?? null,
    [repositories, repositoryId],
  );

  function selectRepository(value: string) {
    setRepositoryId(value);
    const repository = repositories.find((item) => String(item.id) === value);
    if (!repository) return;
    setBranch(repository.defaultBranch);
    if (!name.trim()) setName(repository.fullName.split('/').at(-1) ?? repository.fullName);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!installationId || !repositoryId || !name.trim() || !branch.trim()) return;
    setSaving(true);
    setError(null);
    try {
      const project = await createProject({
        name: name.trim(),
        githubInstallationId: Number(installationId),
        githubRepositoryId: Number(repositoryId),
        defaultBranch: branch.trim(),
      });
      navigate(`/projects/${project.id}`);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Projektet kunde inte skapas.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="page-card" aria-labelledby="create-project-heading">
      <p><Link className="back-link" to="/projects">← Dina projekt</Link></p>
      <p className="eyebrow">GitHub-koppling</p>
      <h1 id="create-project-heading">Skapa projekt</h1>
      <p className="lead">Välj den GitHub App-installation och det repository som ZIP-importerna ska jämföras och levereras till.</p>

      {error && <p className="status-message status-message--error" role="alert">{error}</p>}
      {loadingInstallations && <p role="status">Hämtar GitHub-installationer…</p>}

      {!loadingInstallations && installations.length === 0 && (
        <div className="empty-state">
          <h2>Ingen GitHub App-installation hittades</h2>
          <p>Installera zip-github GitHub App på minst ett repository och ladda sedan om sidan.</p>
          <a className="button" href="https://github.com/settings/installations" target="_blank" rel="noreferrer">Hantera GitHub Apps</a>
        </div>
      )}

      {!loadingInstallations && installations.length > 0 && (
        <form className="import-form" onSubmit={submit}>
          <label htmlFor="github-installation">GitHub-installation</label>
          <select id="github-installation" value={installationId} onChange={(event) => setInstallationId(event.target.value)} required>
            <option value="">Välj installation</option>
            {installations.map((installation) => (
              <option key={installation.id} value={installation.id}>{installation.accountLogin} ({installation.accountType})</option>
            ))}
          </select>

          <label htmlFor="github-repository">Repository</label>
          <select
            id="github-repository"
            value={repositoryId}
            onChange={(event) => selectRepository(event.target.value)}
            disabled={!installationId || loadingRepositories}
            required
          >
            <option value="">{loadingRepositories ? 'Hämtar repositories…' : 'Välj repository'}</option>
            {repositories.map((repository) => (
              <option key={repository.id} value={repository.id}>{repository.fullName}{repository.privateRepository ? ' (privat)' : ''}</option>
            ))}
          </select>
          {installationId && !loadingRepositories && repositories.length === 0 && <p className="field-help">Installationen har inga repositories som är tillgängliga för ditt konto.</p>}

          <label htmlFor="project-name">Projektnamn</label>
          <input id="project-name" value={name} onChange={(event) => setName(event.target.value)} maxLength={120} required />

          <label htmlFor="default-branch">Standardbranch</label>
          <input id="default-branch" value={branch} onChange={(event) => setBranch(event.target.value)} required />
          <p className="field-help">Förvald från repositoryt. Backend verifierar att branchen finns innan projektet skapas.</p>

          {selectedRepository && (
            <p className="selected-file">
              Repository: <a href={selectedRepository.htmlUrl} target="_blank" rel="noreferrer">{selectedRepository.fullName}</a>
            </p>
          )}

          <button className="button" type="submit" disabled={saving || !installationId || !repositoryId || !name.trim() || !branch.trim()}>
            {saving ? 'Skapar projekt…' : 'Skapa projekt'}
          </button>
        </form>
      )}
    </section>
  );
}
