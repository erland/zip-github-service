package info.isaksson.erland.zipbuildserver.application.run;

import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;
import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RunStatusCalculator {
    public boolean shouldSkipLaterCommands(CheckStatus status) {
        return status == CheckStatus.FAILED
                || status == CheckStatus.TIMED_OUT
                || status == CheckStatus.INTERNAL_ERROR;
    }

    public RunStatus finalStatus(boolean failed, boolean timedOut) {
        if (timedOut) {
            return RunStatus.TIMED_OUT;
        }
        if (failed) {
            return RunStatus.FAILED;
        }
        return RunStatus.PASSED;
    }

    public String summaryFor(RunStatus status, int commandCount) {
        return switch (status) {
            case PASSED -> "Verification passed. " + commandCount + " approved command(s) completed.";
            case FAILED -> "Verification failed. Review command-level failure details.";
            case TIMED_OUT -> "Verification timed out. Review command-level timeout details.";
            default -> "Verification completed with status " + status + ".";
        };
    }
}
