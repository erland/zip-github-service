import type { RepositoryEntry } from '../api/repositories';

const RECENT_REPOSITORIES_KEY = 'zipgithub.recent-repositories';
const MAX_RECENT_REPOSITORIES = 5;

export function repositoryKey(repository: RepositoryEntry) {
  return `${repository.githubInstallationId}:${repository.githubRepositoryId}`;
}

export function getRecentRepositoryKeys(): string[] {
  try {
    const raw = localStorage.getItem(RECENT_REPOSITORIES_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((value): value is string => typeof value === 'string').slice(0, MAX_RECENT_REPOSITORIES);
  } catch {
    return [];
  }
}

export function markRepositoryRecent(repository: RepositoryEntry) {
  const key = repositoryKey(repository);
  const next = [key, ...getRecentRepositoryKeys().filter((item) => item !== key)].slice(0, MAX_RECENT_REPOSITORIES);
  try {
    localStorage.setItem(RECENT_REPOSITORIES_KEY, JSON.stringify(next));
  } catch {
    // Recency is only a convenience. Repository selection must still work when storage is unavailable.
  }
}

export function recentRepositories(repositories: RepositoryEntry[]) {
  const byKey = new Map(repositories.map((repository) => [repositoryKey(repository), repository]));
  return getRecentRepositoryKeys().map((key) => byKey.get(key)).filter((value): value is RepositoryEntry => Boolean(value));
}
