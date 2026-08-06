package info.isaksson.erland.zipbuildserver.worker;

import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;
import info.isaksson.erland.zipbuildserver.worker.fake.FakeCommandExecutor;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FakeCommandExecutorTest {
    @Test
    void returnsConfiguredResultForMatchingCommandLabel() {
        FakeCommandExecutor executor = new FakeCommandExecutor()
                .returns(CommandExecutionResult.failed(
                        "npm-test",
                        1,
                        Duration.ofSeconds(3),
                        "stdout",
                        "stderr",
                        "test failed"));

        CommandExecutionResult result = executor.execute(new CommandExecutionRequest(
                "npm-test",
                Path.of("/workspace"),
                ".",
                "npm test -- --runInBand",
                Duration.ofMinutes(10)));

        assertEquals(CheckStatus.FAILED, result.status());
        assertEquals(1, result.exitCode());
        assertEquals("test failed", result.failureMessage());
    }

    @Test
    void returnsDefaultPassingResultWhenNoResultWasConfigured() {
        FakeCommandExecutor executor = new FakeCommandExecutor();

        CommandExecutionResult result = executor.execute(new CommandExecutionRequest(
                "mvn-test",
                Path.of("/workspace"),
                ".",
                "mvn test",
                Duration.ofMinutes(10)));

        assertEquals(CheckStatus.PASSED, result.status());
        assertEquals(0, result.exitCode());
    }
}
