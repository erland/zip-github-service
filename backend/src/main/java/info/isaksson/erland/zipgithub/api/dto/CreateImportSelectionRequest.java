package info.isaksson.erland.zipgithub.api.dto;

import java.util.List;

public record CreateImportSelectionRequest(
        String planDigestSha256,
        String baseCommitSha,
        List<String> selectedPaths,
        List<Override> overrides,
        List<BlockerDecision> blockerDecisions) {
    public record Override(String path, String acknowledgement) { }
    public record BlockerDecision(String path, String decision) { }
}
