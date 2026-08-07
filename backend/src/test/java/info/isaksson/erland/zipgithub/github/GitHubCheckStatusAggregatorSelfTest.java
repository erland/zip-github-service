package info.isaksson.erland.zipgithub.github;

import java.util.List;

public final class GitHubCheckStatusAggregatorSelfTest {
    public static void main(String[] args) {
        assertState("pending", false, List.of(new GitHubCheckStatusAggregator.Run("queued", "")));
        assertState("success", true, List.of(new GitHubCheckStatusAggregator.Run("completed", "success")));
        assertState("failure", true, List.of(new GitHubCheckStatusAggregator.Run("completed", "failure")));
        assertState("cancelled", true, List.of(new GitHubCheckStatusAggregator.Run("completed", "cancelled")));
        assertState("pending", false, List.of());
    }

    private static void assertState(String expected, boolean terminal, List<GitHubCheckStatusAggregator.Run> runs) {
        var result = GitHubCheckStatusAggregator.aggregate(runs, "https://example.test/checks");
        if (!expected.equals(result.state()) || terminal != result.terminal()) {
            throw new AssertionError("Expected " + expected + "/" + terminal + " but got " + result);
        }
    }
}
