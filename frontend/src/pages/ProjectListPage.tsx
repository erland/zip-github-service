import { useEffect, useState } from 'react';
import RepositoryPicker, { type RepositoryPickerGroup } from '../components/RepositoryPicker';
import { getRepositories, type RepositoryEntry } from '../api/repositories';
import { getProjectImports, getProjectWork, getProjectWorkActions } from '../api/projects';

type RepositoryOverview = {
  bucket: 'attention' | 'ongoing' | 'other';
  label: string | null;
  target: string | null;
};

export default function ProjectListPage() {
  const [repositories, setRepositories] = useState<RepositoryEntry[]>([]);
  const [overview, setOverview] = useState<Record<string, RepositoryOverview>>({});
  const [loading, setLoading] = useState(true);
  const [statusLoading, setStatusLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getRepositories()
      .then(async (items) => {
        if (cancelled) return;
        setRepositories(items);
        setStatusLoading(true);
        const entries = await Promise.all(items.map(async (repository) => {
          if (!repository.projectId) return [repositoryKey(repository), { bucket: 'other', label: null, target: null } satisfies RepositoryOverview] as const;
          try {
            const [work, imports] = await Promise.all([
              getProjectWork(repository.projectId),
              getProjectImports(repository.projectId),
            ]);
            const activeImport = imports.find((item) => item.resumeStage === 'UPLOAD' || item.resumeStage === 'REVIEW') ?? null;
            if (activeImport) {
              const target = activeImport.resumeStage === 'REVIEW'
                ? `/projects/${repository.projectId}/imports/${activeImport.id}/review`
                : `/projects/${repository.projectId}/imports/new?importId=${encodeURIComponent(activeImport.id)}`;
              return [repositoryKey(repository), { bucket: 'attention', label: activeImport.resumeStage === 'REVIEW' ? 'Import väntar på granskning' : 'Import väntar på uppladdning', target } satisfies RepositoryOverview] as const;
            }
            if (work?.branchChangedExternally) {
              return [repositoryKey(repository), { bucket: 'attention', label: 'Work-branchen har ändrats externt', target: `/projects/${repository.projectId}` } satisfies RepositoryOverview] as const;
            }
            if (work?.status === 'PR_CLOSED') {
              return [repositoryKey(repository), { bucket: 'attention', label: 'Pull request är stängd utan merge', target: `/projects/${repository.projectId}` } satisfies RepositoryOverview] as const;
            }
            if (work?.headCommitSha && work.lastImportId) {
              try {
                const actions = await getProjectWorkActions(repository.projectId);
                if (actions.state === 'failure' || actions.state === 'cancelled' || actions.state === 'unavailable') {
                  return [repositoryKey(repository), { bucket: 'attention', label: actions.state === 'unavailable' ? 'Actions-status kunde inte verifieras' : 'GitHub Actions behöver uppmärksamhet', target: `/projects/${repository.projectId}/imports/${work.lastImportId}/result` } satisfies RepositoryOverview] as const;
                }
              } catch {
                return [repositoryKey(repository), { bucket: 'attention', label: 'Actions-status kunde inte verifieras', target: `/projects/${repository.projectId}` } satisfies RepositoryOverview] as const;
              }
            }
            if (work?.status === 'ACTIVE') {
              return [repositoryKey(repository), { bucket: 'ongoing', label: 'Work pågår', target: `/projects/${repository.projectId}` } satisfies RepositoryOverview] as const;
            }
            if (work?.status === 'PR_OPEN') {
              return [repositoryKey(repository), { bucket: 'ongoing', label: 'Pull request är öppen', target: `/projects/${repository.projectId}` } satisfies RepositoryOverview] as const;
            }
            return [repositoryKey(repository), { bucket: 'other', label: work ? `Work: ${work.status}` : null, target: `/projects/${repository.projectId}` } satisfies RepositoryOverview] as const;
          } catch {
            return [repositoryKey(repository), { bucket: 'attention', label: 'Status kunde inte verifieras', target: `/projects/${repository.projectId}` } satisfies RepositoryOverview] as const;
          }
        }));
        if (!cancelled) setOverview(Object.fromEntries(entries));
      })
      .catch((reason: unknown) => { if (!cancelled) setError(reason instanceof Error ? reason.message : 'Repositorylistan kunde inte hämtas.'); })
      .finally(() => { if (!cancelled) { setLoading(false); setStatusLoading(false); } });
    return () => { cancelled = true; };
  }, []);

  const groups: RepositoryPickerGroup[] = [
    { id: 'attention', heading: 'Behöver din uppmärksamhet', repositories: repositories.filter((repository) => overview[repositoryKey(repository)]?.bucket === 'attention') },
    { id: 'ongoing', heading: 'Pågående', repositories: repositories.filter((repository) => overview[repositoryKey(repository)]?.bucket === 'ongoing') },
    { id: 'other', heading: 'Övriga repositories', repositories: repositories.filter((repository) => !overview[repositoryKey(repository)] || overview[repositoryKey(repository)]?.bucket === 'other') },
  ];

  return (
    <section className="page-card" aria-labelledby="repository-list-heading">
      <div className="page-heading-row">
        <div>
          <p className="eyebrow">GitHub repositories</p>
          <h1 id="repository-list-heading">Repositories</h1>
          <p className="lead">Repositories som behöver en åtgärd visas först, följt av pågående arbete och övriga repositories.</p>
        </div>
      </div>

      {loading && <p role="status">Hämtar repositories…</p>}
      {!loading && statusLoading && <p role="status">Kontrollerar aktuell Work-, import- och Actions-status…</p>}
      {error && <p className="status-message status-message--error" role="alert">{error}</p>}

      {!loading && !error && repositories.length === 0 && (
        <div className="empty-state">
          <h2>Inga repositories tillgängliga</h2>
          <p>Installera zip-github GitHub App på minst ett repository eller ge installationen åtkomst till fler repositories.</p>
          <a className="button" href="https://github.com/settings/installations" target="_blank" rel="noreferrer">Hantera GitHub Apps</a>
        </div>
      )}

      {!loading && !error && repositories.length > 0 && <RepositoryPicker
        repositories={repositories}
        mode="navigate"
        groups={groups}
        getStatusLabel={(repository) => overview[repositoryKey(repository)]?.label ?? null}
        getTarget={(repository) => overview[repositoryKey(repository)]?.target
          ?? (repository.projectId
            ? `/projects/${repository.projectId}`
            : `/repositories/${repository.githubInstallationId}/${repository.githubRepositoryId}`)}
      />}
    </section>
  );
}

function repositoryKey(repository: RepositoryEntry) {
  return `${repository.githubInstallationId}:${repository.githubRepositoryId}`;
}
