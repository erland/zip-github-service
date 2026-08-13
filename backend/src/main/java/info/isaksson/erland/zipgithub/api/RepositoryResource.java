package info.isaksson.erland.zipgithub.api;

import info.isaksson.erland.zipgithub.api.dto.RepositoryEntryResponse;
import info.isaksson.erland.zipgithub.api.dto.RepositoryWorkResponse;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.application.ProjectApplicationService;
import info.isaksson.erland.zipgithub.github.GitHubInstallationAccess;
import info.isaksson.erland.zipgithub.github.GitHubProjectCatalog;
import info.isaksson.erland.zipgithub.security.CurrentUserProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Path("/api/repositories")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryResource {
    @Inject CurrentUserProvider currentUser;
    @Inject GitHubProjectCatalog catalog;
    @Inject ProjectApplicationService projects;

    @GET
    public List<RepositoryEntryResponse> list() {
        var session = currentUser.requireSession();
        var installations = catalog.listUserInstallations(session.githubUserAccessToken());
        List<RepositoryEntryResponse> result = new ArrayList<>();
        for (var installation : installations) {
            for (var repository : catalog.listUserInstallationRepositories(session.githubUserAccessToken(), installation.id())) {
                String[] parts = repository.fullName().split("/", 2);
                String shortName = parts.length == 2 ? parts[1] : repository.fullName();
                var project = projects.findProjectByRepository(session.userId(), installation.id(), repository.id()).orElse(null);
                var projectId = project == null ? null : project.id();
                var latestImport = projectId == null ? null : projects.listProjectImports(session.userId(), projectId).stream().findFirst().orElse(null);
                result.add(new RepositoryEntryResponse(installation.id(), repository.id(), repository.fullName(), shortName,
                        repository.privateRepository(), repository.defaultBranch(), repository.htmlUrl(), projectId,
                        latestImport == null ? null : latestImport.sourceFilename(),
                        latestImport == null ? (project == null ? null : project.updatedAt()) : latestImport.createdAt()));
            }
        }
        return result.stream().sorted(Comparator.comparing(RepositoryEntryResponse::repositoryName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(RepositoryEntryResponse::repositoryFullName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @POST
    @Path("/{installationId}/{repositoryId}/work")
    public RepositoryWorkResponse startWork(@PathParam("installationId") long installationId,
                                            @PathParam("repositoryId") long repositoryId) {
        try {
            var session = currentUser.requireSession();
            GitHubInstallationAccess.requireVisible(installationId, catalog.listUserInstallations(session.githubUserAccessToken()));
            var project = projects.ensureProjectForRepositoryReadyForWork(session.userId(), session.githubUserAccessToken(), installationId, repositoryId);
            var work = projects.startWork(session.userId(), project.id(), null);
            return new RepositoryWorkResponse(project, ProjectResource.workResponse(work));
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw ApiException.badGateway("REPOSITORY_WORK_START_FAILED",
                    "Could not prepare the GitHub repository or start its Work branch.");
        }
    }
}
