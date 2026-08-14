import { assertSessionActive } from './session';
export type WorkBranchCleanupCandidate = {
  githubInstallationId: number;
  githubRepositoryId: number;
  repositoryFullName: string;
  repositoryUrl: string | null;
  projectId: string | null;
  defaultBranch: string;
  branchName: string;
  branchUrl: string | null;
  commitSha: string;
  pullRequestNumber: number | null;
  pullRequestUrl: string | null;
  classification: string;
  reason: string;
  deletable: boolean;
};

export type WorkBranchCleanupPreview = {
  repositoriesChecked: number;
  workBranchesFound: number;
  safeToDelete: number;
  inUseOrProtected: number;
  unverifiable: number;
  candidates: WorkBranchCleanupCandidate[];
  issues: Array<{ scope: string; reason: string }>;
};

export type WorkBranchCleanupResult = {
  results: Array<{ repositoryFullName: string; branchName: string; status: string; reason: string }>;
};

export async function getWorkBranchCleanupPreview(): Promise<WorkBranchCleanupPreview> {
  return requestJson('/api/maintenance/work-branches');
}

export async function cleanupWorkBranches(candidates: WorkBranchCleanupCandidate[]): Promise<WorkBranchCleanupResult> {
  return requestJson('/api/maintenance/work-branches/cleanup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Zip-GitHub-Request': '1' },
    body: JSON.stringify({ targets: candidates.map((candidate) => ({
      githubInstallationId: candidate.githubInstallationId,
      githubRepositoryId: candidate.githubRepositoryId,
      repositoryFullName: candidate.repositoryFullName,
      branchName: candidate.branchName,
    })) }),
  });
}

async function requestJson<T>(url: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(url, { credentials: 'include', ...init });
  assertSessionActive(response);
  if (!response.ok) {
    try {
      const problem = await response.json() as { detail?: string; title?: string };
      throw new Error(problem.detail || problem.title || `API-fel ${response.status}`);
    } catch (error) {
      if (error instanceof Error && !error.message.startsWith('Unexpected')) throw error;
      throw new Error(`API-fel ${response.status}`);
    }
  }
  return response.json() as Promise<T>;
}
