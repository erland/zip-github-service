package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ImportPlanApprovalResponse(
        UUID importId,
        UUID planId,
        String planDigestSha256,
        String selectionDigestSha256,
        String commitMessage,
        String status,
        Instant approvedAt) { }
