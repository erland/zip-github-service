package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record RepositoryEntryResponse(
        long githubInstallationId,
        long githubRepositoryId,
        String repositoryFullName,
        String repositoryName,
        boolean privateRepository,
        String defaultBranch,
        String htmlUrl,
        UUID projectId,
        String lastSourceFilename,
        Instant lastUsedAt) {}
