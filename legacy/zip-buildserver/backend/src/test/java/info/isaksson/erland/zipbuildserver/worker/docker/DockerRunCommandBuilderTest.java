package info.isaksson.erland.zipbuildserver.worker.docker;

import info.isaksson.erland.zipbuildserver.worker.CommandExecutionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DockerRunCommandBuilderTest {
    @TempDir
    Path workspace;

    @Test
    void buildsDockerRunCommandWithWorkspaceResourceLimitsAndApprovedCommand() {
        ResourceLimitConfig limits = new ResourceLimitConfig(
                "zip-buildserver-worker-node-maven:local",
                "2g",
                "2",
                "bridge",
                4096,
                workspace.getParent().toString(),
                workspace.getParent().toString());
        DockerRunCommandBuilder builder = new DockerRunCommandBuilder(limits, new DockerWorkspacePathMapper(limits));
        CommandExecutionRequest request = new CommandExecutionRequest(
                "maven-test",
                workspace,
                "backend",
                "mvn test",
                Duration.ofMinutes(10));

        List<String> command = builder.build(request, "zip-buildserver-test");

        assertEquals(List.of(
                "docker",
                "run",
                "--rm",
                "--name",
                "zip-buildserver-test",
                "--network",
                "bridge",
                "--memory",
                "2g",
                "--cpus",
                "2",
                "-v",
                workspace.toAbsolutePath().normalize() + ":/workspace:rw",
                "-w",
                "/workspace/backend",
                "zip-buildserver-worker-node-maven:local",
                "/bin/sh",
                "-lc",
                "mvn test"), command);
    }
}
