package info.isaksson.erland.zipbuildserver.domain.model;

public enum CheckStatus {
    PASSED,
    FAILED,
    SKIPPED,
    TIMED_OUT,
    CANCELLED,
    NOT_APPLICABLE,
    INTERNAL_ERROR
}
