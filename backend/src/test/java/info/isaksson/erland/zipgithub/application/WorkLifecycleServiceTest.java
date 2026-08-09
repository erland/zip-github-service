package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.ProjectResponse;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.github.GitHubBranchClient;
import info.isaksson.erland.zipgithub.github.GitHubInstallationTokenProvider;
import info.isaksson.erland.zipgithub.github.GitHubPullRequestClient;
import info.isaksson.erland.zipgithub.persistence.ProjectPersistenceStore;
import info.isaksson.erland.zipgithub.persistence.WorkPersistenceStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkLifecycleServiceTest {
    @Test
    void newWorkIsOnlyActivatedAfterRemoteBranchReadbackMatches() {
        Fixture f = fixture();
        UUID workId = UUID.randomUUID();
        String sha = "a".repeat(40);
        when(f.branches.branchHeadSha("installation-token", "erland/repo", "main")).thenReturn(sha);
        when(f.branches.branchHeadSha(eq("installation-token"), eq("erland/repo"), startsWith("zip-github/work-"))).thenReturn(sha);
        when(f.work.findOpen(f.owner, f.project.id())).thenReturn(Optional.empty());
        when(f.work.activeBranchInUse(eq(f.owner), eq(f.project.id()), anyString())).thenReturn(false);
        when(f.work.createProvisioning(eq(f.owner), eq(f.project.id()), eq("main"), anyString(), eq(sha)))
                .thenAnswer(inv -> new WorkSession(workId, f.project.id(), f.owner, "main", inv.getArgument(3), "PROVISIONING",
                        null, sha, null, null, null, null, Instant.now(), Instant.now()));
        when(f.work.activate(eq(f.owner), eq(f.project.id()), anyString(), eq(sha)))
                .thenAnswer(inv -> new WorkSession(workId, f.project.id(), f.owner, "main", inv.getArgument(2), "ACTIVE",
                        null, sha, null, null, null, null, Instant.now(), Instant.now()));

        WorkSession created = f.service.startWork(f.owner, f.project.id(), null);
        assertEquals("ACTIVE", created.status());
        verify(f.branches).createBranch(eq("installation-token"), eq("erland/repo"), eq(created.branchName()), eq(sha));
        var order = inOrder(f.work, f.branches);
        order.verify(f.work).createProvisioning(eq(f.owner), eq(f.project.id()), eq("main"), eq(created.branchName()), eq(sha));
        order.verify(f.branches).createBranch("installation-token", "erland/repo", created.branchName(), sha);
        order.verify(f.branches).branchHeadSha("installation-token", "erland/repo", created.branchName());
        order.verify(f.work).activate(f.owner, f.project.id(), created.branchName(), sha);
    }

    @Test
    void retryRecoversProvisioningWorkInsteadOfCreatingAnotherBranch() {
        Fixture f = fixture();
        String branch = "zip-github/work-existing";
        String sha = "b".repeat(40);
        WorkSession pending = new WorkSession(UUID.randomUUID(), f.project.id(), f.owner, "main", branch, "PROVISIONING",
                null, sha, null, null, null, null, Instant.now(), Instant.now());
        when(f.work.findOpen(f.owner, f.project.id())).thenReturn(Optional.of(pending));
        when(f.branches.branchHeadSha("installation-token", "erland/repo", branch)).thenReturn(sha);
        when(f.work.activate(f.owner, f.project.id(), branch, sha)).thenReturn(new WorkSession(pending.id(), f.project.id(), f.owner,
                "main", branch, "ACTIVE", null, sha, null, null, null, null, pending.createdAt(), Instant.now()));

        WorkSession recovered = f.service.startWork(f.owner, f.project.id(), null);
        assertEquals(branch, recovered.branchName());
        verify(f.branches, never()).createBranch(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void defaultAndProtectedBranchesCannotBeResumedAsWork() {
        Fixture f = fixture();
        when(f.work.findOpen(f.owner, f.project.id())).thenReturn(Optional.empty());
        when(f.branches.listBranches("installation-token", "erland/repo"))
                .thenReturn(List.of(new GitHubBranchClient.Branch("protected-work", "c".repeat(40), true)));

        ApiException defaultBranch = assertThrows(ApiException.class, () -> f.service.startWork(f.owner, f.project.id(), "main"));
        assertEquals("DEFAULT_BRANCH_AS_WORK_FORBIDDEN", defaultBranch.code());
        ApiException protectedBranch = assertThrows(ApiException.class, () -> f.service.startWork(f.owner, f.project.id(), "protected-work"));
        assertEquals("PROTECTED_WORK_BRANCH_FORBIDDEN", protectedBranch.code());
    }


    @Test
    void mergedPullRequestClosesTheLogicalWork() {
        Fixture f = fixture();
        WorkSession open = new WorkSession(UUID.randomUUID(), f.project.id(), f.owner, "main", "zip-github/work-1", "PR_OPEN",
                "a".repeat(40), "b".repeat(40), UUID.randomUUID(), "c".repeat(64), 42L, "https://github.com/erland/repo/pull/42", Instant.now(), Instant.now());
        when(f.work.findActive(f.owner, f.project.id())).thenReturn(Optional.of(open));
        when(f.pullRequests.getPullRequest("installation-token", "erland/repo", 42L))
                .thenReturn(new GitHubPullRequestClient.GitHubPullRequest(42L, open.pullRequestUrl(), "closed", false, true, "a".repeat(40)));
        when(f.work.updatePullRequestState(f.owner, f.project.id(), "MERGED"))
                .thenReturn(new WorkSession(open.id(), open.projectId(), open.ownerUserId(), open.baseBranch(), open.branchName(), "MERGED",
                        open.headCommitSha(), open.baseCommitSha(), open.lastImportId(), open.lastPlanDigestSha256(), open.pullRequestNumber(), open.pullRequestUrl(), open.createdAt(), Instant.now()));

        assertTrue(f.service.syncWorkPullRequestState(f.owner, f.project.id()).isEmpty());
        verify(f.work).updatePullRequestState(f.owner, f.project.id(), "MERGED");
    }

    private static Fixture fixture() {
        UUID owner = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        ProjectResponse project = new ProjectResponse(projectId, "Repo", 10L, 20L, "erland/repo", true, "main", true,
                Instant.parse("2026-08-08T10:00:00Z"), Instant.parse("2026-08-08T10:00:00Z"));
        ProjectApplicationService service = new ProjectApplicationService();
        ProjectPersistenceStore projects = mock(ProjectPersistenceStore.class);
        WorkPersistenceStore work = mock(WorkPersistenceStore.class);
        GitHubInstallationTokenProvider tokens = mock(GitHubInstallationTokenProvider.class);
        GitHubBranchClient branches = mock(GitHubBranchClient.class);
        GitHubPullRequestClient pullRequests = mock(GitHubPullRequestClient.class);
        service.persistentProjects = projects;
        service.persistentWork = work;
        service.installationTokens = tokens;
        service.githubBranches = branches;
        service.githubPullRequests = pullRequests;
        when(projects.enabled()).thenReturn(true);
        when(projects.findProject(owner, projectId)).thenReturn(Optional.of(project));
        when(work.findActive(owner, projectId)).thenReturn(Optional.empty());
        when(tokens.createInstallationToken(10L)).thenReturn("installation-token");
        return new Fixture(owner, project, service, work, branches, pullRequests);
    }

    private record Fixture(UUID owner, ProjectResponse project, ProjectApplicationService service,
                           WorkPersistenceStore work, GitHubBranchClient branches, GitHubPullRequestClient pullRequests) {}
}
