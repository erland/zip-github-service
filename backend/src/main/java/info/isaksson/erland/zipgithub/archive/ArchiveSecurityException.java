package info.isaksson.erland.zipgithub.archive;

public final class ArchiveSecurityException extends RuntimeException {
    private final ArchiveSecurityCode code;
    private final String entryPath;

    public ArchiveSecurityException(ArchiveSecurityCode code, String entryPath, String message) {
        super(message);
        this.code = code;
        this.entryPath = entryPath;
    }

    public ArchiveSecurityCode code() {
        return code;
    }

    public String entryPath() {
        return entryPath;
    }
}
