package info.isaksson.erland.zipgithub.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A user-owned configuration that binds zip-github to one GitHub repository. */
public record Project(
        UUID id,
        UUID ownerUserId,
        String name,
        Long githubInstallationId,
        Long githubRepositoryId,
        String repositoryOwner,
        String repositoryName,
        String defaultBranch,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public Project {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        name = requireText(name, "name");
        defaultBranch = requireText(defaultBranch, "defaultBranch");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if ((githubRepositoryId == null) != (githubInstallationId == null)) {
            throw new IllegalArgumentException("GitHub repository and installation must be configured together");
        }
        if (githubRepositoryId != null) {
            repositoryOwner = requireText(repositoryOwner, "repositoryOwner");
            repositoryName = requireText(repositoryName, "repositoryName");
        }
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerUserId.equals(userId);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
