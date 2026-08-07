package info.isaksson.erland.zipgithub.comparison;

import java.util.List;
import java.util.UUID;

public record ImportComparison(
        UUID importId,
        String baseCommitSha,
        List<ImportComparisonEntry> entries) {
    public ImportComparison {
        entries = List.copyOf(entries);
    }

    public long count(ImportFileStatus status) {
        return entries.stream().filter(entry -> entry.status() == status).count();
    }
}
