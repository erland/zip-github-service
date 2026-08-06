package info.isaksson.erland.zipgithub.domain.status;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class StateTransitions {
    private StateTransitions() {}

    /** Returns false for an idempotent repetition and true for a real allowed transition. */
    public static <S extends Enum<S>> boolean transition(S current, S target, Map<S, Set<S>> allowed) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(allowed, "allowed");
        if (current == target) return false;
        if (!allowed.getOrDefault(current, Set.of()).contains(target)) {
            throw new DomainTransitionException(current, target);
        }
        return true;
    }
}
