package info.isaksson.erland.zipgithub.actions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class ActionsControlPolicy {
    private final Set<String> dispatch;
    private final Set<String> rerun;

    @Inject
    public ActionsControlPolicy(
            @ConfigProperty(name = "zipgithub.actions.allowed-dispatch-workflows") Optional<String> dispatchWorkflows,
            @ConfigProperty(name = "zipgithub.actions.allowed-rerun-workflows") Optional<String> rerunWorkflows) {
        this(dispatchWorkflows.orElse(""), rerunWorkflows.orElse(""));
    }

    ActionsControlPolicy(String dispatchWorkflows, String rerunWorkflows) {
        this.dispatch = parse(dispatchWorkflows);
        this.rerun = parse(rerunWorkflows);
    }

    public List<String> dispatchIdentifiers() { return List.copyOf(dispatch); }
    public List<String> rerunIdentifiers() { return List.copyOf(rerun); }
    public boolean dispatchAllowed(String identifier, long workflowId, String path) { return matches(dispatch, identifier, workflowId, path); }
    public boolean rerunAllowed(String identifier, long workflowId, String path) { return matches(rerun, identifier, workflowId, path); }

    static Set<String> parse(String configured) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (configured == null || configured.isBlank()) return Set.of();
        for (String raw : configured.split(",")) {
            String value = normalize(raw);
            if (value.isBlank()) continue;
            if (!value.matches("[a-z0-9._/-]{1,200}")) {
                throw new IllegalArgumentException("Invalid workflow allowlist identifier: " + raw.trim());
            }
            result.add(value);
        }
        return Set.copyOf(result);
    }

    private static boolean matches(Set<String> allowed, String identifier, long workflowId, String path) {
        String normalizedIdentifier = normalize(identifier);
        String normalizedPath = normalize(path);
        int at = normalizedPath.indexOf('@');
        if (at >= 0) normalizedPath = normalizedPath.substring(0, at);
        String fileName = normalizedPath.contains("/") ? normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1) : normalizedPath;
        String id = Long.toString(workflowId);
        return allowed.contains(normalizedIdentifier) || allowed.contains(normalizedPath) || allowed.contains(fileName) || allowed.contains(id);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
