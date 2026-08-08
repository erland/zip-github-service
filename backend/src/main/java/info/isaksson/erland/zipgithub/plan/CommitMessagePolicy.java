package info.isaksson.erland.zipgithub.plan;

import java.util.UUID;

/** Normalizes and validates the user-controlled commit message bound to import approval. */
public final class CommitMessagePolicy {
    public static final int MAX_LENGTH = 500;

    private CommitMessagePolicy() {}

    public static String defaultSuggestion(UUID importId) {
        if (importId == null) throw new IllegalArgumentException("importId is required");
        return "Apply approved ZIP import " + importId;
    }

    public static String requireInteractive(String value) {
        if (value == null) throw new IllegalArgumentException("Commit message is required.");
        String normalized = normalizeLineEndings(value).strip();
        if (normalized.isBlank()) throw new IllegalArgumentException("Commit message must not be empty.");
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Commit message must be at most " + MAX_LENGTH + " characters.");
        }
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if ((ch < 0x20 || ch == 0x7f) && ch != '\n') {
                throw new IllegalArgumentException("Commit message contains unsupported control characters.");
            }
        }
        return normalized;
    }

    public static String persistedOrLegacyFallback(String value, UUID importId) {
        if (value == null || value.isBlank()) return defaultSuggestion(importId);
        return requireInteractive(value);
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }
}
