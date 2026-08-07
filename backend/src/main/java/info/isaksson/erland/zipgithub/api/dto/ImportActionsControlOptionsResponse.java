package info.isaksson.erland.zipgithub.api.dto;

import java.util.List;
import java.util.UUID;

public record ImportActionsControlOptionsResponse(UUID importId, String repositoryFullName, String branchRef,
                                                  String commitSha, boolean currentWork, String disabledReason,
                                                  List<WorkflowOption> workflows) {
    public record WorkflowOption(String identifier, long workflowId, String name, String path, String htmlUrl,
                                 boolean dispatchAllowed, boolean rerunAllowed) {}
}
