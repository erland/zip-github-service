import { useQuery } from '@tanstack/react-query';
import { apiRequest } from './client';
import type { ArtifactContent, ArtifactListResponse } from './types';

export function listRunArtifacts(runId: string): Promise<ArtifactListResponse> {
  return apiRequest<ArtifactListResponse>(`/runs/${runId}/artifacts`);
}

export function getArtifact(artifactId: string): Promise<ArtifactContent> {
  return apiRequest<ArtifactContent>(`/artifacts/${artifactId}`);
}

export function useRunArtifacts(runId: string | undefined) {
  return useQuery({
    queryKey: ['run-artifacts', runId],
    queryFn: () => listRunArtifacts(runId!),
    enabled: Boolean(runId),
  });
}

export function useArtifact(artifactId: string | null) {
  return useQuery({
    queryKey: ['artifact', artifactId],
    queryFn: () => getArtifact(artifactId!),
    enabled: Boolean(artifactId),
  });
}
