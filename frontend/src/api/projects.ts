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

export type CreateProjectInput = {
  name: string;
  githubInstallationId: number;
  githubRepositoryId: number;
  defaultBranch: string;
};

export async function getProjects(): Promise<ProjectResponse[]> {
  return requestJson('/api/projects');
}

export async function getProject(projectId: string): Promise<ProjectResponse> {
  return requestJson(`/api/projects/${encodeURIComponent(projectId)}`);
}

export async function getProjectImports(projectId: string): Promise<ImportHistoryItem[]> {
  return requestJson(`/api/projects/${encodeURIComponent(projectId)}/imports`);
}

export async function createProject(input: CreateProjectInput): Promise<ProjectResponse> {
  return requestJson('/api/projects', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Zip-GitHub-Request': '1',
    },
    body: JSON.stringify(input),
  });
}

export type WorkSessionResponse = {
  id: string;
  projectId: string;
  baseBranch: string;
  branchName: string;
  status: string;
  headCommitSha: string | null;
  pullRequestNumber: number | null;
  pullRequestUrl: string | null;
  createdAt: string;
  updatedAt: string;
};

export async function getProjectWork(projectId: string): Promise<WorkSessionResponse | null> {
  const response = await fetch(`/api/projects/${encodeURIComponent(projectId)}/work`, { credentials: 'include' });
  if (response.status === 204) return null;
  if (!response.ok) throw new Error(`API-fel ${response.status}`);
  return response.json() as Promise<WorkSessionResponse>;
}

export async function createWorkPullRequest(projectId: string) {
  return requestJson<import('./imports').PullRequestResponse>(`/api/projects/${encodeURIComponent(projectId)}/work/pull-request`, {
    method: 'POST', headers: { 'X-Zip-GitHub-Request': '1' },
  });
}

async function requestJson<T>(url: string, init: RequestInit = {}): Promise<T> {
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
