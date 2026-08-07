package info.isaksson.erland.zipgithub.selection;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlan;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlanEntry;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/** Validates and creates a deterministic immutable selection for one immutable import plan. */
@ApplicationScoped
public class ImportSelectionFactory {
    public static final String SELECTION_VERSION = "selection-1";

    public ApprovedSelection create(UUID ownerUserId, ImmutableImportPlan plan,
                                    String submittedPlanDigest, String submittedBaseCommitSha,
                                    List<String> requestedSelectedPaths,
                                    List<RequestedOverride> requestedOverrides,
                                    Instant createdAt) {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(createdAt, "createdAt");

        if (!plan.ownerUserId().equals(ownerUserId)) {
            throw ApiException.notFound("IMPORT_PLAN_NOT_FOUND", "The import plan was not found.");
        }
        requireSha256(submittedPlanDigest, "INVALID_PLAN_DIGEST", "planDigestSha256");
        if (!plan.planDigestSha256().equals(submittedPlanDigest)) {
            throw ApiException.conflict("IMPORT_PLAN_DIGEST_MISMATCH",
                    "The submitted plan digest does not match the immutable plan.");
        }
        if (submittedBaseCommitSha == null || !submittedBaseCommitSha.matches("[0-9a-f]{40}")) {
            throw ApiException.badRequest("INVALID_BASE_COMMIT_SHA",
                    "baseCommitSha must be a lower-case 40-character Git SHA.");
        }
        if (!plan.baseCommitSha().equals(submittedBaseCommitSha)) {
            throw ApiException.conflict("IMPORT_PLAN_BASE_MISMATCH",
                    "The submitted base commit no longer matches the immutable plan.");
        }

        Map<String, ImmutableImportPlanEntry> entries = new HashMap<>();
        for (ImmutableImportPlanEntry entry : plan.entries()) entries.put(entry.path(), entry);

        List<String> selected = normalizedUniquePaths(requestedSelectedPaths);
        if (selected.isEmpty()) {
            throw ApiException.badRequest("EMPTY_SELECTION", "At least one changed path must be selected.");
        }

        Map<String, RequestedOverride> overrideRequests = new HashMap<>();
        if (requestedOverrides != null) {
            for (RequestedOverride override : requestedOverrides) {
                if (override == null || override.path() == null || override.path().isBlank()) {
                    throw ApiException.badRequest("INVALID_SELECTION_OVERRIDE", "Override path is required.");
                }
                if (overrideRequests.putIfAbsent(override.path(), override) != null) {
                    throw ApiException.badRequest("DUPLICATE_SELECTION_OVERRIDE",
                            "Each path may have at most one override.");
                }
            }
        }

        List<ApprovedSelectionOverride> overrides = new ArrayList<>();
        Set<String> selectedSet = new HashSet<>(selected);
        for (String path : selected) {
            ImmutableImportPlanEntry entry = entries.get(path);
            if (entry == null) {
                throw ApiException.badRequest("SELECTION_PATH_NOT_IN_PLAN",
                        "Selected path is not present in the immutable import plan: " + path);
            }
            if ("HARD_BLOCKED".equals(entry.blockerType())) {
                throw ApiException.badRequest("HARD_BLOCKED_PATH_SELECTED",
                        "A hard-blocked path can never be selected: " + path);
            }
            if ("NONE".equals(entry.blockerType())
                    && !("ADDED".equals(entry.status()) || "MODIFIED".equals(entry.status()))) {
                throw ApiException.badRequest("NON_CHANGE_PATH_SELECTED",
                        "Only added or modified unblocked paths can be selected: " + path);
            }
            if ("OVERRIDABLE_BLOCKED".equals(entry.blockerType())) {
                RequestedOverride request = overrideRequests.remove(path);
                if (request == null || request.acknowledgement() == null || request.acknowledgement().isBlank()) {
                    throw ApiException.badRequest("OVERRIDE_REQUIRED",
                            "An explicit acknowledgement is required to select: " + path);
                }
                overrides.add(new ApprovedSelectionOverride(path, entry.blockerType(),
                        Objects.requireNonNullElse(entry.policyCode(), "UNKNOWN_POLICY"),
                        request.acknowledgement().trim()));
            }
        }

        if (!overrideRequests.isEmpty()) {
            String path = overrideRequests.keySet().stream().sorted().findFirst().orElseThrow();
            ImmutableImportPlanEntry entry = entries.get(path);
            if (entry == null) {
                throw ApiException.badRequest("SELECTION_PATH_NOT_IN_PLAN",
                        "Override path is not present in the immutable import plan: " + path);
            }
            if (!selectedSet.contains(path)) {
                throw ApiException.badRequest("OVERRIDE_FOR_EXCLUDED_PATH",
                        "Overrides are only valid for selected paths: " + path);
            }
            throw ApiException.badRequest("OVERRIDE_NOT_ALLOWED",
                    "The selected path does not require an override: " + path);
        }

        List<String> excluded = entries.keySet().stream()
                .filter(path -> !selectedSet.contains(path))
                .sorted()
                .toList();
        selected = selected.stream().sorted().toList();
        overrides = overrides.stream().sorted(Comparator.comparing(ApprovedSelectionOverride::path)).toList();

        String digest = digest(plan, selected, excluded, overrides);
        return new ApprovedSelection(UUID.randomUUID(), plan.importId(), plan.id(), ownerUserId,
                plan.planDigestSha256(), plan.baseCommitSha(), SELECTION_VERSION, digest,
                selected, excluded, overrides, createdAt);
    }

    static String digest(ImmutableImportPlan plan, List<String> selectedPaths, List<String> excludedPaths,
                         List<ApprovedSelectionOverride> overrides) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            append(digest, "selectionVersion", SELECTION_VERSION);
            append(digest, "importId", plan.importId());
            append(digest, "planId", plan.id());
            append(digest, "ownerUserId", plan.ownerUserId());
            append(digest, "planDigestSha256", plan.planDigestSha256());
            append(digest, "baseCommitSha", plan.baseCommitSha());
            for (String path : selectedPaths) append(digest, "selectedPath", path);
            for (String path : excludedPaths) append(digest, "excludedPath", path);
            for (ApprovedSelectionOverride override : overrides) {
                append(digest, "overridePath", override.path());
                append(digest, "overrideBlockerType", override.blockerType());
                append(digest, "overridePolicyCode", override.policyCode());
                append(digest, "overrideAcknowledgement", override.acknowledgement());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static List<String> normalizedUniquePaths(List<String> paths) {
        if (paths == null) return List.of();
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String path : paths) {
            if (path == null || path.isBlank() || path.startsWith("/") || path.contains("\\")) {
                throw ApiException.badRequest("INVALID_SELECTION_PATH", "Selection paths must be normalized and relative.");
            }
            if (!unique.add(path)) {
                throw ApiException.badRequest("DUPLICATE_SELECTION_PATH", "Each selected path may appear only once.");
            }
        }
        return List.copyOf(unique);
    }

    private static void requireSha256(String value, String code, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw ApiException.badRequest(code, field + " must be a lower-case SHA-256.");
        }
    }

    private static void append(MessageDigest digest, String key, Object value) {
        digest.update((key + "=" + Objects.toString(value, "<null>") + "\n").getBytes(StandardCharsets.UTF_8));
    }

    public record RequestedOverride(String path, String acknowledgement) { }
}
