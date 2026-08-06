package info.isaksson.erland.zipgithub.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@ApplicationScoped
public class GitHubAppClient implements GitHubProjectCatalog {
    @ConfigProperty(name = "zipgithub.github.app-id") long appId;
    @ConfigProperty(name = "zipgithub.github.app-private-key") String privateKeyPem;
    @Inject ObjectMapper mapper;

    private final HttpClient http = HttpClient.newHttpClient();

    /** Lists installations visible to the authenticated GitHub user for this GitHub App. */
    public List<GitHubInstallation> listUserInstallations(String userAccessToken) {
        JsonNode root = getJson("https://api.github.com/user/installations", userAccessToken);
        List<GitHubInstallation> result = new ArrayList<>();
        for (JsonNode item : root.path("installations")) {
            JsonNode account = item.path("account");
            result.add(new GitHubInstallation(
                    item.path("id").asLong(),
                    account.path("id").asLong(),
                    account.path("login").asText(),
                    account.path("type").asText(),
                    item.path("repository_selection").asText(),
                    item.path("html_url").asText(null)));
        }
        return List.copyOf(result);
    }

    /** Lists repositories through a short-lived installation token, never the user's OAuth token. */
    public List<GitHubRepository> listUserInstallationRepositories(String userAccessToken, long installationId) {
        JsonNode root = getJson("https://api.github.com/user/installations/" + installationId + "/repositories?per_page=100", userAccessToken);
        List<GitHubRepository> result = new ArrayList<>();
        for (JsonNode repo : root.path("repositories")) {
            result.add(new GitHubRepository(
                    repo.path("id").asLong(),
                    repo.path("full_name").asText(),
                    repo.path("private").asBoolean(),
                    repo.path("default_branch").asText(),
                    repo.path("html_url").asText()));
        }
        return List.copyOf(result);
    }


    @Override
    public boolean branchExists(String userAccessToken, String repositoryFullName, String branch) {
        try {
            String encodedBranch = java.net.URLEncoder.encode(branch, StandardCharsets.UTF_8).replace("+", "%20");
            getJson("https://api.github.com/repos/" + repositoryFullName + "/branches/" + encodedBranch, userAccessToken);
            return true;
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("HTTP 404")) return false;
            throw e;
        }
    }

    public String createInstallationToken(long installationId) {
        try {
            HttpRequest request = baseRequest(URI.create("https://api.github.com/app/installations/" + installationId + "/access_tokens"))
                    .header("Authorization", "Bearer " + createAppJwt())
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            JsonNode json = sendJson(request);
            String token = json.path("token").asText(null);
            if (token == null || token.isBlank()) throw new IllegalStateException("GitHub did not return an installation token");
            return token;
        } catch (Exception e) {
            throw new IllegalStateException("Could not create GitHub App installation token", e);
        }
    }

    String createAppJwt() {
        try {
            Instant now = Instant.now();
            String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
            String payload = base64Url("{\"iat\":" + now.minusSeconds(60).getEpochSecond()
                    + ",\"exp\":" + now.plusSeconds(540).getEpochSecond()
                    + ",\"iss\":\"" + appId + "\"}");
            String unsigned = header + "." + payload;
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(parsePrivateKey(privateKeyPem));
            signer.update(unsigned.getBytes(StandardCharsets.US_ASCII));
            return unsigned + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign GitHub App JWT", e);
        }
    }

    private JsonNode getJson(String url, String token) {
        try {
            HttpRequest request = baseRequest(URI.create(url)).header("Authorization", "Bearer " + token).GET().build();
            return sendJson(request);
        } catch (Exception e) {
            throw new IllegalStateException("GitHub API request failed", e);
        }
    }

    private HttpRequest.Builder baseRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "zip-github-service");
    }

    private JsonNode sendJson(HttpRequest request) throws Exception {
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("GitHub API returned HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }

    private static PrivateKey parsePrivateKey(String pem) throws Exception {
        String normalized = pem.replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public record GitHubInstallation(long id, long accountId, String accountLogin, String accountType,
                                     String repositorySelection, String htmlUrl) {}
    public record GitHubRepository(long id, String fullName, boolean privateRepository,
                                   String defaultBranch, String htmlUrl) {}
}
