package info.isaksson.erland.zipbuildserver.application.verification;

public class VerificationPlanParseException extends RuntimeException {
    public VerificationPlanParseException(String message) {
        super(message);
    }

    public VerificationPlanParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
