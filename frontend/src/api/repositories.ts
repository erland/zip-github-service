import type { ProjectResponse, WorkSessionResponse } from './projects';

export type RepositoryEntry = {
  githubInstallationId: number;
  githubRepositoryId: number;
  repositoryFullName: string;
  repositoryName: string;
  privateRepository: boolean;
  defaultBranch: string;
  htmlUrl: string;
  projectId: string | null;
};

export type RepositoryWorkResponse = {
  project: ProjectResponse;
  work: WorkSessionResponse;
};

export async function getRepositories(): Promise<RepositoryEntry[]> {
  return requestJson('/api/repositories');
}

export async function startRepositoryWork(installationId: number, repositoryId: number): Promise<RepositoryWorkResponse> {
  return requestJson(`/api/repositories/${installationId}/${repositoryId}/work`, { method: 'POST' });
}

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, { credentials: 'include', ...init });
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
