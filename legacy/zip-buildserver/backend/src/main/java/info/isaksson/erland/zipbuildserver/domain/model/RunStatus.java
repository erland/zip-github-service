package info.isaksson.erland.zipbuildserver.domain.model;

public enum RunStatus {
    QUEUED,
    RUNNING,
    PASSED,
    FAILED,
    REJECTED,
    TIMED_OUT,
    CANCELLED,
    INCOMPLETE,
    INTERNAL_ERROR;

    public boolean isTerminal() {
        return switch (this) {
            case PASSED, FAILED, REJECTED, TIMED_OUT, CANCELLED, INCOMPLETE, INTERNAL_ERROR -> true;
            case QUEUED, RUNNING -> false;
        };
    }
}
