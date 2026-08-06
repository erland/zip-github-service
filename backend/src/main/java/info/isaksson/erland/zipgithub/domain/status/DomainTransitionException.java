package info.isaksson.erland.zipgithub.domain.status;

public final class DomainTransitionException extends IllegalStateException {
    public DomainTransitionException(Enum<?> current, Enum<?> target) {
        super("Transition from " + current + " to " + target + " is not allowed");
    }
}
