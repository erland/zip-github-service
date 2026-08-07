package info.isaksson.erland.zipgithub.api.dto;

import java.util.List;

public record WorkHistoryResponse(List<WorkCommitResponse> commits, boolean githubAvailable) {}
