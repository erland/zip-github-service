package info.isaksson.erland.zipbuildserver.worker;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public record CommandExecutionRequest(
        String commandLabel,
        Path workspaceRoot,
        String workingDirectory,
        String commandDisplay,
        Duration timeout) {

    public CommandExecutionRequest {
        if (commandLabel == null || commandLabel.isBlank()) {
            throw new IllegalArgumentException("commandLabel is required");
        }
        Objects.requireNonNull(workspaceRoot, "workspaceRoot is required");
        if (workingDirectory == null || workingDirectory.isBlank()) {
            workingDirectory = ".";
        }
        if (commandDisplay == null || commandDisplay.isBlank()) {
            throw new IllegalArgumentException("commandDisplay is required");
        }
        Objects.requireNonNull(timeout, "timeout is required");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    public Path resolvedWorkingDirectory() {
        Path resolved = workspaceRoot.resolve(workingDirectory).normalize();
        if (!resolved.startsWith(workspaceRoot.normalize())) {
            throw new IllegalArgumentException("workingDirectory must stay inside workspaceRoot");
        }
        return resolved;
    }
}
