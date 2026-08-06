package info.isaksson.erland.zipgithub.github;

import info.isaksson.erland.zipgithub.security.CurrentUserProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/github")
@Produces(MediaType.APPLICATION_JSON)
public class GitHubResource {
    @Inject CurrentUserProvider currentUser;
    @Inject GitHubAppClient github;

    @GET
    @Path("/installations")
    public List<GitHubAppClient.GitHubInstallation> installations() {
        var session = currentUser.requireSession();
        return github.listUserInstallations(session.githubUserAccessToken());
    }

    @GET
    @Path("/installations/{installationId}/repositories")
    public List<GitHubAppClient.GitHubRepository> repositories(@PathParam("installationId") long installationId) {
        var session = currentUser.requireSession();
        GitHubInstallationAccess.requireVisible(
                installationId,
                github.listUserInstallations(session.githubUserAccessToken()));
        return github.listUserInstallationRepositories(session.githubUserAccessToken(), installationId);
    }
}
