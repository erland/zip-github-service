package info.isaksson.erland.zipbuildserver.worker.docker;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ApplicationScoped
public class BoundedProcessOutputCollector {
    OutputCapture start(InputStream input, int maxBytes) {
        BoundedOutputReader reader = new BoundedOutputReader(input, maxBytes);
        Thread thread = Thread.ofVirtual().start(reader);
        return new OutputCapture(reader, thread);
    }

    static final class OutputCapture {
        private final BoundedOutputReader reader;
        private final Thread thread;

        private OutputCapture(BoundedOutputReader reader, Thread thread) {
            this.reader = reader;
            this.thread = thread;
        }

        void await(Duration timeout) throws InterruptedException {
            thread.join(timeout);
        }

        String output() {
            return reader.output();
        }

        boolean truncated() {
            return reader.truncated();
        }

        String outputWithTruncationMarker() {
            if (!truncated()) {
                return output();
            }
            return output() + "\n[output truncated]";
        }
    }

    private static final class BoundedOutputReader implements Runnable {
        private final InputStream input;
        private final int maxBytes;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private volatile boolean truncated;

        private BoundedOutputReader(InputStream input, int maxBytes) {
            this.input = input;
            this.maxBytes = Math.max(0, maxBytes);
        }

        @Override
        public void run() {
            byte[] chunk = new byte[8192];
            int read;
            try {
                while ((read = input.read(chunk)) != -1) {
                    int remaining = maxBytes - buffer.size();
                    if (remaining > 0) {
                        buffer.write(chunk, 0, Math.min(read, remaining));
                    }
                    if (read > remaining) {
                        truncated = true;
                    }
                }
            } catch (IOException exception) {
                truncated = true;
            }
        }

        private String output() {
            return buffer.toString(StandardCharsets.UTF_8);
        }

        private boolean truncated() {
            return truncated;
        }
    }
}
