package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportSelectionResponse(
        UUID id,
        UUID importId,
        UUID planId,
        String planDigestSha256,
        String baseCommitSha,
        String selectionVersion,
        String selectionDigestSha256,
        List<String> selectedPaths,
        List<String> excludedPaths,
        List<Override> overrides,
        List<BlockerDecision> blockerDecisions,
        Instant createdAt) {
    public record Override(String path, String blockerType, String policyCode, String acknowledgement) { }
    public record BlockerDecision(String path, String blockerType, String decision) { }
}
