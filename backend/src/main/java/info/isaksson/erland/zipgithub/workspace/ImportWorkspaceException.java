package info.isaksson.erland.zipgithub.workspace;

public class ImportWorkspaceException extends RuntimeException {
    public ImportWorkspaceException(String message) { super(message); }
    public ImportWorkspaceException(String message, Throwable cause) { super(message, cause); }
}
