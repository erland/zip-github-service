package info.isaksson.erland.zipgithub.api.dto;

import java.util.UUID;

public record StagingPromotionResponse(UUID stagingId, UUID projectId, UUID importId, String status, boolean alreadyPromoted) { }
