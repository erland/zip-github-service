package info.isaksson.erland.zipgithub.domain.status;

import java.util.Map;
import java.util.Set;

public enum StagingImportStatus {
    AVAILABLE, CLAIMED, PROMOTED, EXPIRED, CANCELLED;

    private static final Set<StagingImportStatus> TERMINAL = Set.of(PROMOTED, EXPIRED, CANCELLED);
    private static final Map<StagingImportStatus, Set<StagingImportStatus>> ALLOWED = Map.of(
            AVAILABLE, Set.of(CLAIMED, EXPIRED, CANCELLED),
            CLAIMED, Set.of(PROMOTED, EXPIRED, CANCELLED));

    public boolean terminal() { return TERMINAL.contains(this); }
    public static Map<StagingImportStatus, Set<StagingImportStatus>> allowedTransitions() { return ALLOWED; }
}
