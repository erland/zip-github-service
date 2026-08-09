package info.isaksson.erland.zipgithub.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitHubAppClientActionsResilienceTest {
    @Test
    void keepsWorkflowRunsWhenCheckRunsCannotBeRead() throws Exception {
        String sha = "f3689dfd3b5f011e2ba04d56e3a5f50b4bc97e69";
        var client = new GitHubAppClient() {
            @Override
            protected JsonNode getJson(String url, String token) {
                try {
                    if (url.contains("/actions/runs?")) {
                        return mapper.readTree("""
                                {"workflow_runs":[{
                                  "id":31258714926,
                                  "workflow_id":329441754,
                                  "path":".github/workflows/test-action.yml",
                                  "head_branch":"zip-github/work-322395a5-db12-4a5f-b49f-50871147c4a9",
                                  "head_sha":"f3689dfd3b5f011e2ba04d56e3a5f50b4bc97e69",
                                  "name":"Test Action 4",
                                  "status":"completed",
                                  "conclusion":"success",
                                  "event":"push",
                                  "html_url":"https://github.com/erland/got-test-repo/actions/runs/31258714926",
                                  "created_at":"2026-08-08T13:04:53Z",
                                  "updated_at":"2026-08-08T13:05:01Z"
                                }]}
                                """);
                    }
                    if (url.contains("/actions/runs/31258714926/jobs")) {
                        return mapper.readTree("""
                                {"jobs":[{
                                  "id":93100000000,
                                  "name":"test",
                                  "status":"completed",
                                  "conclusion":"success",
                                  "html_url":"https://github.com/erland/got-test-repo/actions/runs/31258714926/job/93100000000",
                                  "started_at":"2026-08-08T13:04:54Z",
                                  "completed_at":"2026-08-08T13:05:00Z"
                                }]}
                                """);
                    }
                    if (url.contains("/check-runs")) {
                        throw new IllegalStateException("GitHub API returned HTTP 403");
                    }
                    throw new AssertionError("Unexpected URL: " + url);
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        client.mapper = new ObjectMapper();

        var status = client.readCommitActions("installation-token", "erland/got-test-repo", sha);

        assertEquals("success", status.state());
        assertTrue(status.terminal());
        assertEquals(1, status.workflows().size());
        assertEquals(31258714926L, status.workflows().getFirst().id());
        assertEquals(sha, status.workflows().getFirst().headSha());
        assertEquals("push", status.workflows().getFirst().event());
        assertTrue(status.checks().isEmpty());
        assertNull(status.diagnosticCode());
    }

    @Test
    void reportsMissingActionsPermissionWithoutPretendingNoWorkflowExists() {
        var client = new GitHubAppClient() {
            @Override
            protected JsonNode getJson(String url, String token) {
                if (url.contains("/actions/runs?")) throw new IllegalStateException("GitHub API returned HTTP 403");
                throw new AssertionError("Unexpected URL: " + url);
            }
        };
        client.mapper = new ObjectMapper();

        var status = client.readCommitActions("installation-token", "erland/got-test-repo",
                "f3689dfd3b5f011e2ba04d56e3a5f50b4bc97e69");

        assertEquals("unavailable", status.state());
        assertEquals("ACTIONS_PERMISSION_REQUIRED", status.diagnosticCode());
        assertTrue(status.diagnosticMessage().contains("Actions"));
        assertTrue(status.workflows().isEmpty());
    }

    @Test
    void keepsWorkflowRunVisibleWhenJobsEndpointFails() throws Exception {
        String sha = "f3689dfd3b5f011e2ba04d56e3a5f50b4bc97e69";
        var client = new GitHubAppClient() {
            @Override
            protected JsonNode getJson(String url, String token) {
                try {
                    if (url.contains("/actions/runs?")) return mapper.readTree("""
                            {"workflow_runs":[{"id":31258714926,"workflow_id":329441754,"path":".github/workflows/test-action.yml",
                            "head_branch":"zip-github/work-322395a5-db12-4a5f-b49f-50871147c4a9","head_sha":"f3689dfd3b5f011e2ba04d56e3a5f50b4bc97e69",
                            "name":"Test Action 4","status":"completed","conclusion":"success","event":"push",
                            "html_url":"https://github.com/erland/got-test-repo/actions/runs/31258714926",
                            "created_at":"2026-08-08T13:04:53Z","updated_at":"2026-08-08T13:05:01Z"}]}
                            """);
                    if (url.contains("/actions/runs/31258714926/jobs")) throw new IllegalStateException("GitHub API returned HTTP 502");
                    if (url.contains("/check-runs")) return mapper.readTree("{\"check_runs\":[]}");
                    throw new AssertionError("Unexpected URL: " + url);
                } catch (java.io.IOException e) { throw new IllegalStateException(e); }
            }
        };
        client.mapper = new ObjectMapper();

        var status = client.readCommitActions("installation-token", "erland/got-test-repo", sha);
        assertEquals(1, status.workflows().size());
        assertEquals(31258714926L, status.workflows().getFirst().id());
        assertTrue(status.workflows().getFirst().jobs().isEmpty());
        assertEquals("success", status.state());
    }
}
