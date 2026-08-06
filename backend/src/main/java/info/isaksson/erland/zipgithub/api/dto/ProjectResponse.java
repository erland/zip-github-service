package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(UUID id, String name, long githubInstallationId, long githubRepositoryId,
                              String repositoryFullName, boolean privateRepository, String defaultBranch,
                              boolean active, Instant createdAt, Instant updatedAt) {}
