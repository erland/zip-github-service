package info.isaksson.erland.zipbuildserver.worker.docker;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedProcessOutputCollectorTest {
    private final BoundedProcessOutputCollector collector = new BoundedProcessOutputCollector();

    @Test
    void capturesSmallOutputWithoutTruncationMarker() throws Exception {
        BoundedProcessOutputCollector.OutputCapture capture = collect("hello", 10);

        assertEquals("hello", capture.output());
        assertFalse(capture.truncated());
        assertEquals("hello", capture.outputWithTruncationMarker());
    }

    @Test
    void truncatesOversizedOutputAndAddsMarker() throws Exception {
        BoundedProcessOutputCollector.OutputCapture capture = collect("abcdefghij", 4);

        assertEquals("abcd", capture.output());
        assertTrue(capture.truncated());
        assertEquals("abcd\n[output truncated]", capture.outputWithTruncationMarker());
    }

    @Test
    void truncatesOversizedErrorOutputTheSameWayAsStandardOutput() throws Exception {
        BoundedProcessOutputCollector.OutputCapture capture = collect("error details", 5);

        assertEquals("error", capture.output());
        assertTrue(capture.truncated());
        assertEquals("error\n[output truncated]", capture.outputWithTruncationMarker());
    }

    @Test
    void doesNotAddTruncationMarkerForUnfinishedOutputThatHasNotExceededLimit() throws Exception {
        PipedInputStream input = new PipedInputStream();
        PipedOutputStream output = new PipedOutputStream(input);
        BoundedProcessOutputCollector.OutputCapture capture = collector.start(input, 20);

        output.write("partial".getBytes(StandardCharsets.UTF_8));
        output.flush();
        capture.await(Duration.ofMillis(20));

        assertEquals("partial", capture.output());
        assertFalse(capture.truncated());
        assertEquals("partial", capture.outputWithTruncationMarker());

        output.close();
        capture.await(Duration.ofSeconds(1));
    }

    @Test
    void treatsReadErrorsAsTruncatedOutput() throws Exception {
        BoundedProcessOutputCollector.OutputCapture capture = collector.start(new FailingInputStream(), 10);

        capture.await(Duration.ofSeconds(1));

        assertEquals("", capture.output());
        assertTrue(capture.truncated());
        assertEquals("\n[output truncated]", capture.outputWithTruncationMarker());
    }

    @Test
    void zeroByteLimitCapturesNoOutputAndMarksInputAsTruncated() throws Exception {
        BoundedProcessOutputCollector.OutputCapture capture = collect("abc", 0);

        assertEquals("", capture.output());
        assertTrue(capture.truncated());
        assertEquals("\n[output truncated]", capture.outputWithTruncationMarker());
    }

    private BoundedProcessOutputCollector.OutputCapture collect(String value, int maxBytes) throws Exception {
        BoundedProcessOutputCollector.OutputCapture capture = collector.start(
                new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)),
                maxBytes);
        capture.await(Duration.ofSeconds(1));
        return capture;
    }

    private static final class FailingInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("boom");
        }
    }
}
