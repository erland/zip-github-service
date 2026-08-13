package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.WorkBranchCleanupRequest;
import info.isaksson.erland.zipgithub.github.*;
import info.isaksson.erland.zipgithub.persistence.WorkPersistenceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkBranchMaintenanceServiceTest {
    private final GitHubProjectCatalog catalog = mock(GitHubProjectCatalog.class);
    private final GitHubInstallationTokenProvider tokens = mock(GitHubInstallationTokenProvider.class);
    private final GitHubBranchClient branches = mock(GitHubBranchClient.class);
    private final GitHubPullRequestClient pullRequests = mock(GitHubPullRequestClient.class);
    private final WorkPersistenceStore workStore = mock(WorkPersistenceStore.class);
    private WorkBranchMaintenanceService service;
    private final GitHubAppClient.GitHubInstallation installation = new GitHubAppClient.GitHubInstallation(10, 1, "owner", "User", "all", null);
    private final GitHubAppClient.GitHubRepository repository = new GitHubAppClient.GitHubRepository(20, "owner/repo", false, "main", "https://github.com/owner/repo");
    private final String workBranch = "zip-github/work-123e4567-e89b-12d3-a456-426614174000";

    @BeforeEach
    void setUp() {
        service = new WorkBranchMaintenanceService();
        service.catalog = catalog;
        service.installationTokens = tokens;
        service.branches = branches;
        service.pullRequests = pullRequests;
        service.workStore = workStore;
        when(catalog.listUserInstallations("user-token")).thenReturn(List.of(installation));
        when(catalog.listUserInstallationRepositories("user-token", 10)).thenReturn(List.of(repository));
        when(tokens.createInstallationToken(10)).thenReturn("installation-token");
        when(branches.listBranches("installation-token", "owner/repo")).thenReturn(List.of(new GitHubBranchClient.Branch(workBranch, "a".repeat(40), false)));
    }

    @Test
    void safeOrphanIsDeletable() {
        var preview = service.preview(UUID.randomUUID(), "user-token");
        assertEquals(1, preview.safeToDelete());
        assertEquals("SAFE_TO_DELETE", preview.candidates().getFirst().classification());
    }

    @Test
    void activeWorkAndOpenPullRequestAreNeverDeletable() {
        when(workStore.nonTerminalBranchInUse(10, 20, workBranch)).thenReturn(true);
        assertEquals("ACTIVE_WORK", service.preview(UUID.randomUUID(), "user-token").candidates().getFirst().classification());
        reset(workStore);
        when(pullRequests.hasOpenPullRequestForHead("installation-token", "owner/repo", workBranch)).thenReturn(true);
        assertEquals("OPEN_PULL_REQUEST", service.preview(UUID.randomUUID(), "user-token").candidates().getFirst().classification());
    }

    @Test
    void protectedAndUnverifiableBranchesFailClosed() {
        when(branches.listBranches("installation-token", "owner/repo")).thenReturn(List.of(new GitHubBranchClient.Branch(workBranch, "a".repeat(40), true)));
        assertEquals("PROTECTED", service.preview(UUID.randomUUID(), "user-token").candidates().getFirst().classification());
        when(branches.listBranches("installation-token", "owner/repo")).thenReturn(List.of(new GitHubBranchClient.Branch(workBranch, "a".repeat(40), false)));
        when(pullRequests.hasOpenPullRequestForHead("installation-token", "owner/repo", workBranch)).thenThrow(new IllegalStateException("GitHub unavailable"));
        assertEquals("UNVERIFIED", service.preview(UUID.randomUUID(), "user-token").candidates().getFirst().classification());
    }

    @Test
    void cleanupRechecksAndSkipsStaleCandidate() {
        when(workStore.nonTerminalBranchInUse(10, 20, workBranch)).thenReturn(true);
        var result = service.cleanup(UUID.randomUUID(), "user-token", new WorkBranchCleanupRequest(List.of(
                new WorkBranchCleanupRequest.Target(10, 20, "owner/repo", workBranch))));
        assertEquals("SKIPPED", result.results().getFirst().status());
        verify(branches, never()).deleteBranch(anyString(), anyString(), anyString());
    }

    @Test
    void cleanupDeletesOnlyAfterFreshSafeClassification() {
        var result = service.cleanup(UUID.randomUUID(), "user-token", new WorkBranchCleanupRequest(List.of(
                new WorkBranchCleanupRequest.Target(10, 20, "owner/repo", workBranch))));
        assertEquals("DELETED", result.results().getFirst().status());
        verify(branches).deleteBranch("installation-token", "owner/repo", workBranch);
    }
}
