package info.isaksson.erland.zipbuildserver.worker;

import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;

import java.time.Duration;
import java.util.Objects;

public record CommandExecutionResult(
        String commandLabel,
        CheckStatus status,
        int exitCode,
        Duration duration,
        boolean timedOut,
        String stdout,
        String stderr,
        String failureMessage) {

    public CommandExecutionResult {
        if (commandLabel == null || commandLabel.isBlank()) {
            throw new IllegalArgumentException("commandLabel is required");
        }
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(duration, "duration is required");
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
        failureMessage = failureMessage == null ? "" : failureMessage;
    }

    public static CommandExecutionResult passed(String commandLabel, Duration duration, String stdout, String stderr) {
        return new CommandExecutionResult(commandLabel, CheckStatus.PASSED, 0, duration, false, stdout, stderr, "");
    }

    public static CommandExecutionResult failed(
            String commandLabel,
            int exitCode,
            Duration duration,
            String stdout,
            String stderr,
            String failureMessage) {
        return new CommandExecutionResult(commandLabel, CheckStatus.FAILED, exitCode, duration, false, stdout, stderr, failureMessage);
    }

    public static CommandExecutionResult timedOut(
            String commandLabel,
            Duration duration,
            String stdout,
            String stderr) {
        return new CommandExecutionResult(commandLabel, CheckStatus.TIMED_OUT, -1, duration, true, stdout, stderr, "Command timed out.");
    }
}
