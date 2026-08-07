export type ProjectResponse = {
  id: string;
  name: string;
  githubInstallationId: number;
  githubRepositoryId: number;
  repositoryFullName: string;
  privateRepository: boolean;
  defaultBranch: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ImportHistoryItem = {
  id: string;
  projectId: string;
  baseBranch: string;
  status: string;
  createdAt: string;
  sourceFilename: string | null;
  sourceSizeBytes: number | null;
  planDigestSha256: string | null;
  pullRequestNumber: number | null;
  pullRequestUrl: string | null;
  resumeStage: 'UPLOAD' | 'REVIEW' | 'RESULT';
};

export async function getProject(projectId: string): Promise<ProjectResponse> {
  return requestJson(`/api/projects/${encodeURIComponent(projectId)}`);
}

export async function getProjectImports(projectId: string): Promise<ImportHistoryItem[]> {
  return requestJson(`/api/projects/${encodeURIComponent(projectId)}/imports`);
}

async function requestJson<T>(url: string): Promise<T> {
  const response = await fetch(url, { credentials: 'include' });
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
