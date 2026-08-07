package info.isaksson.erland.zipgithub.actions;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActionsControlAuditStoreTest {
    @Test
    void duplicateRetryReusesTheOriginalOperationWithoutCreatingAnotherClaim() {
        var store = new ActionsControlAuditStore();
        store.persistent = false;
        UUID owner = UUID.randomUUID();
        UUID project = UUID.randomUUID();
        UUID importId = UUID.randomUUID();
        String sha = "0123456789abcdef0123456789abcdef01234567";

        var first = store.create(owner, project, importId, "WORKFLOW_DISPATCH", "ci.yml", 42L, null,
                "zip-github/work-test", sha, "request-12345678");
        var retry = store.create(owner, project, importId, "WORKFLOW_DISPATCH", "ci.yml", 42L, null,
                "zip-github/work-test", sha, "request-12345678");

        assertTrue(first.created());
        assertFalse(retry.created());
        assertEquals(first.audit().id(), retry.audit().id());
        assertEquals("STARTED", retry.audit().status());

        var succeeded = store.succeed(first.audit(), 42L, 99L, "https://github.example/actions/runs/99");
        var afterSuccessRetry = store.create(owner, project, importId, "WORKFLOW_DISPATCH", "ci.yml", 42L, null,
                "zip-github/work-test", sha, "request-12345678");
        assertFalse(afterSuccessRetry.created());
        assertEquals(succeeded.id(), afterSuccessRetry.audit().id());
        assertEquals("SUCCEEDED", afterSuccessRetry.audit().status());
        assertEquals(99L, afterSuccessRetry.audit().workflowRunId());
    }
}
