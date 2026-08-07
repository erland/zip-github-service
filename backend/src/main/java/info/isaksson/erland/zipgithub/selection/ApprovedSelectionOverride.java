package info.isaksson.erland.zipgithub.selection;

import java.util.Objects;

/** Immutable audit entry proving an explicit override for one overridable policy blocker. */
public record ApprovedSelectionOverride(
        String path,
        String blockerType,
        String policyCode,
        String acknowledgement) {

    public ApprovedSelectionOverride {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(blockerType, "blockerType");
        Objects.requireNonNull(policyCode, "policyCode");
        Objects.requireNonNull(acknowledgement, "acknowledgement");
        if (path.isBlank() || path.startsWith("/") || path.contains("\\")) {
            throw new IllegalArgumentException("path must be normalized and relative");
        }
        if (!"OVERRIDABLE_BLOCKED".equals(blockerType)) {
            throw new IllegalArgumentException("only OVERRIDABLE_BLOCKED entries can be overridden");
        }
        if (policyCode.isBlank()) throw new IllegalArgumentException("policyCode is required");
        if (acknowledgement.isBlank()) throw new IllegalArgumentException("acknowledgement is required");
    }
}
