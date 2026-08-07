package info.isaksson.erland.zipgithub.policy;

import info.isaksson.erland.zipgithub.comparison.ImportFileStatus;

public record ImportPolicyEntry(
        String path,
        ImportFileStatus status,
        ImportFileStatus comparisonStatus,
        ImportPolicySeverity severity,
        String policyCode,
        String message,
        Long archiveSizeBytes,
        String archiveSha256,
        Long repositorySizeBytes,
        String repositorySha256) {
}
