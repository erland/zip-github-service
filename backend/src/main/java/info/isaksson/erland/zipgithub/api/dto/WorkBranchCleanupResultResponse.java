package info.isaksson.erland.zipgithub.api.dto;

import java.util.List;

public record WorkBranchCleanupResultResponse(List<Result> results) {
    public record Result(String repositoryFullName, String branchName, String status, String reason) {}
}
