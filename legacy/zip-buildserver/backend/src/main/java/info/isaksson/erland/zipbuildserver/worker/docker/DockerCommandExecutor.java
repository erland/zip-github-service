package info.isaksson.erland.zipbuildserver.worker.docker;

import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;
import info.isaksson.erland.zipbuildserver.worker.CommandExecutionRequest;
import info.isaksson.erland.zipbuildserver.worker.CommandExecutionResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class DockerCommandExecutor {
    private final ResourceLimitConfig resourceLimitConfig;
    private final DockerRunCommandBuilder dockerRunCommandBuilder;
    private final BoundedProcessOutputCollector outputCollector;
    private final DockerContainerCleanup containerCleanup;

    public DockerCommandExecutor(
            ResourceLimitConfig resourceLimitConfig,
            DockerRunCommandBuilder dockerRunCommandBuilder,
            BoundedProcessOutputCollector outputCollector,
            DockerContainerCleanup containerCleanup) {
        this.resourceLimitConfig = resourceLimitConfig;
        this.dockerRunCommandBuilder = dockerRunCommandBuilder;
        this.outputCollector = outputCollector;
        this.containerCleanup = containerCleanup;
    }

    public CommandExecutionResult execute(CommandExecutionRequest request) {
        request.resolvedWorkingDirectory();

        Instant started = Instant.now();
        String containerName = "zip-buildserver-" + UUID.randomUUID();
        List<String> command = dockerRunCommandBuilder.build(request, containerName);

        try {
            Process process = new ProcessBuilder(command).start();
            BoundedProcessOutputCollector.OutputCapture stdout = outputCollector.start(
                    process.getInputStream(),
                    resourceLimitConfig.maxOutputBytes());
            BoundedProcessOutputCollector.OutputCapture stderr = outputCollector.start(
                    process.getErrorStream(),
                    resourceLimitConfig.maxOutputBytes());

            boolean completed = process.waitFor(request.timeout().toMillis(), TimeUnit.MILLISECONDS);
            Duration duration = Duration.between(started, Instant.now());

            if (!completed) {
                process.destroyForcibly();
                containerCleanup.cleanup(containerName);
                stdout.await(Duration.ofSeconds(2));
                stderr.await(Duration.ofSeconds(2));
                return CommandExecutionResult.timedOut(
                        request.commandLabel(),
                        duration,
                        stdout.output(),
                        stderr.outputWithTruncationMarker() + "\nCommand exceeded timeout of " + request.timeout().toSeconds() + " seconds.");
            }

            int exitCode = process.exitValue();
            stdout.await(Duration.ofSeconds(2));
            stderr.await(Duration.ofSeconds(2));

            String stdoutText = stdout.outputWithTruncationMarker();
            String stderrText = stderr.outputWithTruncationMarker();

            if (exitCode == 0) {
                return CommandExecutionResult.passed(request.commandLabel(), duration, stdoutText, stderrText);
            }
            return CommandExecutionResult.failed(
                    request.commandLabel(),
                    exitCode,
                    duration,
                    stdoutText,
                    stderrText,
                    "Docker worker command exited with status " + exitCode + ".");
        } catch (IOException exception) {
            return new CommandExecutionResult(
                    request.commandLabel(),
                    CheckStatus.INTERNAL_ERROR,
                    -1,
                    Duration.between(started, Instant.now()),
                    false,
                    "",
                    exception.getMessage(),
                    "Could not start Docker worker command.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            containerCleanup.cleanup(containerName);
            return new CommandExecutionResult(
                    request.commandLabel(),
                    CheckStatus.CANCELLED,
                    -1,
                    Duration.between(started, Instant.now()),
                    false,
                    "",
                    exception.getMessage(),
                    "Docker worker command was interrupted.");
        }
    }

    List<String> dockerRunCommand(CommandExecutionRequest request, String containerName) {
        return dockerRunCommandBuilder.build(request, containerName);
    }

}
