package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.*;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.github.*;
import info.isaksson.erland.zipgithub.persistence.ProjectPersistenceStore;
import info.isaksson.erland.zipgithub.persistence.WorkPersistenceStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.regex.Pattern;

@ApplicationScoped
public class WorkBranchMaintenanceService {
    private static final Pattern WORK_BRANCH = Pattern.compile("^zip-github/work-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Inject GitHubProjectCatalog catalog;
    @Inject GitHubInstallationTokenProvider installationTokens;
    @Inject GitHubBranchClient branches;
    @Inject GitHubPullRequestClient pullRequests;
    @Inject WorkPersistenceStore workStore;
    @Inject ProjectPersistenceStore projectStore;
    @Inject ProjectApplicationService projects;

    public WorkBranchCleanupPreviewResponse preview(UUID userId, String userAccessToken) {
        List<WorkBranchCleanupCandidateResponse> candidates = new ArrayList<>();
        List<WorkBranchCleanupIssueResponse> issues = new ArrayList<>();
        int repositoriesChecked = 0;
        List<GitHubAppClient.GitHubInstallation> installations;
        try {
            installations = catalog.listUserInstallations(userAccessToken);
        } catch (RuntimeException e) {
            return new WorkBranchCleanupPreviewResponse(0, 0, 0, 0, 1, List.of(),
                    List.of(new WorkBranchCleanupIssueResponse("GitHub App-installationer", "Installationerna kunde inte inventeras säkert.")));
        }
        for (var installation : installations) {
            List<GitHubAppClient.GitHubRepository> repositories;
            try { repositories = catalog.listUserInstallationRepositories(userAccessToken, installation.id()); }
            catch (RuntimeException e) {
                issues.add(new WorkBranchCleanupIssueResponse("Installation " + installation.accountLogin(), "Repositorylistan kunde inte inventeras fullständigt."));
                continue;
            }
            String token;
            try { token = installationTokens.createInstallationToken(installation.id()); }
            catch (RuntimeException e) {
                issues.add(new WorkBranchCleanupIssueResponse("Installation " + installation.accountLogin(), "Installation token kunde inte skapas; inga brancher från installationen är raderbara."));
                continue;
            }
            for (var repository : repositories) {
                repositoriesChecked++;
                List<GitHubBranchClient.Branch> repositoryBranches;
                try { repositoryBranches = branches.listBranches(token, repository.fullName()); }
                catch (RuntimeException e) {
                    issues.add(new WorkBranchCleanupIssueResponse(repository.fullName(), "Branchlistan kunde inte inventeras fullständigt."));
                    continue;
                }
                for (var branch : repositoryBranches) {
                    if (!WORK_BRANCH.matcher(branch.name()).matches()) continue;
                    candidates.add(classify(userId, installation.id(), repository, branch, token));
                }
            }
        }
        candidates.sort(Comparator.comparing(WorkBranchCleanupCandidateResponse::repositoryFullName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(WorkBranchCleanupCandidateResponse::branchName));
        issues.sort(Comparator.comparing(WorkBranchCleanupIssueResponse::scope, String.CASE_INSENSITIVE_ORDER));
        int safe = (int)candidates.stream().filter(WorkBranchCleanupCandidateResponse::deletable).count();
        int unverifiableBranches = (int)candidates.stream().filter(c -> c.classification().equals("UNVERIFIED")).count();
        return new WorkBranchCleanupPreviewResponse(repositoriesChecked, candidates.size(), safe,
                candidates.size() - safe - unverifiableBranches, unverifiableBranches + issues.size(), List.copyOf(candidates), List.copyOf(issues));
    }

    public WorkBranchCleanupResultResponse cleanup(UUID userId, String userAccessToken, WorkBranchCleanupRequest request) {
        List<WorkBranchCleanupResultResponse.Result> results = new ArrayList<>();
        List<WorkBranchCleanupRequest.Target> targets = request == null || request.targets() == null ? List.of() : request.targets();
        if (targets.size() > 500) throw ApiException.badRequest("WORK_BRANCH_CLEANUP_TOO_LARGE", "At most 500 Work branches may be cleaned in one request.");
        Set<Long> installationIds = new HashSet<>();
        try { catalog.listUserInstallations(userAccessToken).forEach(i -> installationIds.add(i.id())); }
        catch (RuntimeException e) {
            for (var target : targets) results.add(new WorkBranchCleanupResultResponse.Result(
                    target == null ? "" : Objects.toString(target.repositoryFullName(), ""),
                    target == null ? "" : Objects.toString(target.branchName(), ""), "ERROR", "GitHub App-behörighet kunde inte verifieras."));
            return new WorkBranchCleanupResultResponse(List.copyOf(results));
        }

        for (var target : targets) {
            String repoName = target == null ? "" : Objects.toString(target.repositoryFullName(), "");
            String branchName = target == null ? "" : Objects.toString(target.branchName(), "");
            if (target == null || !installationIds.contains(target.githubInstallationId()) || !WORK_BRANCH.matcher(branchName).matches()) {
                results.add(new WorkBranchCleanupResultResponse.Result(repoName, branchName, "SKIPPED", "Behörighet eller branch-namespace kunde inte verifieras."));
                continue;
            }
            try {
                var repository = catalog.listUserInstallationRepositories(userAccessToken, target.githubInstallationId()).stream()
                        .filter(r -> r.id() == target.githubRepositoryId() && r.fullName().equals(repoName)).findFirst().orElse(null);
                if (repository == null) {
                    results.add(new WorkBranchCleanupResultResponse.Result(repoName, branchName, "SKIPPED", "Repositoryt är inte längre synligt för användaren."));
                    continue;
                }
                String token = installationTokens.createInstallationToken(target.githubInstallationId());
                var branch = branches.listBranches(token, repository.fullName()).stream().filter(b -> b.name().equals(branchName)).findFirst().orElse(null);
                if (branch == null) {
                    results.add(new WorkBranchCleanupResultResponse.Result(repoName, branchName, "SKIPPED", "Branchen finns inte längre."));
                    continue;
                }
                var classification = classify(userId, target.githubInstallationId(), repository, branch, token);
                if (!classification.deletable()) {
                    results.add(new WorkBranchCleanupResultResponse.Result(repoName, branchName, "SKIPPED", classification.reason()));
                    continue;
                }
                branches.deleteBranch(token, repository.fullName(), branch.name());
                results.add(new WorkBranchCleanupResultResponse.Result(repoName, branchName, "DELETED", "Branchen raderades efter förnyad säkerhetskontroll."));
            } catch (RuntimeException e) {
                results.add(new WorkBranchCleanupResultResponse.Result(repoName, branchName, "ERROR", "Säkerhetskontroll eller radering kunde inte slutföras."));
            }
        }
        return new WorkBranchCleanupResultResponse(List.copyOf(results));
    }

    private WorkBranchCleanupCandidateResponse classify(UUID currentUserId, long installationId, GitHubAppClient.GitHubRepository repository,
                                                         GitHubBranchClient.Branch branch, String installationToken) {
        CandidateLinks links = links(currentUserId, installationId, repository, branch);
        if (branch.name().equals(repository.defaultBranch())) return candidate(installationId, repository, branch, links, (WorkSession) null, "DEFAULT_BRANCH", "Repositoryts default branch får aldrig raderas här.", false);
        if (branch.protectedBranch()) return candidate(installationId, repository, branch, links, (WorkSession) null, "PROTECTED", "Branchen är protected på GitHub.", false);

        List<WorkSession> before;
        WorkSession prContext = null;
        try {
            before = workStore.findNonTerminalByRepositoryBranch(installationId, repository.id(), branch.name());
            for (WorkSession work : before) {
                if (work.pullRequestNumber() != null) {
                    if (prContext == null) prContext = work;
                    projects.reconcileWorkPullRequestStateStrict(work.ownerUserId(), work.projectId());
                }
            }

            List<WorkSession> active = workStore.findNonTerminalByRepositoryBranch(installationId, repository.id(), branch.name());
            if (!active.isEmpty()) {
                WorkSession display = active.stream().filter(w -> w.pullRequestNumber() != null).findFirst().orElse(active.getFirst());
                return candidate(installationId, repository, branch, links, display, "ACTIVE_WORK", reasonForActiveWork(display), false);
            }

            Optional<GitHubPullRequestClient.GitHubPullRequest> openPr = pullRequests.findOpenPullRequestForHead(installationToken, repository.fullName(), branch.name());
            if (openPr.isPresent()) {
                return candidate(installationId, repository, branch, links, openPr.get(), "OPEN_PULL_REQUEST", "Branchen används som head för en öppen pull request.", false);
            }
        } catch (RuntimeException e) {
            return candidate(installationId, repository, branch, links, prContext, "UNVERIFIED", "Status kunde inte verifieras säkert; ingen radering tillåts.", false);
        }
        return candidate(installationId, repository, branch, links, prContext, "SAFE_TO_DELETE", "Ingen icke-terminal Work eller öppen pull request hittades.", true);
    }

    private CandidateLinks links(UUID currentUserId, long installationId, GitHubAppClient.GitHubRepository repository, GitHubBranchClient.Branch branch) {
        UUID projectId = null;
        if (projectStore != null && projectStore.enabled()) {
            try { projectId = projectStore.findProjectByRepository(currentUserId, installationId, repository.id()).map(ProjectResponse::id).orElse(null); }
            catch (RuntimeException ignored) { projectId = null; }
        }
        String repositoryUrl = repository.htmlUrl();
        String branchUrl = repositoryUrl == null || repositoryUrl.isBlank() ? null : repositoryUrl + "/tree/" + branch.name();
        return new CandidateLinks(projectId, repositoryUrl, branchUrl);
    }

    private static String reasonForActiveWork(WorkSession work) {
        return switch (work.status()) {
            case "PROVISIONING" -> "Work-sessionen håller på att provisioneras.";
            case "PR_OPEN" -> "Work-sessionens pull request är öppen.";
            case "PR_CLOSED" -> "Work-sessionens pull request är stängd men inte mergad.";
            default -> "Branchen används av en aktiv Work-session.";
        };
    }

    private static WorkBranchCleanupCandidateResponse candidate(long installationId, GitHubAppClient.GitHubRepository repository,
                                                                 GitHubBranchClient.Branch branch, CandidateLinks links, WorkSession work,
                                                                 String classification, String reason, boolean deletable) {
        return new WorkBranchCleanupCandidateResponse(installationId, repository.id(), repository.fullName(), links.repositoryUrl(),
                links.projectId(), repository.defaultBranch(), branch.name(), links.branchUrl(), branch.commitSha(),
                work == null ? null : work.pullRequestNumber(), work == null ? null : work.pullRequestUrl(), classification, reason, deletable);
    }

    private static WorkBranchCleanupCandidateResponse candidate(long installationId, GitHubAppClient.GitHubRepository repository,
                                                                 GitHubBranchClient.Branch branch, CandidateLinks links,
                                                                 GitHubPullRequestClient.GitHubPullRequest pr,
                                                                 String classification, String reason, boolean deletable) {
        return new WorkBranchCleanupCandidateResponse(installationId, repository.id(), repository.fullName(), links.repositoryUrl(),
                links.projectId(), repository.defaultBranch(), branch.name(), links.branchUrl(), branch.commitSha(),
                pr == null ? null : pr.number(), pr == null ? null : pr.htmlUrl(), classification, reason, deletable);
    }

    private record CandidateLinks(UUID projectId, String repositoryUrl, String branchUrl) {}
}
