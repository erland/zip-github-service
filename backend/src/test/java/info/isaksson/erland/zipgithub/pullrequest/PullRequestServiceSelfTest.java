package info.isaksson.erland.zipgithub.pullrequest;

import info.isaksson.erland.zipgithub.delivery.GitDeliveryResult;
import info.isaksson.erland.zipgithub.github.GitHubPullRequestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

public final class PullRequestServiceSelfTest {
    public static void main(String[] args) {
        final int[] creates = {0};
        final String userToken = "user-access-token";
        GitHubPullRequestClient client = new GitHubPullRequestClient() {
            @Override public GitHubPullRequest createDraftPullRequest(String t, String r, String title, String h, String b, String body) {
                if (!userToken.equals(t)) throw new AssertionError("PR create must use authenticated user access token");
                if (!"Explicit PR title".equals(title) || !"Explicit PR description".equals(body)) throw new AssertionError("PR metadata must be user supplied");
                creates[0]++;
                return new GitHubPullRequest(42, "https://github.com/o/r/pull/42", "open", true);
            }
            @Override public Optional<GitHubPullRequest> findOpenPullRequest(String t, String r, String h, String b) {
                if (!userToken.equals(t)) throw new AssertionError("PR lookup must use authenticated user access token");
                return Optional.empty();
            }
        };
        var service = new PullRequestService(client, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        var d = new GitDeliveryResult(UUID.randomUUID(), "o/r", "main", "zip-github/import-x",
                "a".repeat(40), "b".repeat(40), "c".repeat(64), Instant.EPOCH);
        var result = service.createOrReuseDraft(userToken, d, "Explicit PR title", "Explicit PR description");
        if (result.pullRequestNumber() != 42 || creates[0] != 1) throw new AssertionError();

        GitHubPullRequestClient existingClient = new GitHubPullRequestClient() {
            @Override public GitHubPullRequest createDraftPullRequest(String t, String r, String title, String h, String b, String body) {
                throw new AssertionError("must not create duplicate PR");
            }
            @Override public Optional<GitHubPullRequest> findOpenPullRequest(String t, String r, String h, String b) {
                if (!userToken.equals(t)) throw new AssertionError("PR lookup must use authenticated user access token");
                return Optional.of(new GitHubPullRequest(43, "https://github.com/o/r/pull/43", "open", true));
            }
        };
        var reused = new PullRequestService(existingClient, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
                .createOrReuseDraft(userToken, d, "Explicit PR title", "Explicit PR description");
        if (reused.pullRequestNumber() != 43) throw new AssertionError();

        final int[] retryCreates = {0};
        GitHubPullRequestClient retryClient = new GitHubPullRequestClient() {
            private boolean created;
            @Override public GitHubPullRequest createDraftPullRequest(String t, String r, String title, String h, String b, String body) {
                if (!userToken.equals(t)) throw new AssertionError("PR retry create must use authenticated user access token");
                retryCreates[0]++;
                created = true;
                throw new IllegalStateException("response lost after GitHub created the PR");
            }
            @Override public Optional<GitHubPullRequest> findOpenPullRequest(String t, String r, String h, String b) {
                if (!userToken.equals(t)) throw new AssertionError("PR retry lookup must use authenticated user access token");
                return created ? Optional.of(new GitHubPullRequest(44, "https://github.com/o/r/pull/44", "open", true)) : Optional.empty();
            }
        };
        var recovered = new PullRequestService(retryClient, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
                .createOrReuseDraft(userToken, d, "Explicit PR title", "Explicit PR description");
        if (recovered.pullRequestNumber() != 44 || retryCreates[0] != 1)
            throw new AssertionError("retry recovery must reuse the already-created PR");

        try {
            service.createOrReuseDraft(userToken, d, " ", "Description");
            throw new AssertionError("blank PR title must be rejected");
        } catch (IllegalArgumentException expected) { }
        try {
            service.createOrReuseDraft(userToken, d, "Title", " ");
            throw new AssertionError("blank PR description must be rejected");
        } catch (IllegalArgumentException expected) { }
    }
}
