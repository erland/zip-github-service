package info.isaksson.erland.zipbuildserver.application.run;

import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;
import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunStatusCalculatorTest {
    private final RunStatusCalculator calculator = new RunStatusCalculator();

    @Test
    void finalStatusPassedWhenNoFailureOrTimeoutOccurred() {
        assertEquals(RunStatus.PASSED, calculator.finalStatus(false, false));
    }

    @Test
    void finalStatusFailedWhenFailureOccurred() {
        assertEquals(RunStatus.FAILED, calculator.finalStatus(true, false));
    }

    @Test
    void finalStatusTimedOutTakesPrecedenceOverFailure() {
        assertEquals(RunStatus.TIMED_OUT, calculator.finalStatus(true, true));
    }

    @Test
    void skipsLaterCommandsAfterFailureTimeoutOrInternalError() {
        assertTrue(calculator.shouldSkipLaterCommands(CheckStatus.FAILED));
        assertTrue(calculator.shouldSkipLaterCommands(CheckStatus.TIMED_OUT));
        assertTrue(calculator.shouldSkipLaterCommands(CheckStatus.INTERNAL_ERROR));
    }

    @Test
    void doesNotSkipLaterCommandsAfterPassedOrSkippedStatus() {
        assertFalse(calculator.shouldSkipLaterCommands(CheckStatus.PASSED));
        assertFalse(calculator.shouldSkipLaterCommands(CheckStatus.SKIPPED));
    }

    @Test
    void summaryMessagesMatchExistingRunSummaries() {
        assertEquals("Verification passed. 3 approved command(s) completed.", calculator.summaryFor(RunStatus.PASSED, 3));
        assertEquals("Verification failed. Review command-level failure details.", calculator.summaryFor(RunStatus.FAILED, 3));
        assertEquals("Verification timed out. Review command-level timeout details.", calculator.summaryFor(RunStatus.TIMED_OUT, 3));
        assertEquals("Verification completed with status RUNNING.", calculator.summaryFor(RunStatus.RUNNING, 3));
    }
}
