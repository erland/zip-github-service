package info.isaksson.erland.zipgithub.snapshot;

public class RepositorySnapshotException extends RuntimeException {
    public RepositorySnapshotException(String message) { super(message); }
    public RepositorySnapshotException(String message, Throwable cause) { super(message, cause); }
}
