package info.isaksson.erland.zipgithub.domain.status;

import java.util.Map;
import java.util.Set;

public enum SourceUploadStatus {
    CREATED, UPLOADING, STORED, VALIDATING, VALIDATED, REJECTED, FAILED, EXPIRED, DELETED;

    private static final Map<SourceUploadStatus, Set<SourceUploadStatus>> ALLOWED = Map.ofEntries(
            Map.entry(CREATED, Set.of(UPLOADING, EXPIRED)),
            Map.entry(UPLOADING, Set.of(STORED, FAILED, EXPIRED)),
            Map.entry(STORED, Set.of(VALIDATING, EXPIRED, DELETED)),
            Map.entry(VALIDATING, Set.of(VALIDATED, REJECTED, FAILED, EXPIRED)),
            Map.entry(VALIDATED, Set.of(EXPIRED, DELETED)),
            Map.entry(REJECTED, Set.of(DELETED)),
            Map.entry(FAILED, Set.of(DELETED)),
            Map.entry(EXPIRED, Set.of(DELETED))
    );

    public static Map<SourceUploadStatus, Set<SourceUploadStatus>> allowedTransitions() { return ALLOWED; }
}
