import type { RepositoryEntry } from '../api/repositories';
import { getRecentRepositoryKeys, repositoryKey } from './recentRepositories';

export type RepositorySuggestion = {
  repository: RepositoryEntry;
  confidence: 'high' | 'medium';
  reason: string;
  score: number;
};

const GENERIC_REPO_PREFIXES = ['roman', 'bradspel', 'pwa'];
const TRAILING_NOISE = /(?:[-_.](?:r\d{2,}|v?\d+(?:\.\d+){1,3}|rc[.-]?\d+|20\d{2}[-_.]?\d{2}[-_.]?\d{2}|release|preview|repo[-_.]?cleanup|cleanup|fix|updated|update))+$/i;

export function suggestRepository(filename: string, repositories: RepositoryEntry[]): RepositorySuggestion | null {
  if (repositories.length === 0) return null;
  const fileKey = stableName(filename);
  if (!fileKey) return null;
  const recentKeys = getRecentRepositoryKeys();
  const recentIndex = new Map(recentKeys.map((key, index) => [key, index]));
  const ranked = repositories.map((repository) => scoreRepository(fileKey, repository, recentIndex.get(repositoryKey(repository))))
    .sort((a, b) => b.score - a.score);
  const best = ranked[0];
  const second = ranked[1];
  if (!best || best.score < 60) return null;
  if (second && best.score - second.score < 12) return null;
  return {
    repository: best.repository,
    confidence: best.score >= 82 ? 'high' : 'medium',
    reason: best.reason,
    score: best.score,
  };
}

function scoreRepository(fileKey: string, repository: RepositoryEntry, recentPosition?: number) {
  const repoKey = stableName(repository.repositoryName);
  const fullRepoKey = stableName(repository.repositoryFullName.split('/').pop() ?? repository.repositoryFullName);
  const previousKey = stableName(repository.lastSourceFilename ?? '');
  let score = 0;
  let reason = '';

  const direct = similarityScore(fileKey, repoKey || fullRepoKey);
  if (direct >= 90) { score += direct; reason = 'Filnamnet matchar repositorynamnet.'; }
  else if (direct >= 65) { score += direct; reason = 'Filnamnet liknar repositorynamnet.'; }

  if (previousKey) {
    const previous = similarityScore(fileKey, previousKey);
    if (previous >= 95) { score += 90; reason = 'Filnamnet matchar en tidigare uppladdning till repositoryt.'; }
    else if (previous >= 75) { score += 38; reason = 'Filnamnet har samma stabila prefix som en tidigare uppladdning.'; }
  }

  if (recentPosition !== undefined) score += Math.max(1, 8 - recentPosition * 2);
  if (repository.lastUsedAt) {
    const ageDays = (Date.now() - new Date(repository.lastUsedAt).getTime()) / 86_400_000;
    if (Number.isFinite(ageDays) && ageDays >= 0) score += ageDays <= 7 ? 5 : ageDays <= 30 ? 2 : 0;
  }
  return { repository, score, reason: reason || 'Repositoryt har använts nyligen.' };
}

export function stableName(value: string) {
  let normalized = value.toLowerCase().replace(/\.zip$/i, '');
  normalized = normalized
    .replace(/[-_.](?:r\d{2,}|v?\d+(?:\.\d+){1,3}|rc[.-]?\d+|20\d{2}[-_.]?\d{2}[-_.]?\d{2})(?=$|[-_.])/gi, '')
    .replace(/[-_.](?:release|preview|repo[-_.]?cleanup|cleanup|fix|updated|update)(?=$|[-_.])/gi, '');
  normalized = normalized.replace(/[^a-z0-9åäö]+/g, '-').replace(/^-+|-+$/g, '');
  let previous = '';
  while (normalized !== previous) {
    previous = normalized;
    normalized = normalized.replace(TRAILING_NOISE, '').replace(/-+$/g, '');
  }
  for (const prefix of GENERIC_REPO_PREFIXES) {
    if (normalized.startsWith(`${prefix}-`) && normalized.length > prefix.length + 3) {
      normalized = normalized.slice(prefix.length + 1);
      break;
    }
  }
  return normalized;
}

function similarityScore(left: string, right: string) {
  if (!left || !right) return 0;
  if (left === right) return 100;
  if (left.startsWith(right) || right.startsWith(left)) return 88;
  if (left.includes(right) || right.includes(left)) return 76;
  const leftParts = new Set(left.split('-').filter(Boolean));
  const rightParts = new Set(right.split('-').filter(Boolean));
  const overlap = [...leftParts].filter((part) => rightParts.has(part)).length;
  const union = new Set([...leftParts, ...rightParts]).size;
  return union ? Math.round((overlap / union) * 70) : 0;
}
