package info.isaksson.erland.zipgithub.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Initializes a truly empty GitHub repository without leaving bootstrap content in its current tree. */
@ApplicationScoped
public class GitRepositoryBootstrapService {
    private static final String CREATE_MESSAGE = "Initialize empty repository for zip-GitHub";
    private static final String CLEANUP_MESSAGE = "Remove zip-GitHub bootstrap marker";
    private static final String MARKER_PATH = ".zip-github-bootstrap";
    private static final String MARKER_CONTENT = "Temporary bootstrap marker for zip-GitHub.\n";
    private static final URI API_BASE = URI.create("https://api.github.com");

    @Inject GitHubInstallationTokenProvider tokens;
    @Inject ObjectMapper mapper;

    private final HttpClient http = HttpClient.newHttpClient();

    public String bootstrapEmptyRepository(long installationId, String repositoryFullName, String defaultBranch) {
        String token = tokens.createInstallationToken(installationId);
        return bootstrapEmptyRepository(API_BASE, repositoryFullName, token, defaultBranch);
    }

    String bootstrapEmptyRepository(URI apiBase, String repositoryFullName, String token, String defaultBranch) {
        if (apiBase == null || repositoryFullName == null || repositoryFullName.isBlank()
                || token == null || token.isBlank() || defaultBranch == null || defaultBranch.isBlank() || unsafeBranch(defaultBranch)) {
            throw new IllegalArgumentException("Repository, token and a safe default branch are required.");
        }
        try {
            String encodedContent = Base64.getEncoder().encodeToString(MARKER_CONTENT.getBytes(StandardCharsets.UTF_8));
            var createPayload = mapper.createObjectNode()
                    .put("message", CREATE_MESSAGE)
                    .put("content", encodedContent);
            // GitHub explicitly recommends the Contents API to initialize an empty repository.
            // Omit branch on the first write so GitHub creates the repository's configured default branch.
            JsonNode created = sendJson(apiBase.resolve("/repos/" + repositoryFullName + "/contents/" + MARKER_PATH), token,
                    "PUT", createPayload.toString());
            String markerSha = created.path("content").path("sha").asText(null);
            if (markerSha == null || markerSha.isBlank()) {
                throw new IllegalStateException("GitHub did not return the bootstrap marker SHA.");
            }

            var deletePayload = mapper.createObjectNode()
                    .put("message", CLEANUP_MESSAGE)
                    .put("sha", markerSha)
                    .put("branch", defaultBranch);
            JsonNode deleted = sendJson(apiBase.resolve("/repos/" + repositoryFullName + "/contents/" + MARKER_PATH), token,
                    "DELETE", deletePayload.toString());
            String commitSha = deleted.path("commit").path("sha").asText(null);
            if (commitSha == null || !commitSha.matches("[0-9a-fA-F]{40,64}")) {
                throw new IllegalStateException("GitHub did not return a valid bootstrap cleanup commit SHA.");
            }
            return commitSha.toLowerCase();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize empty GitHub repository through the Contents API.", e);
        }
    }

    private JsonNode sendJson(URI uri, String token, String method, String payload) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("Content-Type", "application/json");
        if ("PUT".equals(method)) builder.PUT(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        else if ("DELETE".equals(method)) builder.method("DELETE", HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        else throw new IllegalArgumentException("Unsupported bootstrap HTTP method: " + method);
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String body = response.body() == null ? "" : response.body();
            String githubMessage = "";
            try {
                JsonNode problem = mapper.readTree(body);
                githubMessage = problem.path("message").asText("");
            } catch (Exception ignored) {
                // Keep the public diagnostic bounded and structured even for a non-JSON upstream response.
            }
            throw new GitHubContentsBootstrapException(response.statusCode(), githubMessage);
        }
        return mapper.readTree(response.body());
    }

    public static final class GitHubContentsBootstrapException extends IllegalStateException {
        private final int statusCode;
        private final String githubMessage;

        GitHubContentsBootstrapException(int statusCode, String githubMessage) {
            super("GitHub Contents bootstrap failed with HTTP " + statusCode
                    + (githubMessage == null || githubMessage.isBlank() ? "" : ": " + githubMessage));
            this.statusCode = statusCode;
            this.githubMessage = githubMessage == null ? "" : githubMessage.trim();
        }

        public int statusCode() {
            return statusCode;
        }

        public String githubMessage() {
            return githubMessage;
        }
    }

    private static boolean unsafeBranch(String branch) {
        return branch.contains("..") || branch.startsWith("/") || branch.endsWith("/") || branch.contains("\\");
    }
}
