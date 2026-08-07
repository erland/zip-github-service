export type GitHubInstallation = {
  id: number;
  accountId: number;
  accountLogin: string;
  accountType: string;
  repositorySelection: string;
  htmlUrl: string;
};

export type GitHubRepository = {
  id: number;
  fullName: string;
  privateRepository: boolean;
  defaultBranch: string;
  htmlUrl: string;
};

export async function getGitHubInstallations(): Promise<GitHubInstallation[]> {
  return requestJson('/api/github/installations');
}

export async function getInstallationRepositories(installationId: number): Promise<GitHubRepository[]> {
  return requestJson(`/api/github/installations/${installationId}/repositories`);
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
