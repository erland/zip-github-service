package info.isaksson.erland.zipbuildserver.domain;

import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;
import info.isaksson.erland.zipbuildserver.domain.state.RunStatusTransitions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunStatusTransitionsTest {
    @Test
    void queuedRunMayStartOrBeRejected() {
        assertTrue(RunStatusTransitions.canTransition(RunStatus.QUEUED, RunStatus.RUNNING));
        assertTrue(RunStatusTransitions.canTransition(RunStatus.QUEUED, RunStatus.REJECTED));
    }

    @Test
    void runningRunMayCompleteWithTerminalStatus() {
        assertTrue(RunStatusTransitions.canTransition(RunStatus.RUNNING, RunStatus.PASSED));
        assertTrue(RunStatusTransitions.canTransition(RunStatus.RUNNING, RunStatus.FAILED));
        assertTrue(RunStatusTransitions.canTransition(RunStatus.RUNNING, RunStatus.TIMED_OUT));
    }

    @Test
    void terminalRunCannotTransitionAgain() {
        assertFalse(RunStatusTransitions.canTransition(RunStatus.PASSED, RunStatus.FAILED));
    }
}
