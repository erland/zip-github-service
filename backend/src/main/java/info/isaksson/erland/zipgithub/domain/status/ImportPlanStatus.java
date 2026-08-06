package info.isaksson.erland.zipgithub.domain.status;

import java.util.Map;
import java.util.Set;

public enum ImportPlanStatus {
    DRAFT, READY, APPROVED, REJECTED, SUPERSEDED, EXPIRED;

    private static final Map<ImportPlanStatus, Set<ImportPlanStatus>> ALLOWED = Map.ofEntries(
            Map.entry(DRAFT, Set.of(READY, REJECTED, EXPIRED)),
            Map.entry(READY, Set.of(APPROVED, REJECTED, SUPERSEDED, EXPIRED)),
            Map.entry(APPROVED, Set.of(SUPERSEDED, EXPIRED))
    );

    public static Map<ImportPlanStatus, Set<ImportPlanStatus>> allowedTransitions() { return ALLOWED; }
}
