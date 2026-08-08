package info.isaksson.erland.zipgithub.github;

public final class ActionLogCondensorSelfTest {
    public static void main(String[] args) {
        var maven = ActionLogCondensor.condense("\u001B[31m[ERROR] Failed to execute goal org.apache.maven:maven-compiler-plugin\u001B[0m\n"
                + "Authorization: Bearer ghp_abcdefghijklmnopqrstuvwxyz123456\n"
                + "token=github_pat_abcdefghijklmnopqrstuvwxyz123456\n");
        require("Maven/Gradle".equals(maven.tool()), "Maven should be detected");
        require(maven.lines().size() == 1, "Only the Maven error line should be retained");
        require(!maven.lines().getFirst().contains("\u001B"), "ANSI must be removed");

        String sanitized = ActionLogCondensor.sanitize("npm ERR! boom\nAuthorization: Bearer ghp_abcdefghijklmnopqrstuvwxyz123456\npassword=hunter2");
        require(!sanitized.contains("ghp_"), "GitHub token must be redacted");
        require(!sanitized.contains("hunter2"), "password must be redacted");
        require(sanitized.contains("[REDACTED]"), "redaction marker expected");

        var vite = ActionLogCondensor.condense("vite: error failed to load config from vite.config.ts\nError: broken\n");
        require("npm/Vite".equals(vite.tool()), "Vite should be detected");
        require(!vite.lines().isEmpty(), "Vite excerpt expected");

        var unknown = ActionLogCondensor.condense("ordinary informational output only");
        require(unknown.lines().isEmpty(), "Unknown logs should not be exposed as guessed errors");
        System.out.println("ActionLogCondensorSelfTest passed");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
