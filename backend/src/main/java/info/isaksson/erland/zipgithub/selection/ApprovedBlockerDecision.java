package info.isaksson.erland.zipgithub.selection;

import java.util.Objects;

/** Explicit user decision for a blocking review entry. */
public record ApprovedBlockerDecision(String path, String blockerType, String decision) {
    public ApprovedBlockerDecision {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path is required");
        if (!Objects.equals(blockerType, "HARD_BLOCKED") && !Objects.equals(blockerType, "OVERRIDABLE_BLOCKED")) {
            throw new IllegalArgumentException("blockerType must describe a blocking entry");
        }
        if (!Objects.equals(decision, "EXCLUDE")
                && !Objects.equals(decision, "INCLUDE_OVERRIDE")
                && !Objects.equals(decision, "ACKNOWLEDGE_EXCLUSION")) {
            throw new IllegalArgumentException("invalid blocker decision");
        }
    }
}
