import { assertSessionActive } from './session';
export type ClaimedStagingImport = {
  stagingId: string;
  originalFilename: string;
  sizeBytes: number;
  sha256: string;
  expiresAt: string;
  claimedAt: string;
};

export type StagingPromotionResponse = {
  stagingId: string;
  projectId: string;
  importId: string;
  status: 'PROMOTED';
  alreadyPromoted: boolean;
};

export async function claimStagingImport(token: string): Promise<ClaimedStagingImport> {
  return requestJson('/api/staging-imports/claim', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Zip-GitHub-Request': '1',
    },
    body: JSON.stringify({ token }),
  });
}

export async function getClaimedStagingImport(stagingId: string): Promise<ClaimedStagingImport> {
  return requestJson(`/api/staging-imports/${encodeURIComponent(stagingId)}`);
}

export type StagingPromotionTarget =
  | { projectId: string }
  | { githubInstallationId: number; githubRepositoryId: number };

export async function promoteStagingImport(stagingId: string, target: StagingPromotionTarget, confirmOpenPullRequest = false): Promise<StagingPromotionResponse> {
  return requestJson(`/api/staging-imports/${encodeURIComponent(stagingId)}/promote`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Zip-GitHub-Request': '1',
    },
    body: JSON.stringify({ ...target, confirmOpenPullRequest }),
  });
}

async function requestJson<T>(url: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(url, { credentials: 'include', ...init });
  assertSessionActive(response);
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
