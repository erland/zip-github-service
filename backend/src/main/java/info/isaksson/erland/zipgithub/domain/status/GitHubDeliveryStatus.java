package info.isaksson.erland.zipgithub.domain.status;

import java.util.Map;
import java.util.Set;

public enum GitHubDeliveryStatus {
    CREATED, PREPARING, COMMITTING, PUSHING, BRANCH_PUSHED, CREATING_PULL_REQUEST,
    PULL_REQUEST_CREATED, COMPLETED, FAILED, CANCELLED;

    private static final Map<GitHubDeliveryStatus, Set<GitHubDeliveryStatus>> ALLOWED = Map.ofEntries(
            Map.entry(CREATED, Set.of(PREPARING, CANCELLED)),
            Map.entry(PREPARING, Set.of(COMMITTING, FAILED, CANCELLED)),
            Map.entry(COMMITTING, Set.of(PUSHING, FAILED)),
            Map.entry(PUSHING, Set.of(BRANCH_PUSHED, FAILED)),
            Map.entry(BRANCH_PUSHED, Set.of(CREATING_PULL_REQUEST, FAILED)),
            Map.entry(CREATING_PULL_REQUEST, Set.of(PULL_REQUEST_CREATED, FAILED)),
            Map.entry(PULL_REQUEST_CREATED, Set.of(COMPLETED, FAILED)),
            Map.entry(FAILED, Set.of(PREPARING, COMMITTING, PUSHING, CREATING_PULL_REQUEST, CANCELLED))
    );

    public static Map<GitHubDeliveryStatus, Set<GitHubDeliveryStatus>> allowedTransitions() { return ALLOWED; }
}
