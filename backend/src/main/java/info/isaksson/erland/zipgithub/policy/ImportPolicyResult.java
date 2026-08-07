package info.isaksson.erland.zipgithub.policy;

import info.isaksson.erland.zipgithub.comparison.ImportFileStatus;

import java.util.List;
import java.util.UUID;

public record ImportPolicyResult(
        UUID importId,
        String baseCommitSha,
        String policyVersion,
        boolean approvable,
        List<ImportPolicyEntry> entries) {
    public ImportPolicyResult {
        entries = List.copyOf(entries);
    }

    public long count(ImportFileStatus status) {
        return entries.stream().filter(entry -> entry.status() == status).count();
    }

    public long warnings() {
        return entries.stream().filter(entry -> entry.severity() == ImportPolicySeverity.WARNING).count();
    }

    public long blockers() {
        return entries.stream().filter(entry -> entry.severity() == ImportPolicySeverity.BLOCKING).count();
    }
}
