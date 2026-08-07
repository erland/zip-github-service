package info.isaksson.erland.zipgithub.actions;

import info.isaksson.erland.zipgithub.github.GitHubActionsDetailsClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportActionsDetails(UUID importId, String repositoryFullName, String commitSha, String detailsUrl,
                                   List<GitHubActionsDetailsClient.Artifact> artifacts,
                                   List<GitHubActionsDetailsClient.FailureExcerpt> failures, Instant checkedAt) {}
