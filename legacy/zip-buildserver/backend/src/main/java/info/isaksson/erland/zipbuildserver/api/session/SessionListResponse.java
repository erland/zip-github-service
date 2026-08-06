package info.isaksson.erland.zipbuildserver.api.session;

import java.util.List;

public record SessionListResponse(List<SessionResponse> sessions) {
}
