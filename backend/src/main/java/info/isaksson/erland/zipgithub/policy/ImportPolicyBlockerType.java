package info.isaksson.erland.zipgithub.policy;

/** Classifies whether a policy-blocked path can ever be selected for delivery. */
public enum ImportPolicyBlockerType {
    NONE,
    HARD_BLOCKED,
    OVERRIDABLE_BLOCKED
}
