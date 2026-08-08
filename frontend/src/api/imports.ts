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
  blockerType: 'NONE' | 'HARD_BLOCKED' | 'OVERRIDABLE_BLOCKED';
  policyCode: string | null;
  message: string | null;
  archiveSizeBytes: number | null;
  archiveSha256: string | null;
  repositorySizeBytes: number | null;
  repositorySha256: string | null;
  archiveMode?: string | null;
  repositoryMode?: string | null;
  effectiveMode?: string | null;
  modeChanged?: boolean;
  textCandidate: boolean;
};


export type ImportPlanApprovalResponse = {
  importId: string;
  planId: string;
  planDigestSha256: string;
  selectionDigestSha256: string;
  commitMessage: string;
  status: 'APPROVED';
  approvedAt: string;
};

export type ImportSelectionResponse = {
  id: string;
  importId: string;
  planId: string;
  planDigestSha256: string;
  baseCommitSha: string;
  selectionVersion: string;
  selectionDigestSha256: string;
  selectedPaths: string[];
  excludedPaths: string[];
  overrides: Array<{ path: string; blockerType: string; policyCode: string; acknowledgement: string }>;
  createdAt: string;
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
  hardBlocked: number;
  overridableBlocked: number;
  warnings: number;
  entries: ImportPlanEntry[];
  createdAt: string;
};

export async function createImport(projectId: string, author?: { name: string; email: string }): Promise<ImportResponse> {
  return requestJson(`/api/projects/${encodeURIComponent(projectId)}/imports`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ authorName: author?.name ?? null, authorEmail: author?.email ?? null }),
  });
}

export async function createRepositorySnapshot(importId: string): Promise<RepositorySnapshotResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/repository-snapshot`, { method: 'POST' });
}

export async function createImportPlan(importId: string): Promise<ImportPlanResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/plan`, { method: 'POST' });
}

export async function prepareImportReview(importId: string): Promise<ImportPlanResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/prepare-review`, { method: 'POST' });
}

export async function getImportPlan(importId: string): Promise<ImportPlanResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/plan`);
}

export async function createImportSelection(importId: string, planDigestSha256: string, baseCommitSha: string,
  selectedPaths: string[], overridePaths: string[]): Promise<ImportSelectionResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/selection`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      planDigestSha256,
      baseCommitSha,
      selectedPaths,
      overrides: overridePaths.map((path) => ({
        path,
        acknowledgement: 'User explicitly approved this policy override in the review UI.',
      })),
    }),
  });
}

export async function approveImportPlan(importId: string, planDigestSha256: string, selectionDigestSha256: string,
  commitMessage: string): Promise<ImportPlanApprovalResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/plan/approval`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ planDigestSha256, selectionDigestSha256, commitMessage }),
  });
}

export async function getImportSelection(importId: string): Promise<ImportSelectionResponse | null> {
  return requestOptionalJson(`/api/imports/${encodeURIComponent(importId)}/selection`);
}

export async function getImportPlanApproval(importId: string): Promise<ImportPlanApprovalResponse | null> {
  return requestOptionalJson(`/api/imports/${encodeURIComponent(importId)}/plan/approval`);
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

async function requestOptionalJson<T>(url: string): Promise<T | null> {
  const response = await fetch(url, { credentials: 'include' });
  if (response.status === 404) return null;
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
  selectionDigestSha256: string;
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

export async function getDelivery(importId: string): Promise<GitDeliveryResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/delivery`);
}

export async function findDelivery(importId: string): Promise<GitDeliveryResponse | null> {
  return requestOptionalJson(`/api/imports/${encodeURIComponent(importId)}/delivery`);
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

export async function cancelImport(importId: string): Promise<ImportResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/cancel`, { method: 'POST' });
}

export type ActionsItemState = 'not_started' | 'pending' | 'success' | 'failure' | 'cancelled' | 'unavailable';

export interface ActionsJobResponse {
  id: number;
  name: string;
  state: Exclude<ActionsItemState, 'not_started' | 'unavailable'>;
  terminal: boolean;
  htmlUrl: string | null;
  startedAt: string | null;
  completedAt: string | null;
}

export interface ActionsWorkflowRunResponse {
  id: number;
  workflowId: number;
  workflowPath: string;
  headBranch: string;
  headSha: string;
  name: string;
  state: Exclude<ActionsItemState, 'not_started' | 'unavailable'>;
  terminal: boolean;
  event: string;
  htmlUrl: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  jobs: ActionsJobResponse[];
}

export interface ActionsCheckRunResponse {
  id: number;
  name: string;
  state: Exclude<ActionsItemState, 'not_started' | 'unavailable'>;
  terminal: boolean;
  htmlUrl: string | null;
  appName: string;
  startedAt: string | null;
  completedAt: string | null;
}

export interface ImportActionsStatusResponse {
  importId: string;
  repositoryFullName: string;
  commitSha: string;
  state: ActionsItemState;
  terminal: boolean;
  detailsUrl: string;
  workflows: ActionsWorkflowRunResponse[];
  checks: ActionsCheckRunResponse[];
  checkedAt: string;
}

export async function getImportActions(importId: string): Promise<ImportActionsStatusResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/actions`, { credentials: 'include' });
}

export interface ActionsArtifactResponse {
  id: number;
  name: string;
  sizeBytes: number;
  expired: boolean;
  createdAt: string | null;
  expiresAt: string | null;
  workflowRunId: number;
  workflowName: string;
  githubUrl: string;
}

export interface ActionsFailureResponse {
  workflowRunId: number;
  workflowName: string;
  jobId: number;
  jobName: string;
  stepName: string;
  tool: string;
  lines: string[];
  githubUrl: string;
}

export interface ImportActionsDetailsResponse {
  importId: string;
  repositoryFullName: string;
  commitSha: string;
  detailsUrl: string;
  artifacts: ActionsArtifactResponse[];
  failures: ActionsFailureResponse[];
  checkedAt: string;
}

export async function getImportActionDetails(importId: string): Promise<ImportActionsDetailsResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/actions/details`, { credentials: 'include' });
}

export interface ActionsControlWorkflowOption {
  identifier: string;
  workflowId: number;
  name: string;
  path: string;
  htmlUrl: string | null;
  dispatchAllowed: boolean;
  rerunAllowed: boolean;
}

export interface ImportActionsControlOptionsResponse {
  importId: string;
  repositoryFullName: string;
  branchRef: string;
  commitSha: string;
  currentWork: boolean;
  disabledReason: string | null;
  workflows: ActionsControlWorkflowOption[];
}

export interface ActionsControlOperationResponse {
  operationId: string;
  operation: 'WORKFLOW_DISPATCH' | 'RERUN_FAILED_JOBS';
  status: 'STARTED' | 'SUCCEEDED' | 'FAILED';
  replayed: boolean;
  workflowIdentifier: string;
  workflowId: number | null;
  workflowRunId: number | null;
  branchRef: string;
  targetCommitSha: string;
  githubUrl: string | null;
  errorCode: string | null;
  createdAt: string;
  updatedAt: string;
}

export async function getImportActionsControlOptions(importId: string): Promise<ImportActionsControlOptionsResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/actions/control`);
}

export async function dispatchImportWorkflow(importId: string, workflowIdentifier: string, expectedRef: string,
  expectedCommitSha: string, idempotencyKey: string): Promise<ActionsControlOperationResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/actions/dispatch`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ workflowIdentifier, expectedRef, expectedCommitSha, idempotencyKey, confirmed: true }),
  });
}

export async function rerunImportWorkflowFailedJobs(importId: string, workflowRunId: number, expectedRef: string,
  expectedCommitSha: string, idempotencyKey: string): Promise<ActionsControlOperationResponse> {
  return requestJson(`/api/imports/${encodeURIComponent(importId)}/actions/rerun-failed`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ workflowRunId, expectedRef, expectedCommitSha, idempotencyKey, confirmed: true }),
  });
}
