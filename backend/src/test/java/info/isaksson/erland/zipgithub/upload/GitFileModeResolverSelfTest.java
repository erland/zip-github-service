package info.isaksson.erland.zipgithub.upload;

public final class GitFileModeResolverSelfTest {
    public static void main(String[] args) {
        assertMode("100755", GitFileModeResolver.effectiveMode(GitFileMode.EXECUTABLE, "100644", true));
        assertMode("100644", GitFileModeResolver.effectiveMode(GitFileMode.REGULAR, "100755", true));
        assertMode("100755", GitFileModeResolver.effectiveMode(null, "100755", true));
        assertMode("100644", GitFileModeResolver.effectiveMode(null, "100644", true));
        assertMode("100644", GitFileModeResolver.effectiveMode(null, null, false));
        System.out.println("GitFileModeResolverSelfTest passed");
    }
    private static void assertMode(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected " + expected + " but got " + actual);
    }
}
