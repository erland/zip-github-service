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
  sourceType?: 'WEB_UPLOAD' | 'STORED_UPLOAD' | 'STAGING_IMPORT';
  sourceReference?: string | null;
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
  remoteHeadCommitSha: string | null;
  branchChangedExternally: boolean;
  lastImportId: string | null;
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


export type WorkCommit = {
  sha: string;
  message: string;
  authorName: string;
  authorEmail: string;
  authoredAt: string;
  htmlUrl: string | null;
  fallback: boolean;
};

export type WorkHistoryResponse = {
  commits: WorkCommit[];
  githubAvailable: boolean;
};

export async function getProjectWorkCommits(projectId: string): Promise<WorkHistoryResponse> {
  return requestJson(`/api/projects/${encodeURIComponent(projectId)}/work/commits`);
}

export async function createWorkPullRequest(projectId: string) {
  return requestJson<import('./imports').PullRequestResponse>(`/api/projects/${encodeURIComponent(projectId)}/work/pull-request`, {
    method: 'POST', headers: { 'X-Zip-GitHub-Request': '1' },
  });
}

export type WorkBranch = { name: string; commitSha: string };

export async function getAvailableWorkBranches(projectId: string): Promise<WorkBranch[]> {
  return requestJson(`/api/projects/${encodeURIComponent(projectId)}/work/branches`);
}

export async function startProjectWork(projectId: string, existingBranch?: string): Promise<WorkSessionResponse> {
  return requestJson(`/api/projects/${encodeURIComponent(projectId)}/work`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', 'X-Zip-GitHub-Request': '1' },
    body: JSON.stringify({ existingBranch: existingBranch || null }),
  });
}

export async function abandonProjectWork(projectId: string, deleteBranch: boolean): Promise<WorkSessionResponse> {
  return requestJson(`/api/projects/${encodeURIComponent(projectId)}/work/abandon`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', 'X-Zip-GitHub-Request': '1' },
    body: JSON.stringify({ deleteBranch }),
  });
}

export async function archiveProject(projectId: string): Promise<void> {
  const response = await fetch(`/api/projects/${encodeURIComponent(projectId)}`, {
    method: 'DELETE', credentials: 'include', headers: { 'X-Zip-GitHub-Request': '1' },
  });
  if (!response.ok) throw new Error(`API-fel ${response.status}`);
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

export async function getProjectWorkActions(projectId: string): Promise<import('./imports').ImportActionsStatusResponse> {
  return requestJson(`/api/projects/${encodeURIComponent(projectId)}/work/actions`);
}

export async function getProjectWorkActionDetails(projectId: string): Promise<import('./imports').ImportActionsDetailsResponse> {
  return requestJson(`/api/projects/${encodeURIComponent(projectId)}/work/actions/details`);
}
