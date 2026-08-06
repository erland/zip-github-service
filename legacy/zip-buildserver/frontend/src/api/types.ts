export type RunStatus = 'QUEUED' | 'RUNNING' | 'PASSED' | 'FAILED' | 'REJECTED' | 'TIMED_OUT' | 'CANCELLED' | 'INCOMPLETE' | 'INTERNAL_ERROR';

export type SessionStatus = 'OPEN' | 'CLOSED';

export type SourcePackageStatus = 'ACCEPTED' | 'REJECTED';

export interface VerificationPlanSummary {
  id: string;
  displayName: string;
  supportedProjectIndicators: string[];
}

export interface HealthResponse {
  status: string;
  service: string;
}

export interface VerificationSession {
  id: string;
  label: string | null;
  status: SessionStatus;
  createdAt: string;
  closedAt: string | null;
  createdBy: string | null;
  retentionPolicy: string | null;
}

export interface CreateSessionRequest {
  label?: string;
  retentionPolicy?: string;
}

export interface DetectedProject {
  path: string;
  technology: string;
  buildIndicators: string[];
  selectedPlanId: string | null;
  selectionReason: string;
}

export interface ProjectDetectionSummary {
  projects: DetectedProject[];
  supported: boolean;
  summary: string;
}

export interface SourcePackage {
  id: string;
  sessionId: string;
  originalFilename: string;
  checksumSha256: string;
  compressedSizeBytes: number;
  extractedSizeBytes: number | null;
  fileCount: number | null;
  topLevelEntries: string | null;
  status: SourcePackageStatus;
  rejectionReason: string | null;
  createdAt: string;
  projectDetection: ProjectDetectionSummary | null;
}


export type CheckStatus = 'PASSED' | 'FAILED' | 'SKIPPED' | 'TIMED_OUT' | 'CANCELLED' | 'NOT_APPLICABLE' | 'INTERNAL_ERROR';

export interface RunCommand {
  id: string;
  commandLabel: string;
  workingDirectory: string;
  commandDisplay: string;
  status: CheckStatus;
  exitCode: number | null;
  startedAt: string | null;
  completedAt: string | null;
  durationMillis: number | null;
  logExcerpt: string | null;
  failureCategory: string | null;
  failureMessage: string | null;
  stdoutArtifactRef: string | null;
  stderrArtifactRef: string | null;
}

export interface VerificationRun {
  id: string;
  sessionId: string;
  sourcePackageId: string;
  status: RunStatus;
  planId: string | null;
  requestedPlanId: string | null;
  networkMode: string | null;
  summary: string | null;
  startedAt: string | null;
  completedAt: string | null;
  durationMillis: number | null;
  commands: RunCommand[];
}

export interface RunListResponse {
  runs: VerificationRun[];
}

export interface CreateRunRequest {
  packageId: string;
  requestedPlanId?: string;
}

export interface RunSummary {
  runId: string;
  status: RunStatus;
  summary: string | null;
  planId: string | null;
  primaryFailure: string | null;
  commandsRun: string[];
  suggestedFocus: string[];
  partial: boolean;
}

export interface ArtifactReference {
  id: string;
  runId: string;
  type: string;
  sizeBytes: number;
  createdAt: string;
  expiresAt: string | null;
}

export interface ArtifactListResponse {
  artifacts: ArtifactReference[];
}

export interface ArtifactContent {
  id: string;
  runId: string;
  type: string;
  content: string;
}
