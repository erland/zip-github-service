import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import type { RepositoryEntry } from '../api/repositories';
import { markRepositoryRecent, recentRepositories, repositoryKey } from '../repositories/recentRepositories';

export type RepositoryPickerGroup = {
  id: string;
  heading: string;
  repositories: RepositoryEntry[];
};

type RepositoryPickerProps = {
  repositories: RepositoryEntry[];
  mode: 'navigate' | 'select';
  groups?: RepositoryPickerGroup[];
  getStatusLabel?: (repository: RepositoryEntry) => string | null;
  selectedRepositoryKey?: string;
  onSelect?: (repository: RepositoryEntry) => void;
  getTarget?: (repository: RepositoryEntry) => string;
};

export default function RepositoryPicker({ repositories, mode, groups, getStatusLabel, selectedRepositoryKey = '', onSelect, getTarget }: RepositoryPickerProps) {
  const [query, setQuery] = useState('');
  const normalizedQuery = query.trim().toLowerCase();
  const filtered = useMemo(() => {
    if (!normalizedQuery) return repositories;
    return repositories.filter((repository) => repository.repositoryName.toLowerCase().includes(normalizedQuery)
      || repository.repositoryFullName.toLowerCase().includes(normalizedQuery));
  }, [repositories, normalizedQuery]);
  const recent = useMemo(() => recentRepositories(repositories), [repositories]);
  const duplicateNames = useMemo(() => {
    const counts = new Map<string, number>();
    repositories.forEach((repository) => counts.set(repository.repositoryName.toLowerCase(), (counts.get(repository.repositoryName.toLowerCase()) ?? 0) + 1));
    return counts;
  }, [repositories]);

  function activate(repository: RepositoryEntry) {
    if (mode === 'navigate') markRepositoryRecent(repository);
    onSelect?.(repository);
  }

  return <div className="repository-picker">
    <label className="repository-search" htmlFor="repository-filter">
      <span>Sök repositories</span>
      <input id="repository-filter" type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Skriv en del av namnet…" autoComplete="off" />
    </label>

    {!normalizedQuery && !groups && recent.length > 0 && <section className="repository-picker-recent" aria-labelledby="recent-repositories-heading">
      <h2 id="recent-repositories-heading">Senast använda</h2>
      <div className="repository-picker-recent-list">
        {recent.map((repository) => mode === 'navigate'
          ? <Link key={repositoryKey(repository)} className="repository-recent-link" to={getTarget?.(repository) ?? '#'} onClick={() => activate(repository)}>
              <strong>{repository.repositoryName}</strong><span>{repository.repositoryFullName}</span>
            </Link>
          : <button key={repositoryKey(repository)} className={`repository-recent-button${selectedRepositoryKey === repositoryKey(repository) ? ' is-selected' : ''}`} type="button" onClick={() => activate(repository)}>
              <strong>{repository.repositoryName}</strong><span>{repository.repositoryFullName}</span>
            </button>)}
      </div>
    </section>}

    <div className="repository-picker-all-heading"><h2>{normalizedQuery ? 'Sökresultat' : groups ? 'Repositoryöversikt' : 'Alla repositories'}</h2><span>{filtered.length}</span></div>
    {filtered.length === 0 ? <p className="empty-filter-result">Inga repositories matchar “{query.trim()}”.</p> : <div className="repository-picker-scroll" tabIndex={0} aria-label="Repositorylista">
      {mode === 'navigate' ? normalizedQuery || !groups ? <ul className="repository-list">
        {filtered.map((repository) => <RepositoryNavigationItem key={repositoryKey(repository)} repository={repository} duplicateNames={duplicateNames} getTarget={getTarget} getStatusLabel={getStatusLabel} activate={activate} />)}
      </ul> : groups.map((group) => group.repositories.length > 0 && <section key={group.id} className="repository-attention-group" aria-labelledby={`repository-group-${group.id}`}>
        <div className="repository-picker-all-heading"><h3 id={`repository-group-${group.id}`}>{group.heading}</h3><span>{group.repositories.length}</span></div>
        <ul className="repository-list">
          {group.repositories.map((repository) => <RepositoryNavigationItem key={repositoryKey(repository)} repository={repository} duplicateNames={duplicateNames} getTarget={getTarget} getStatusLabel={getStatusLabel} activate={activate} />)}
        </ul>
      </section>) : <fieldset className="choice-list repository-choice-list"><legend className="sr-only">Repository</legend>
        {filtered.map((repository) => {
          const key = repositoryKey(repository);
          return <label key={key} className={`choice-row${selectedRepositoryKey === key ? ' is-selected' : ''}`}>
            <input type="radio" name="staging-repository" value={key} checked={selectedRepositoryKey === key} onChange={() => activate(repository)} />
            <span><strong>{repository.repositoryName}</strong><small>{repository.repositoryFullName}</small></span>
          </label>;
        })}
      </fieldset>}
    </div>}
  </div>;
}

function RepositoryNavigationItem({
  repository,
  duplicateNames,
  getTarget,
  getStatusLabel,
  activate,
}: {
  repository: RepositoryEntry;
  duplicateNames: Map<string, number>;
  getTarget?: (repository: RepositoryEntry) => string;
  getStatusLabel?: (repository: RepositoryEntry) => string | null;
  activate: (repository: RepositoryEntry) => void;
}) {
  const statusLabel = getStatusLabel?.(repository);
  return <li>
    <Link className="repository-list-link" to={getTarget?.(repository) ?? '#'} onClick={() => activate(repository)}>
      <strong>{repository.repositoryName}</strong>
      {(duplicateNames.get(repository.repositoryName.toLowerCase()) ?? 0) > 1 && <span>{repository.repositoryFullName}</span>}
      {statusLabel && <small>{statusLabel}</small>}
    </Link>
  </li>;
}

