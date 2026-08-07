package info.isaksson.erland.zipgithub.delivery;

public final class GitDeliveryRetrySelfTest {
    public static void main(String[] args) {
        if (!GitDeliveryService.isRetryable("fatal: unable to access: Could not resolve host")) throw new AssertionError();
        if (!GitDeliveryService.isRetryable("HTTP 503 from upstream")) throw new AssertionError();
        if (GitDeliveryService.isRetryable("non-fast-forward rejected")) throw new AssertionError();
        if (GitDeliveryService.isRetryable("base branch moved")) throw new AssertionError();
    }
}
