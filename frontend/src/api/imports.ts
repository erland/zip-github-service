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

export async function createImport(projectId: string, baseBranch: string): Promise<ImportResponse> {
  const response = await fetch(`/api/projects/${encodeURIComponent(projectId)}/imports`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ baseBranch }),
  });
  if (!response.ok) throw await apiError(response);
  return response.json();
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
