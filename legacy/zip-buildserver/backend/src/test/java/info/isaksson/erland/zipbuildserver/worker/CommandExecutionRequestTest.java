package info.isaksson.erland.zipbuildserver.worker;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandExecutionRequestTest {
    @Test
    void resolvesWorkingDirectoryInsideWorkspace() {
        CommandExecutionRequest request = new CommandExecutionRequest(
                "test",
                Path.of("/workspace"),
                "backend",
                "mvn test",
                Duration.ofMinutes(10));

        assertEquals(Path.of("/workspace/backend"), request.resolvedWorkingDirectory());
    }

    @Test
    void rejectsWorkingDirectoryTraversalOutsideWorkspace() {
        CommandExecutionRequest request = new CommandExecutionRequest(
                "test",
                Path.of("/workspace"),
                "../outside",
                "mvn test",
                Duration.ofMinutes(10));

        assertThrows(IllegalArgumentException.class, request::resolvedWorkingDirectory);
    }
}
