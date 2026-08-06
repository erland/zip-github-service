const defaultApiBaseUrl = '/api';

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function responseMessage(response: Response): Promise<string> {
  const fallback = `API request failed with status ${response.status}`;
  const contentType = response.headers.get('content-type') ?? '';

  if (!contentType.includes('application/json')) {
    return fallback;
  }

  try {
    const body = (await response.json()) as { message?: string; error?: string };
    return body.message ?? body.error ?? fallback;
  } catch {
    return fallback;
  }
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${defaultApiBaseUrl}${path}`, {
    headers: {
      Accept: 'application/json',
      ...init?.headers,
    },
    ...init,
  });

  if (!response.ok) {
    throw new ApiError(await responseMessage(response), response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
