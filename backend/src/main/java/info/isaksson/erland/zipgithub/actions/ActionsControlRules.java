package info.isaksson.erland.zipgithub.actions;

import java.util.Objects;
import java.util.UUID;

/** Pure guard rules kept separate so stale-Work and exact-run checks are easy to regression-test. */
public final class ActionsControlRules {
    private ActionsControlRules() {}

    public static boolean currentWork(UUID expectedImportId, String expectedRef, String expectedCommitSha,
                                      UUID actualImportId, String actualRef, String actualCommitSha) {
        return Objects.equals(expectedImportId, actualImportId)
                && Objects.equals(expectedRef, actualRef)
                && Objects.equals(expectedCommitSha, actualCommitSha);
    }

    public static boolean exactRun(String expectedRef, String expectedCommitSha, String runRef, String runCommitSha) {
        return Objects.equals(expectedRef, runRef) && Objects.equals(expectedCommitSha, runCommitSha);
    }
}
