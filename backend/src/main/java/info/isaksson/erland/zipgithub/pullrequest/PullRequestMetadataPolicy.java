package info.isaksson.erland.zipgithub.pullrequest;

/** Validates user-controlled pull request metadata before it is sent to GitHub. */
public final class PullRequestMetadataPolicy {
    public static final int MAX_TITLE_LENGTH = 256;
    public static final int MAX_DESCRIPTION_LENGTH = 65536;

    private PullRequestMetadataPolicy() {}

    public static String requireTitle(String value) {
        return require(value, "Pull request title", MAX_TITLE_LENGTH);
    }

    public static String requireDescription(String value) {
        return require(value, "Pull request description", MAX_DESCRIPTION_LENGTH);
    }

    private static String require(String value, String label, int maxLength) {
        if (value == null) throw new IllegalArgumentException(label + " is required.");
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(label + " must not be empty.");
        if (normalized.length() > maxLength) throw new IllegalArgumentException(label + " must be at most " + maxLength + " characters.");
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if ((ch < 0x20 || ch == 0x7f) && ch != '\n' && ch != '\t') {
                throw new IllegalArgumentException(label + " contains unsupported control characters.");
            }
        }
        return normalized;
    }
}
