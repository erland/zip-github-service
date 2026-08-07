package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ImportCheckStatusResponse(UUID importId, String repositoryFullName, String commitSha, String state,
                                        boolean terminal, int total, int pending, int successful, int failed,
                                        int cancelled, String detailsUrl, Instant checkedAt) {}
