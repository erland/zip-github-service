package info.isaksson.erland.zipgithub.delivery;

/** Git author and committer metadata locked to an import at creation time. */
public record GitCommitIdentity(String authorName, String authorEmail, String committerName, String committerEmail) {
    public GitCommitIdentity {
        authorName = valid(authorName, "authorName");
        authorEmail = valid(authorEmail, "authorEmail");
        committerName = valid(committerName, "committerName");
        committerEmail = valid(committerEmail, "committerEmail");
        if (!authorEmail.contains("@") || !committerEmail.contains("@"))
            throw new IllegalArgumentException("Git email addresses must contain @.");
    }

    private static String valid(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        String trimmed = value.trim();
        if (trimmed.length() > 254 || trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\r') >= 0 || trimmed.indexOf('\0') >= 0)
            throw new IllegalArgumentException(field + " is invalid.");
        return trimmed;
    }
}
