import { useMutation } from '@tanstack/react-query';
import { apiRequest } from './client';
import type { SourcePackage } from './types';

export interface UploadPackageInput {
  sessionId: string;
  file: File;
}

export function uploadPackage({ sessionId, file }: UploadPackageInput): Promise<SourcePackage> {
  const formData = new FormData();
  formData.append('file', file);

  return apiRequest<SourcePackage>(`/sessions/${sessionId}/packages`, {
    method: 'POST',
    body: formData,
  });
}

export function useUploadPackage() {
  return useMutation({
    mutationFn: uploadPackage,
  });
}
