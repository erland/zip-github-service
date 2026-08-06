package info.isaksson.erland.zipbuildserver.worker.docker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerContainerCleanupTest {
    @AfterEach
    void clearInterruptedFlag() {
        Thread.interrupted();
    }

    @Test
    void startsDockerRemoveForceCommandForContainerName() {
        AtomicReference<List<String>> command = new AtomicReference<>();
        DockerContainerCleanup cleanup = new DockerContainerCleanup(arguments -> {
            command.set(arguments);
            return new WaitingProcess(true);
        });

        cleanup.cleanup("zip-buildserver-test");

        assertEquals(List.of("docker", "rm", "-f", "zip-buildserver-test"), command.get());
    }

    @Test
    void waitsAtMostFiveSecondsForCleanupProcess() {
        AtomicReference<Long> timeout = new AtomicReference<>();
        AtomicReference<TimeUnit> unit = new AtomicReference<>();
        DockerContainerCleanup cleanup = new DockerContainerCleanup(arguments -> new WaitingProcess(true) {
            @Override
            public boolean waitFor(long timeoutValue, TimeUnit unitValue) {
                timeout.set(timeoutValue);
                unit.set(unitValue);
                return true;
            }
        });

        cleanup.cleanup("zip-buildserver-test");

        assertEquals(5L, timeout.get());
        assertEquals(TimeUnit.SECONDS, unit.get());
    }

    @Test
    void ignoresCleanupStartFailures() {
        DockerContainerCleanup cleanup = new DockerContainerCleanup(arguments -> {
            throw new IOException("docker unavailable");
        });

        cleanup.cleanup("zip-buildserver-test");
    }

    @Test
    void restoresInterruptedFlagWhenCleanupWaitIsInterrupted() {
        DockerContainerCleanup cleanup = new DockerContainerCleanup(arguments -> new WaitingProcess(true) {
            @Override
            public boolean waitFor(long timeoutValue, TimeUnit unitValue) throws InterruptedException {
                throw new InterruptedException("interrupted");
            }
        });

        cleanup.cleanup("zip-buildserver-test");

        assertTrue(Thread.currentThread().isInterrupted());
    }

    private static class WaitingProcess extends Process {
        private final boolean waitResult;

        private WaitingProcess(boolean waitResult) {
            this.waitResult = waitResult;
        }

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() {
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            return waitResult;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
        }
    }
}
