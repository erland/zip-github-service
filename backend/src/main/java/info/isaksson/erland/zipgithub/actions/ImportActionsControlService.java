package info.isaksson.erland.zipgithub.actions;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.application.ProjectApplicationService;
import info.isaksson.erland.zipgithub.application.WorkSession;
import info.isaksson.erland.zipgithub.github.GitHubActionsControlClient;
import info.isaksson.erland.zipgithub.github.GitHubInstallationTokenProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ImportActionsControlService {
    @Inject ProjectApplicationService application;
    @Inject GitHubInstallationTokenProvider tokens;
    @Inject GitHubActionsControlClient github;
    @Inject ActionsControlPolicy policy;
    @Inject ActionsControlAuditStore audit;

    public Options options(UUID ownerUserId, UUID importId) {
        Context context = context(ownerUserId, importId, false, null, null);
        Set<String> identifiers = new LinkedHashSet<>();
        identifiers.addAll(policy.dispatchIdentifiers());
        identifiers.addAll(policy.rerunIdentifiers());
        if (identifiers.isEmpty()) return new Options(importId, context.repositoryFullName(), context.branchRef(), context.commitSha(),
                context.currentWork(), context.disabledReason(), List.of());
        String token = tokens.createInstallationToken(context.installationId());
        List<WorkflowOption> workflows = new ArrayList<>();
        for (String identifier : identifiers) {
            try {
                var workflow = github.workflow(token, context.repositoryFullName(), identifier);
                workflows.add(new WorkflowOption(identifier, workflow.id(), workflow.name(), workflow.path(), workflow.htmlUrl(),
                        "active".equalsIgnoreCase(workflow.state()) && policy.dispatchAllowed(identifier, workflow.id(), workflow.path()),
                        policy.rerunAllowed(identifier, workflow.id(), workflow.path())));
            } catch (RuntimeException ignored) {
                // Invalid/removed configured workflows remain unavailable instead of expanding the allowlist implicitly.
            }
        }
        return new Options(importId, context.repositoryFullName(), context.branchRef(), context.commitSha(),
                context.currentWork(), context.disabledReason(), List.copyOf(workflows));
    }

    public OperationResult dispatch(UUID ownerUserId, UUID importId, String workflowIdentifier, String expectedRef,
                                    String expectedCommitSha, String idempotencyKey, boolean confirmed) {
        requireConfirmed(confirmed); requireIdempotencyKey(idempotencyKey);
        Context context = context(ownerUserId, importId, true, expectedRef, expectedCommitSha);
        requireActionsWritePermission(context.installationId());
        String token = tokens.createInstallationToken(context.installationId());
        var workflow = github.workflow(token, context.repositoryFullName(), requireText(workflowIdentifier, "workflowIdentifier"));
        if (!policy.dispatchAllowed(workflowIdentifier, workflow.id(), workflow.path())) {
            throw ApiException.forbidden("WORKFLOW_NOT_ALLOWED", "This workflow is not allowed for manual dispatch.");
        }
        if (!"active".equalsIgnoreCase(workflow.state())) {
            throw ApiException.conflict("WORKFLOW_NOT_ACTIVE", "Only active workflows may be dispatched from zip-github.");
        }
        var created = audit.create(ownerUserId, context.projectId(), importId, "WORKFLOW_DISPATCH", workflowIdentifier,
                workflow.id(), null, context.branchRef(), context.commitSha(), idempotencyKey.trim());
        if (!created.created()) {
            assertSameIdempotentTarget(created.audit(), workflowIdentifier, null, context.branchRef(), context.commitSha());
            return toResult(created.audit(), true);
        }
        try {
            // Revalidate Work after winning the idempotency claim, immediately before the external side effect.
            context(ownerUserId, importId, true, expectedRef, expectedCommitSha);
            var result = github.dispatch(token, context.repositoryFullName(), workflowIdentifier, context.branchRef());
            return toResult(audit.succeed(created.audit(), workflow.id(), result.workflowRunId(),
                    result.htmlUrl() == null ? workflow.htmlUrl() : result.htmlUrl()), false);
        } catch (RuntimeException e) {
            audit.fail(created.audit(), "GITHUB_DISPATCH_FAILED");
            if (e instanceof ApiException api) throw api;
            throw ApiException.badGateway("GITHUB_DISPATCH_FAILED", "GitHub could not dispatch the workflow.");
        }
    }

    public OperationResult rerunFailed(UUID ownerUserId, UUID importId, long workflowRunId, String expectedRef,
                                       String expectedCommitSha, String idempotencyKey, boolean confirmed) {
        requireConfirmed(confirmed); requireIdempotencyKey(idempotencyKey);
        if (workflowRunId <= 0) throw ApiException.badRequest("INVALID_WORKFLOW_RUN", "workflowRunId must be positive.");
        Context context = context(ownerUserId, importId, true, expectedRef, expectedCommitSha);
        requireActionsWritePermission(context.installationId());
        String token = tokens.createInstallationToken(context.installationId());
        var run = github.workflowRun(token, context.repositoryFullName(), workflowRunId);
        if (!ActionsControlRules.exactRun(context.branchRef(), context.commitSha(), run.headBranch(), run.headSha())) {
            throw ApiException.conflict("WORKFLOW_RUN_STALE", "The workflow run does not belong to the current Work head.");
        }
        String identifier = run.workflowPath() == null || run.workflowPath().isBlank() ? Long.toString(run.workflowId()) : run.workflowPath();
        if (!policy.rerunAllowed(identifier, run.workflowId(), run.workflowPath())) {
            throw ApiException.forbidden("WORKFLOW_NOT_ALLOWED", "This workflow is not allowed for rerun.");
        }
        if (!"failure".equalsIgnoreCase(run.conclusion())) {
            throw ApiException.conflict("WORKFLOW_RERUN_NOT_FAILED", "Only failed workflow runs may be rerun from zip-github.");
        }
        var created = audit.create(ownerUserId, context.projectId(), importId, "RERUN_FAILED_JOBS", identifier,
                run.workflowId(), run.id(), context.branchRef(), context.commitSha(), idempotencyKey.trim());
        if (!created.created()) {
            assertSameIdempotentTarget(created.audit(), identifier, workflowRunId, context.branchRef(), context.commitSha());
            return toResult(created.audit(), true);
        }
        try {
            context(ownerUserId, importId, true, expectedRef, expectedCommitSha);
            var verifiedRun = github.workflowRun(token, context.repositoryFullName(), workflowRunId);
            if (!ActionsControlRules.exactRun(context.branchRef(), context.commitSha(), verifiedRun.headBranch(), verifiedRun.headSha())
                    || verifiedRun.workflowId() != run.workflowId()) {
                throw ApiException.conflict("WORKFLOW_RUN_STALE", "The workflow run changed or no longer matches the current Work head.");
            }
            var result = github.rerunFailedJobs(token, context.repositoryFullName(), workflowRunId);
            return toResult(audit.succeed(created.audit(), run.workflowId(), result.workflowRunId(), result.htmlUrl()), false);
        } catch (RuntimeException e) {
            audit.fail(created.audit(), e instanceof ApiException api ? api.code() : "GITHUB_RERUN_FAILED");
            if (e instanceof ApiException api) throw api;
            throw ApiException.badGateway("GITHUB_RERUN_FAILED", "GitHub could not rerun the failed jobs.");
        }
    }

    private Context context(UUID ownerUserId, UUID importId, boolean requireCurrent, String expectedRef, String expectedCommitSha) {
        var importResponse = application.getImport(ownerUserId, importId);
        var sources = application.deliverySources(ownerUserId, importId);
        var delivery = application.findGitDelivery(ownerUserId, importId).orElseThrow(() ->
                ApiException.conflict("GIT_DELIVERY_REQUIRED", "The import must be pushed before Actions can be controlled."));
        WorkSession work = application.activeWork(ownerUserId, importResponse.projectId()).orElse(null);
        boolean current = work != null && work.hasCommit()
                && ActionsControlRules.currentWork(importId, delivery.branchName(), delivery.commitSha(),
                work.lastImportId(), work.branchName(), work.headCommitSha());
        String reason = current ? null : "Work has moved on or is no longer active; Actions control is disabled for this import result.";
        if (requireCurrent) {
            if (!current) throw ApiException.conflict("STALE_WORK", reason);
            if (!delivery.branchName().equals(requireText(expectedRef, "expectedRef"))
                    || !delivery.commitSha().equals(requireSha(expectedCommitSha))) {
                throw ApiException.conflict("STALE_ACTIONS_VIEW", "The displayed branch/ref or commit is stale. Refresh before controlling Actions.");
            }
        }
        return new Context(importResponse.projectId(), sources.githubInstallationId(), delivery.repositoryFullName(),
                delivery.branchName(), delivery.commitSha(), current, reason);
    }

    private static void assertSameIdempotentTarget(ActionsControlAudit existing, String workflowIdentifier,
                                                   Long workflowRunId, String branchRef, String commitSha) {
        boolean sameWorkflow = existing.workflowIdentifier().equals(workflowIdentifier);
        boolean sameRun = workflowRunId == null ? existing.workflowRunId() == null : workflowRunId.equals(existing.workflowRunId());
        if (!sameWorkflow || !sameRun || !existing.branchRef().equals(branchRef) || !existing.targetCommitSha().equals(commitSha)) {
            throw ApiException.conflict("IDEMPOTENCY_KEY_REUSED", "The idempotency key is already bound to another Actions operation target.");
        }
    }

    private void requireActionsWritePermission(long installationId) {
        if (!github.hasActionsWritePermission(installationId)) {
            throw ApiException.forbidden("ACTIONS_WRITE_PERMISSION_REQUIRED",
                    "The repository GitHub App installation must grant Actions read and write permission.");
        }
    }

    private static void requireConfirmed(boolean confirmed) {
        if (!confirmed) throw ApiException.badRequest("EXPLICIT_CONFIRMATION_REQUIRED", "Explicit confirmation is required.");
    }
    private static void requireIdempotencyKey(String value) {
        String key = requireText(value, "idempotencyKey");
        if (key.length() > 100 || !key.matches("[A-Za-z0-9._:-]{8,100}"))
            throw ApiException.badRequest("INVALID_IDEMPOTENCY_KEY", "idempotencyKey has an invalid format.");
    }
    private static String requireSha(String value) {
        String sha = requireText(value, "expectedCommitSha").toLowerCase();
        if (!sha.matches("[0-9a-f]{40}")) throw ApiException.badRequest("INVALID_COMMIT_SHA", "expectedCommitSha must be a full commit SHA.");
        return sha;
    }
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw ApiException.badRequest("VALIDATION_ERROR", field + " is required.");
        return value.trim();
    }
    private static OperationResult toResult(ActionsControlAudit item, boolean replayed) {
        return new OperationResult(item.id(), item.operation(), item.status(), replayed, item.workflowIdentifier(), item.workflowId(),
                item.workflowRunId(), item.branchRef(), item.targetCommitSha(), item.githubUrl(), item.errorCode(), item.createdAt(), item.updatedAt());
    }

    public record Options(UUID importId, String repositoryFullName, String branchRef, String commitSha, boolean currentWork,
                          String disabledReason, List<WorkflowOption> workflows) {}
    public record WorkflowOption(String identifier, long workflowId, String name, String path, String htmlUrl,
                                 boolean dispatchAllowed, boolean rerunAllowed) {}
    public record OperationResult(UUID operationId, String operation, String status, boolean replayed, String workflowIdentifier,
                                  Long workflowId, Long workflowRunId, String branchRef, String targetCommitSha, String githubUrl,
                                  String errorCode, java.time.Instant createdAt, java.time.Instant updatedAt) {}
    private record Context(UUID projectId, long installationId, String repositoryFullName, String branchRef, String commitSha,
                           boolean currentWork, String disabledReason) {}
}
