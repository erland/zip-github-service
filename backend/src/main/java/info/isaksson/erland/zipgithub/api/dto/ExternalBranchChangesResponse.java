package info.isaksson.erland.zipgithub.api.dto;

import java.util.List;

public record ExternalBranchChangesResponse(boolean branchChanged, String previousKnownHeadSha,
                                            String reviewBaseHeadSha, List<String> changedPaths) {}
