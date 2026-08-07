package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PullRequestResponse(UUID importId, String repositoryFullName, String baseBranch, String branchName,
                                  String commitSha, String planDigestSha256, long pullRequestNumber,
                                  String pullRequestUrl, boolean draft, String state, String status, Instant createdAt) { }
