package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ImportResponse(UUID id, UUID projectId, String baseBranch, String status, Instant createdAt) {}
