package info.isaksson.erland.zipgithub.delivery;

public class GitDeliveryException extends RuntimeException {
    private final boolean retryable;

    public GitDeliveryException(String message) { this(message, false, null); }
    public GitDeliveryException(String message, Throwable cause) { this(message, false, cause); }
    public GitDeliveryException(String message, boolean retryable) { this(message, retryable, null); }
    public GitDeliveryException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }
    public boolean retryable() { return retryable; }
}
