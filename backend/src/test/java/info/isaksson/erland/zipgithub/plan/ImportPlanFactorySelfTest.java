package info.isaksson.erland.zipgithub.plan;

import java.util.List;
import java.util.UUID;

public final class ImportPlanFactorySelfTest {
    public static void main(String[] args) {
        var entry = new ImmutableImportPlanEntry("README.md", "MODIFIED", "MODIFIED", "NONE",
                null, null, 4L, "a".repeat(64), 3L, "b".repeat(64), true);
        String one = ImportPlanFactory.digest(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "c".repeat(64), "1".repeat(40), "mvp-1", "READY", List.of(entry));
        String two = ImportPlanFactory.digest(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "c".repeat(64), "1".repeat(40), "mvp-1", "READY", List.of(entry));
        if (!one.equals(two) || one.length() != 64) throw new AssertionError("plan digest is not deterministic");
        System.out.println("ImportPlanFactorySelfTest passed");
    }
}
