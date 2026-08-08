package info.isaksson.erland.zipgithub.github;

final class GitHubActionsStatusMapper {
    private GitHubActionsStatusMapper() {}

    static State map(String status, String conclusion) {
        if (!"completed".equals(status)) return new State("pending", false);
        String normalized = conclusion == null ? "" : conclusion;
        if ("success".equals(normalized) || "neutral".equals(normalized) || "skipped".equals(normalized)) {
            return new State("success", true);
        }
        if ("cancelled".equals(normalized) || "stale".equals(normalized)) {
            return new State("cancelled", true);
        }
        return new State("failure", true);
    }

    static String aggregate(boolean available, int itemCount, Iterable<State> states) {
        if (!available) return "unavailable";
        if (itemCount == 0) return "not_started";
        boolean pending = false, failure = false, cancelled = false;
        for (State state : states) {
            pending |= !state.terminal();
            failure |= "failure".equals(state.value());
            cancelled |= "cancelled".equals(state.value());
        }
        if (pending) return "pending";
        if (failure) return "failure";
        if (cancelled) return "cancelled";
        return "success";
    }

    record State(String value, boolean terminal) {}
}
