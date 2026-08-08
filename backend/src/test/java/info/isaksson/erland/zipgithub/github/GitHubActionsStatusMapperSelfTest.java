package info.isaksson.erland.zipgithub.github;

import java.util.List;

public final class GitHubActionsStatusMapperSelfTest {
    public static void main(String[] args) {
        assertState("pending", false, "queued", "");
        assertState("pending", false, "in_progress", "");
        assertState("success", true, "completed", "success");
        assertState("success", true, "completed", "neutral");
        assertState("success", true, "completed", "skipped");
        assertState("cancelled", true, "completed", "cancelled");
        assertState("failure", true, "completed", "failure");
        assertState("failure", true, "completed", "timed_out");

        assertEquals("not_started", GitHubActionsStatusMapper.aggregate(true, 0, List.of()));
        assertEquals("unavailable", GitHubActionsStatusMapper.aggregate(false, 0, List.of()));
        assertEquals("pending", GitHubActionsStatusMapper.aggregate(true, 2, List.of(
                new GitHubActionsStatusMapper.State("success", true),
                new GitHubActionsStatusMapper.State("pending", false))));
        assertEquals("failure", GitHubActionsStatusMapper.aggregate(true, 2, List.of(
                new GitHubActionsStatusMapper.State("success", true),
                new GitHubActionsStatusMapper.State("failure", true))));
        assertEquals("cancelled", GitHubActionsStatusMapper.aggregate(true, 1, List.of(
                new GitHubActionsStatusMapper.State("cancelled", true))));
        assertEquals("success", GitHubActionsStatusMapper.aggregate(true, 2, List.of(
                new GitHubActionsStatusMapper.State("success", true),
                new GitHubActionsStatusMapper.State("success", true))));
    }

    private static void assertState(String expectedValue, boolean expectedTerminal, String status, String conclusion) {
        var actual = GitHubActionsStatusMapper.map(status, conclusion);
        assertEquals(expectedValue, actual.value());
        if (actual.terminal() != expectedTerminal) throw new AssertionError(actual);
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
}
