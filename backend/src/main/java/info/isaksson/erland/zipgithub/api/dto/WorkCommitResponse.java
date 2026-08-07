package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;

public record WorkCommitResponse(String sha, String message, String authorName, String authorEmail,
                                 Instant authoredAt, String htmlUrl, boolean fallback) {}
