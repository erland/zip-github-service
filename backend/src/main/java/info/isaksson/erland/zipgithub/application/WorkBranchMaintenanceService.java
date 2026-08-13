package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.*;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.github.*;
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
                    candidates.add(classify(installation.id(), repository, branch, token));
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
                var classification = classify(target.githubInstallationId(), repository, branch, token);
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

    private WorkBranchCleanupCandidateResponse classify(long installationId, GitHubAppClient.GitHubRepository repository,
                                                         GitHubBranchClient.Branch branch, String installationToken) {
        if (branch.name().equals(repository.defaultBranch())) return candidate(installationId, repository, branch, "DEFAULT_BRANCH", "Repositoryts default branch får aldrig raderas här.", false);
        if (branch.protectedBranch()) return candidate(installationId, repository, branch, "PROTECTED", "Branchen är protected på GitHub.", false);
        try {
            if (workStore.nonTerminalBranchInUse(installationId, repository.id(), branch.name()))
                return candidate(installationId, repository, branch, "ACTIVE_WORK", "Branchen används av en icke-terminal Work-session.", false);
            if (pullRequests.hasOpenPullRequestForHead(installationToken, repository.fullName(), branch.name()))
                return candidate(installationId, repository, branch, "OPEN_PULL_REQUEST", "Branchen används som head för en öppen pull request.", false);
        } catch (RuntimeException e) {
            return candidate(installationId, repository, branch, "UNVERIFIED", "Status kunde inte verifieras säkert; ingen radering tillåts.", false);
        }
        return candidate(installationId, repository, branch, "SAFE_TO_DELETE", "Ingen icke-terminal Work eller öppen pull request hittades.", true);
    }

    private static WorkBranchCleanupCandidateResponse candidate(long installationId, GitHubAppClient.GitHubRepository repository,
                                                                 GitHubBranchClient.Branch branch, String classification,
                                                                 String reason, boolean deletable) {
        return new WorkBranchCleanupCandidateResponse(installationId, repository.id(), repository.fullName(), repository.defaultBranch(),
                branch.name(), branch.commitSha(), classification, reason, deletable);
    }
}
