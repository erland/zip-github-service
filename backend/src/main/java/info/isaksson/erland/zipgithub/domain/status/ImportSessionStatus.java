package info.isaksson.erland.zipgithub.domain.status;

import java.util.Map;
import java.util.Set;

public enum ImportSessionStatus {
    CREATED, UPLOADING, INSPECTING, PLAN_READY, APPROVED, DELIVERING,
    PULL_REQUEST_CREATED, COMPLETED,
    UPLOAD_FAILED, INSPECTION_FAILED, BLOCKED, DELIVERY_FAILED, CANCELLED, EXPIRED;

    private static final Set<ImportSessionStatus> TERMINAL = Set.of(
            COMPLETED, UPLOAD_FAILED, INSPECTION_FAILED, BLOCKED,
            DELIVERY_FAILED, CANCELLED, EXPIRED);

    private static final Map<ImportSessionStatus, Set<ImportSessionStatus>> ALLOWED = Map.ofEntries(
            Map.entry(CREATED, Set.of(UPLOADING, CANCELLED, EXPIRED)),
            Map.entry(UPLOADING, Set.of(INSPECTING, UPLOAD_FAILED, CANCELLED, EXPIRED)),
            Map.entry(INSPECTING, Set.of(PLAN_READY, INSPECTION_FAILED, BLOCKED, CANCELLED, EXPIRED)),
            Map.entry(PLAN_READY, Set.of(APPROVED, BLOCKED, CANCELLED, EXPIRED)),
            Map.entry(APPROVED, Set.of(DELIVERING, CANCELLED, EXPIRED)),
            Map.entry(DELIVERING, Set.of(PULL_REQUEST_CREATED, DELIVERY_FAILED)),
            Map.entry(PULL_REQUEST_CREATED, Set.of(COMPLETED, DELIVERY_FAILED)),
            Map.entry(DELIVERY_FAILED, Set.of(DELIVERING, CANCELLED, EXPIRED))
    );

    public boolean terminal() { return TERMINAL.contains(this); }
    public static Map<ImportSessionStatus, Set<ImportSessionStatus>> allowedTransitions() { return ALLOWED; }
}
