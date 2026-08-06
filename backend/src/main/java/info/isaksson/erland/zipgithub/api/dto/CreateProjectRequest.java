package info.isaksson.erland.zipgithub.api.dto;

public record CreateProjectRequest(String name, Long githubInstallationId, Long githubRepositoryId, String defaultBranch) {}
