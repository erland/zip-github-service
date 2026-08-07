export type ImportResponse = {
  id: string;
  projectId: string;
  baseBranch: string;
  status: string;
  createdAt: string;
};

export type SourceUploadResponse = {
  id: string;
  importId: string;
  originalFilename: string;
  sizeBytes: number;
  sha256: string;
  status: string;
  createdAt: string;
  retentionDeadline: string;
};

export type RepositorySnapshotResponse = {
  importId: string;
  repositoryFullName: string;
  branch: string;
  baseCommitSha: string;
  entryCount: number;
  createdAt: string;
};

export type ImportPlanEntry = {
  path: string;
  status: 'ADDED' | 'MODIFIED' | 'UNCHANGED' | 'IGNORED' | 'BLOCKED';
  comparisonStatus: string | null;
  severity: 'NONE' | 'WARNING' | 'BLOCKING';
  policyCode: string | null;
  message: string | null;
  archiveSizeBytes: number | null;
  archiveSha256: string | null;
  repositorySizeBytes: number | null;
  repositorySha256: string | null;
  textCandidate: boolean;
};


export type ImportPlanApprovalResponse = {
  importId: string;
  planId: string;
  planDigestSha256: string;
  status: 'APPROVED';
  approvedAt: string;
};

export type ImportPlanResponse = {
  id: string;
  importId: string;
  sourceUploadSha256: string;
  baseCommitSha: string;
  policyVersion: string;
  planDigestSha256: string;
  status: string;
  approvable: boolean;
  added: number;
  modified: number;
  unchanged: number;
  ignored: number;
  blocked: number;
  warnings: number;
  entries: ImportPlanEntry[];
  createdAt: string;
};

export async function createImport(projectId: string, baseBranch: string): Promise<ImportResponse> {
  return requestJson(`/api/projects/${encodeURIComponent(projectId)}/imports`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ baseBranch }),
  });
}

export async function createRepositorySnapshot(importId: string): Promise<RepositorySnapshotResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/repository-snapshot`, { method: 'POST' });
}

export async function createImportPlan(importId: string): Promise<ImportPlanResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/plan`, { method: 'POST' });
}

export async function getImportPlan(importId: string): Promise<ImportPlanResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/plan`);
}

export async function approveImportPlan(importId: string, planDigestSha256: string): Promise<ImportPlanApprovalResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/plan/approval`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ planDigestSha256 }),
  });
}

export function uploadZip(
  importId: string,
  file: File,
  onProgress: (percent: number) => void,
  signal?: AbortSignal,
): Promise<SourceUploadResponse> {
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest();
    request.open('PUT', `/api/imports/${encodeURIComponent(importId)}/upload`);
    request.withCredentials = true;
    request.setRequestHeader('Content-Type', file.type || 'application/zip');
    request.setRequestHeader('X-Filename', file.name);
    request.setRequestHeader('X-Zip-GitHub-Request', '1');
    request.upload.onprogress = (event) => {
      if (event.lengthComputable && event.total > 0) onProgress(Math.round((event.loaded / event.total) * 100));
    };
    request.onload = () => {
      if (request.status >= 200 && request.status < 300) {
        onProgress(100);
        resolve(JSON.parse(request.responseText) as SourceUploadResponse);
      } else {
        reject(new Error(readXhrError(request)));
      }
    };
    request.onerror = () => reject(new Error('Uppladdningen kunde inte genomföras.'));
    request.onabort = () => reject(new DOMException('Uppladdningen avbröts.', 'AbortError'));
    signal?.addEventListener('abort', () => request.abort(), { once: true });
    request.send(file);
  });
}

async function requestJson<T>(url: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.method && !['GET', 'HEAD'].includes(init.method.toUpperCase())) headers.set('X-Zip-GitHub-Request', '1');
  const response = await fetch(url, { ...init, headers, credentials: 'include' });
  if (!response.ok) throw await apiError(response);
  return response.json() as Promise<T>;
}

async function apiError(response: Response): Promise<Error> {
  try {
    const problem = await response.json() as { detail?: string; title?: string };
    return new Error(problem.detail || problem.title || `API-fel ${response.status}`);
  } catch {
    return new Error(`API-fel ${response.status}`);
  }
}

function readXhrError(request: XMLHttpRequest): string {
  try {
    const problem = JSON.parse(request.responseText) as { detail?: string; title?: string };
    return problem.detail || problem.title || `API-fel ${request.status}`;
  } catch {
    return `API-fel ${request.status}`;
  }
}

export type AppliedImportWorkspaceResponse = {
  importId: string;
  repositoryFullName: string;
  baseCommitSha: string;
  planDigestSha256: string;
  appliedFileCount: number;
  appliedPaths: string[];
  status: 'FILES_APPLIED';
  preparedAt: string;
};

export type GitDeliveryResponse = {
  importId: string;
  repositoryFullName: string;
  baseBranch: string;
  branchName: string;
  baseCommitSha: string;
  commitSha: string;
  planDigestSha256: string;
  status: 'PUSHED';
  pushedAt: string;
};

export type PullRequestResponse = {
  importId: string;
  repositoryFullName: string;
  baseBranch: string;
  branchName: string;
  commitSha: string;
  planDigestSha256: string;
  pullRequestNumber: number;
  pullRequestUrl: string;
  draft: boolean;
  state: string;
  status: 'PULL_REQUEST_CREATED';
  createdAt: string;
};

export async function prepareImportWorkspace(importId: string): Promise<AppliedImportWorkspaceResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/workspace`, { method: 'POST' });
}

export async function deliverImport(importId: string): Promise<GitDeliveryResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/delivery`, { method: 'POST' });
}

export async function createPullRequest(importId: string): Promise<PullRequestResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/pull-request`, { method: 'POST' });
}

export async function getPullRequest(importId: string): Promise<PullRequestResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/pull-request`);
}

export type ImportCheckState = 'pending' | 'success' | 'failure' | 'cancelled' | 'unavailable';

export interface ImportCheckStatusResponse {
  importId: string;
  repositoryFullName: string;
  commitSha: string;
  state: ImportCheckState;
  terminal: boolean;
  total: number;
  pending: number;
  successful: number;
  failed: number;
  cancelled: number;
  detailsUrl: string;
  checkedAt: string;
}

export async function getImportChecks(importId: string): Promise<ImportCheckStatusResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/checks`, { credentials: 'include' });
}

export async function getImport(importId: string): Promise<ImportResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}`);
}
