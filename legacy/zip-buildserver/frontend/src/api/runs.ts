import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './client';
import type { CreateRunRequest, RunListResponse, RunSummary, VerificationRun } from './types';

export interface CreateRunInput extends CreateRunRequest {
  sessionId: string;
}

export function createRun({ sessionId, packageId, requestedPlanId }: CreateRunInput): Promise<VerificationRun> {
  return apiRequest<VerificationRun>(`/sessions/${sessionId}/runs`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ packageId, requestedPlanId }),
  });
}

export function getRun(runId: string): Promise<VerificationRun> {
  return apiRequest<VerificationRun>(`/runs/${runId}`);
}

export function getRunSummary(runId: string): Promise<RunSummary> {
  return apiRequest<RunSummary>(`/runs/${runId}/summary`);
}

export function listSessionRuns(sessionId: string): Promise<RunListResponse> {
  return apiRequest<RunListResponse>(`/sessions/${sessionId}/runs`);
}

export function useCreateRun() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createRun,
    onSuccess: (run) => {
      queryClient.invalidateQueries({ queryKey: ['session-runs', run.sessionId] });
      queryClient.setQueryData(['run', run.id], run);
    },
  });
}

export function useRun(runId: string | undefined) {
  return useQuery({
    queryKey: ['run', runId],
    queryFn: () => getRun(runId!),
    enabled: Boolean(runId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'QUEUED' || status === 'RUNNING' ? 2500 : false;
    },
  });
}

export function useRunSummary(runId: string | undefined) {
  return useQuery({
    queryKey: ['run-summary', runId],
    queryFn: () => getRunSummary(runId!),
    enabled: Boolean(runId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'QUEUED' || status === 'RUNNING' ? 2500 : false;
    },
  });
}

export function useSessionRuns(sessionId: string | undefined) {
  return useQuery({
    queryKey: ['session-runs', sessionId],
    queryFn: () => listSessionRuns(sessionId!),
    enabled: Boolean(sessionId),
  });
}
