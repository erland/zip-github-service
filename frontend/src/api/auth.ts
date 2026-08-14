export type AuthenticatedUser = {
  id: string;
  githubUserId: number;
  login: string;
  avatarUrl: string | null;
  gitName: string;
  gitEmail: string;
};

export async function getCurrentUser(): Promise<AuthenticatedUser | null> {
  const response = await fetch('/api/auth/me', { credentials: 'include' });
  if (response.status === 401) return null;
  if (!response.ok) throw await apiError(response);
  return response.json() as Promise<AuthenticatedUser>;
}

export async function logout(): Promise<void> {
  const response = await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include',
    headers: { 'X-Zip-GitHub-Request': '1' },
  });
  if (response.status === 401) return;
  if (!response.ok) throw await apiError(response);
}

async function apiError(response: Response): Promise<Error> {
  try {
    const problem = await response.json() as { detail?: string; title?: string };
    return new Error(problem.detail || problem.title || `API-fel ${response.status}`);
  } catch {
    return new Error(`API-fel ${response.status}`);
  }
}
