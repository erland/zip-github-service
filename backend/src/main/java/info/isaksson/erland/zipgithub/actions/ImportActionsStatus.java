package info.isaksson.erland.zipgithub.actions;

import info.isaksson.erland.zipgithub.github.GitHubActionsClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportActionsStatus(UUID importId, String repositoryFullName, String commitSha, String state,
                                  boolean terminal, String detailsUrl, List<GitHubActionsClient.WorkflowRun> workflows,
                                  List<GitHubActionsClient.CheckRun> checks, Instant checkedAt) {}
