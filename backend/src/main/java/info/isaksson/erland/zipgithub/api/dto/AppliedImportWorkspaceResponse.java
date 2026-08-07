package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AppliedImportWorkspaceResponse(
        UUID importId,
        String repositoryFullName,
        String baseCommitSha,
        String planDigestSha256,
        int appliedFileCount,
        List<String> appliedPaths,
        String status,
        Instant preparedAt) { }
