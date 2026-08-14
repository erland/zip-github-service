package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.ProjectResponse;
import info.isaksson.erland.zipgithub.api.dto.WorkBranchCleanupRequest;
import info.isaksson.erland.zipgithub.github.*;
import info.isaksson.erland.zipgithub.persistence.ProjectPersistenceStore;
import info.isaksson.erland.zipgithub.persistence.WorkPersistenceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkBranchMaintenanceServiceTest {
    private final GitHubProjectCatalog catalog = mock(GitHubProjectCatalog.class);
    private final GitHubInstallationTokenProvider tokens = mock(GitHubInstallationTokenProvider.class);
    private final GitHubBranchClient branches = mock(GitHubBranchClient.class);
    private final GitHubPullRequestClient pullRequests = mock(GitHubPullRequestClient.class);
    private final WorkPersistenceStore workStore = mock(WorkPersistenceStore.class);
    private final ProjectPersistenceStore projectStore = mock(ProjectPersistenceStore.class);
    private final ProjectApplicationService projects = mock(ProjectApplicationService.class);
    private WorkBranchMaintenanceService service;
    private final GitHubAppClient.GitHubInstallation installation = new GitHubAppClient.GitHubInstallation(10, 1, "owner", "User", "all", null, "write");
    private final GitHubAppClient.GitHubRepository repository = new GitHubAppClient.GitHubRepository(20, "owner/repo", false, "main", "https://github.com/owner/repo");
    private final String workBranch = "zip-github/work-123e4567-e89b-12d3-a456-426614174000";
    private final UUID currentUser = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorkBranchMaintenanceService();
        service.catalog = catalog;
        service.installationTokens = tokens;
        service.branches = branches;
        service.pullRequests = pullRequests;
        service.workStore = workStore;
        service.projectStore = projectStore;
        service.projects = projects;
        when(catalog.listUserInstallations("user-token")).thenReturn(List.of(installation));
        when(catalog.listUserInstallationRepositories("user-token", 10)).thenReturn(List.of(repository));
        when(tokens.createInstallationToken(10)).thenReturn("installation-token");
        when(branches.listBranches("installation-token", "owner/repo")).thenReturn(List.of(new GitHubBranchClient.Branch(workBranch, "a".repeat(40), false)));
        when(workStore.findNonTerminalByRepositoryBranch(10, 20, workBranch)).thenReturn(List.of());
        when(pullRequests.findOpenPullRequestForHead("installation-token", "owner/repo", workBranch)).thenReturn(Optional.empty());
        when(projectStore.enabled()).thenReturn(true);
    }

    @Test
    void safeOrphanIsDeletable() {
        var preview = service.preview(currentUser, "user-token");
        assertEquals(1, preview.safeToDelete());
        assertEquals("SAFE_TO_DELETE", preview.candidates().getFirst().classification());
        assertEquals("https://github.com/owner/repo/tree/" + workBranch, preview.candidates().getFirst().branchUrl());
    }

    @Test
    void activeWorkAndOpenPullRequestAreNeverDeletable() {
        WorkSession active = work("ACTIVE", null, null, currentUser, UUID.randomUUID());
        when(workStore.findNonTerminalByRepositoryBranch(10, 20, workBranch)).thenReturn(List.of(active));
        assertEquals("ACTIVE_WORK", service.preview(currentUser, "user-token").candidates().getFirst().classification());

        reset(workStore);
        when(workStore.findNonTerminalByRepositoryBranch(10, 20, workBranch)).thenReturn(List.of());
        when(pullRequests.findOpenPullRequestForHead("installation-token", "owner/repo", workBranch)).thenReturn(Optional.of(
                new GitHubPullRequestClient.GitHubPullRequest(17, "https://github.com/owner/repo/pull/17", "open", false)));
        var open = service.preview(currentUser, "user-token").candidates().getFirst();
        assertEquals("OPEN_PULL_REQUEST", open.classification());
        assertEquals(17L, open.pullRequestNumber());
        assertEquals("https://github.com/owner/repo/pull/17", open.pullRequestUrl());
    }

    @Test
    void reconcilesMergedPullRequestBeforeClassifyingBranch() {
        UUID owner = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        WorkSession staleOpen = work("PR_OPEN", 31L, "https://github.com/owner/repo/pull/31", owner, projectId);
        when(workStore.findNonTerminalByRepositoryBranch(10, 20, workBranch))
                .thenReturn(List.of(staleOpen))
                .thenReturn(List.of());
        when(projects.reconcileWorkPullRequestStateStrict(owner, projectId)).thenReturn(Optional.empty());

        var candidate = service.preview(currentUser, "user-token").candidates().getFirst();

        assertEquals("SAFE_TO_DELETE", candidate.classification());
        assertTrue(candidate.deletable());
        assertEquals(31L, candidate.pullRequestNumber());
        assertEquals("https://github.com/owner/repo/pull/31", candidate.pullRequestUrl());
        verify(projects).reconcileWorkPullRequestStateStrict(owner, projectId);
    }

    @Test
    void reconciliationFailureMakesBranchUnverifiable() {
        UUID owner = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        WorkSession staleOpen = work("PR_OPEN", 31L, "https://github.com/owner/repo/pull/31", owner, projectId);
        when(workStore.findNonTerminalByRepositoryBranch(10, 20, workBranch)).thenReturn(List.of(staleOpen));
        when(projects.reconcileWorkPullRequestStateStrict(owner, projectId)).thenThrow(new IllegalStateException("GitHub unavailable"));

        var candidate = service.preview(currentUser, "user-token").candidates().getFirst();
        assertEquals("UNVERIFIED", candidate.classification());
        assertFalse(candidate.deletable());
    }

    @Test
    void onlyCurrentUsersProjectIsExposedAsInternalNavigation() {
        UUID projectId = UUID.randomUUID();
        when(projectStore.findProjectByRepository(currentUser, 10, 20)).thenReturn(Optional.of(new ProjectResponse(
                projectId, "repo", 10, 20, "owner/repo", false, "main", true, Instant.now(), Instant.now())));
        var candidate = service.preview(currentUser, "user-token").candidates().getFirst();
        assertEquals(projectId, candidate.projectId());
    }

    @Test
    void protectedAndUnverifiableBranchesFailClosed() {
        when(branches.listBranches("installation-token", "owner/repo")).thenReturn(List.of(new GitHubBranchClient.Branch(workBranch, "a".repeat(40), true)));
        assertEquals("PROTECTED", service.preview(currentUser, "user-token").candidates().getFirst().classification());
        when(branches.listBranches("installation-token", "owner/repo")).thenReturn(List.of(new GitHubBranchClient.Branch(workBranch, "a".repeat(40), false)));
        when(pullRequests.findOpenPullRequestForHead("installation-token", "owner/repo", workBranch)).thenThrow(new IllegalStateException("GitHub unavailable"));
        assertEquals("UNVERIFIED", service.preview(currentUser, "user-token").candidates().getFirst().classification());
    }

    @Test
    void cleanupRechecksAndSkipsStaleCandidate() {
        WorkSession active = work("ACTIVE", null, null, currentUser, UUID.randomUUID());
        when(workStore.findNonTerminalByRepositoryBranch(10, 20, workBranch)).thenReturn(List.of(active));
        var result = service.cleanup(currentUser, "user-token", new WorkBranchCleanupRequest(List.of(
                new WorkBranchCleanupRequest.Target(10, 20, "owner/repo", workBranch))));
        assertEquals("SKIPPED", result.results().getFirst().status());
        verify(branches, never()).deleteBranch(anyString(), anyString(), anyString());
    }

    @Test
    void cleanupDeletesOnlyAfterFreshSafeClassification() {
        var result = service.cleanup(currentUser, "user-token", new WorkBranchCleanupRequest(List.of(
                new WorkBranchCleanupRequest.Target(10, 20, "owner/repo", workBranch))));
        assertEquals("DELETED", result.results().getFirst().status());
        verify(branches).deleteBranch("installation-token", "owner/repo", workBranch);
    }

    private WorkSession work(String status, Long prNumber, String prUrl, UUID owner, UUID projectId) {
        Instant now = Instant.now();
        return new WorkSession(UUID.randomUUID(), projectId, owner, "main", workBranch, status, "a".repeat(40),
                "b".repeat(40), null, null, prNumber, prUrl, now, now);
    }
}
