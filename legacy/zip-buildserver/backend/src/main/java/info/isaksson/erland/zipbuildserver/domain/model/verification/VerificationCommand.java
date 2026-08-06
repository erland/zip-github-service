package info.isaksson.erland.zipbuildserver.domain.model.verification;

public record VerificationCommand(
        String label,
        String workingDirectory,
        String commandDisplay,
        int timeoutSeconds,
        boolean optional) {
}
