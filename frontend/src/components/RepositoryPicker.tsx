import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import type { RepositoryEntry } from '../api/repositories';
import { markRepositoryRecent, recentRepositories, repositoryKey } from '../repositories/recentRepositories';

type RepositoryPickerProps = {
  repositories: RepositoryEntry[];
  mode: 'navigate' | 'select';
  selectedRepositoryKey?: string;
  onSelect?: (repository: RepositoryEntry) => void;
  getTarget?: (repository: RepositoryEntry) => string;
};

export default function RepositoryPicker({ repositories, mode, selectedRepositoryKey = '', onSelect, getTarget }: RepositoryPickerProps) {
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

    {!normalizedQuery && recent.length > 0 && <section className="repository-picker-recent" aria-labelledby="recent-repositories-heading">
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

    <div className="repository-picker-all-heading"><h2>{normalizedQuery ? 'Sökresultat' : 'Alla repositories'}</h2><span>{filtered.length}</span></div>
    {filtered.length === 0 ? <p className="empty-filter-result">Inga repositories matchar “{query.trim()}”.</p> : <div className="repository-picker-scroll" tabIndex={0} aria-label="Repositorylista">
      {mode === 'navigate' ? <ul className="repository-list">
        {filtered.map((repository) => <li key={repositoryKey(repository)}>
          <Link className="repository-list-link" to={getTarget?.(repository) ?? '#'} onClick={() => activate(repository)}>
            <strong>{repository.repositoryName}</strong>{(duplicateNames.get(repository.repositoryName.toLowerCase()) ?? 0) > 1 && <span>{repository.repositoryFullName}</span>}
          </Link>
        </li>)}
      </ul> : <fieldset className="choice-list repository-choice-list"><legend className="sr-only">Repository</legend>
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
