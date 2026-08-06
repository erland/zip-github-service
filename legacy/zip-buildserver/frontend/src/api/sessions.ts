import { useMutation, useQuery } from '@tanstack/react-query';
import { apiRequest } from './client';
import type { CreateSessionRequest, VerificationSession } from './types';

export function createSession(request: CreateSessionRequest): Promise<VerificationSession> {
  return apiRequest<VerificationSession>('/sessions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
}

export function getSession(sessionId: string): Promise<VerificationSession> {
  return apiRequest<VerificationSession>(`/sessions/${sessionId}`);
}

export function useCreateSession() {
  return useMutation({
    mutationFn: createSession,
  });
}

export function useSession(sessionId: string | undefined) {
  return useQuery({
    queryKey: ['session', sessionId],
    queryFn: () => getSession(sessionId!),
    enabled: Boolean(sessionId),
  });
}
