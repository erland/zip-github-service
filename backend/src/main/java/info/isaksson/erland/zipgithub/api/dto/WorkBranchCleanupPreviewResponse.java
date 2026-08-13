package info.isaksson.erland.zipgithub.api.dto;

import java.util.List;

public record WorkBranchCleanupPreviewResponse(
        int repositoriesChecked,
        int workBranchesFound,
        int safeToDelete,
        int inUseOrProtected,
        int unverifiable,
        List<WorkBranchCleanupCandidateResponse> candidates,
        List<WorkBranchCleanupIssueResponse> issues
) {}
