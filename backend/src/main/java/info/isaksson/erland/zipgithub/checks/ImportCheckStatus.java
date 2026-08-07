package info.isaksson.erland.zipgithub.checks;

import java.time.Instant;
import java.util.UUID;

public record ImportCheckStatus(UUID importId, String repositoryFullName, String commitSha, String state,
                                boolean terminal, int total, int pending, int successful, int failed,
                                int cancelled, String detailsUrl, Instant checkedAt) {}
