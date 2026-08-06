package info.isaksson.erland.zipgithub.upload;

public final class UploadTooLargeException extends RuntimeException {
    private final long maximumBytes;

    public UploadTooLargeException(long maximumBytes) {
        super("Upload exceeds the configured maximum of " + maximumBytes + " bytes.");
        this.maximumBytes = maximumBytes;
    }

    public long maximumBytes() { return maximumBytes; }
}
