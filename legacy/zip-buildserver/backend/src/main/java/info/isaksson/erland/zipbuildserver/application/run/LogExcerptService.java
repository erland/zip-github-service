package info.isaksson.erland.zipbuildserver.application.run;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LogExcerptService {
    private static final int DEFAULT_LIMIT = 2_000;

    public String excerpt(String stdout, String stderr) {
        String combined = join(stdout, stderr);
        if (combined.length() <= DEFAULT_LIMIT) {
            return combined;
        }
        int tailLength = DEFAULT_LIMIT - 40;
        return "[log excerpt truncated]\n" + combined.substring(Math.max(0, combined.length() - tailLength));
    }

    private String join(String stdout, String stderr) {
        String safeStdout = stdout == null ? "" : stdout.strip();
        String safeStderr = stderr == null ? "" : stderr.strip();
        if (safeStdout.isBlank()) {
            return safeStderr;
        }
        if (safeStderr.isBlank()) {
            return safeStdout;
        }
        return safeStdout + "\n" + safeStderr;
    }
}
