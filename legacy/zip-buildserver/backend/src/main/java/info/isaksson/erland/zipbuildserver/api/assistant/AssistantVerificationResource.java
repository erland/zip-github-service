package info.isaksson.erland.zipbuildserver.api.assistant;

import info.isaksson.erland.zipbuildserver.api.run.CreateRunRequest;
import info.isaksson.erland.zipbuildserver.api.run.RunResponse;
import info.isaksson.erland.zipbuildserver.api.session.CreateSessionRequest;
import info.isaksson.erland.zipbuildserver.api.session.SessionResponse;
import info.isaksson.erland.zipbuildserver.application.VerificationSessionService;
import info.isaksson.erland.zipbuildserver.application.mapper.AssistantResponseMapper;
import info.isaksson.erland.zipbuildserver.application.run.VerificationRunService;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/api/assistant")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AssistantVerificationResource {
    private final VerificationSessionService sessionService;
    private final VerificationRunService runService;
    private final AssistantResponseMapper responseMapper;

    public AssistantVerificationResource(
            VerificationSessionService sessionService,
            VerificationRunService runService,
            AssistantResponseMapper responseMapper) {
        this.sessionService = sessionService;
        this.runService = runService;
        this.responseMapper = responseMapper;
    }

    @POST
    @Path("/verification-sessions")
    public RestResponse<AssistantSessionResponse> createSession(@Valid AssistantCreateSessionRequest request) {
        SessionResponse session = sessionService.create(new CreateSessionRequest(
                request == null ? null : request.label(),
                request == null ? null : request.retentionPolicy()));
        return RestResponse.status(RestResponse.Status.CREATED, responseMapper.toAssistantSession(session));
    }

    @POST
    @Path("/verification-sessions/{sessionId}/runs")
    public RestResponse<AssistantRunResponse> createRun(
            @PathParam("sessionId") UUID sessionId,
            @Valid AssistantCreateRunRequest request) {
        RunResponse run = runService.create(sessionId, new CreateRunRequest(
                request == null ? null : request.packageId(),
                request == null ? null : request.requestedPlanId()));
        return RestResponse.status(RestResponse.Status.CREATED, responseMapper.toAssistantRun(run, runService.summary(run.id())));
    }

    @GET
    @Path("/verification-runs/{runId}/summary")
    public AssistantRunSummaryResponse summary(@PathParam("runId") UUID runId) {
        return responseMapper.toAssistantSummary(runService.get(runId), runService.summary(runId));
    }

    @GET
    @Path("/verification-runs/{runId}/failed-log-excerpts")
    public AssistantFailedLogExcerptResponse failedLogExcerpts(@PathParam("runId") UUID runId) {
        return responseMapper.toFailedLogExcerpts(runService.get(runId));
    }
}

