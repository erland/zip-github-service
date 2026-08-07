package info.isaksson.erland.zipgithub.api.dto;
import java.util.UUID;
public record AuthenticatedUserResponse(UUID id, long githubUserId, String login, String avatarUrl, String gitName, String gitEmail) {}
