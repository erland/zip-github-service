package info.isaksson.erland.zipgithub.github;

import java.util.List;

final class GitHubCheckStatusAggregator {
    private GitHubCheckStatusAggregator() {}

    static GitHubCheckStatusClient.GitHubCheckStatus aggregate(List<Run> runs, String detailsUrl) {
        int pending = 0, successful = 0, failed = 0, cancelled = 0;
        for (Run run : runs) {
            if (!"completed".equals(run.status())) pending++;
            else if ("cancelled".equals(run.conclusion())) cancelled++;
            else if ("success".equals(run.conclusion()) || "neutral".equals(run.conclusion()) || "skipped".equals(run.conclusion())) successful++;
            else failed++;
        }
        String state;
        boolean terminal;
        if (runs.isEmpty() || pending > 0) { state = "pending"; terminal = false; }
        else if (failed > 0) { state = "failure"; terminal = true; }
        else if (cancelled > 0) { state = "cancelled"; terminal = true; }
        else { state = "success"; terminal = true; }
        return new GitHubCheckStatusClient.GitHubCheckStatus(state, terminal, runs.size(), pending, successful,
                failed, cancelled, detailsUrl);
    }

    record Run(String status, String conclusion) {}
}
