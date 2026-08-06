package info.isaksson.erland.zipbuildserver.domain.state;

import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;

public final class RunStatusTransitions {
    private RunStatusTransitions() {
    }

    public static boolean canTransition(RunStatus from, RunStatus to) {
        if (from == null || to == null || from.isTerminal()) {
            return false;
        }

        return switch (from) {
            case QUEUED -> to == RunStatus.RUNNING
                    || to == RunStatus.REJECTED
                    || to == RunStatus.CANCELLED
                    || to == RunStatus.INTERNAL_ERROR;
            case RUNNING -> to == RunStatus.PASSED
                    || to == RunStatus.FAILED
                    || to == RunStatus.TIMED_OUT
                    || to == RunStatus.CANCELLED
                    || to == RunStatus.INCOMPLETE
                    || to == RunStatus.INTERNAL_ERROR;
            default -> false;
        };
    }
}
