package info.isaksson.erland.zipgithub.api.dto;

public record UpdateProjectRequest(String name, Long githubInstallationId, Long githubRepositoryId,
                                   String defaultBranch, Boolean active) {}
