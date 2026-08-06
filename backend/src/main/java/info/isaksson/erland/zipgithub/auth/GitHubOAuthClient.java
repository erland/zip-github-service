package info.isaksson.erland.zipgithub.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class GitHubOAuthClient {
    @ConfigProperty(name="zipgithub.github.client-id") String clientId;
    @ConfigProperty(name="zipgithub.github.client-secret") String clientSecret;
    @ConfigProperty(name="zipgithub.github.callback-url") String callbackUrl;
    @Inject ObjectMapper mapper;
    private final HttpClient http = HttpClient.newHttpClient();

    public URI authorizationUri(String state) {
        String query = "client_id=" + enc(clientId) + "&redirect_uri=" + enc(callbackUrl) + "&state=" + enc(state);
        return URI.create("https://github.com/login/oauth/authorize?" + query);
    }

    public GitHubUser exchangeAndLoadUser(String code) {
        try {
            String body = "client_id=" + enc(clientId) + "&client_secret=" + enc(clientSecret) + "&code=" + enc(code) + "&redirect_uri=" + enc(callbackUrl);
            HttpRequest tokenRequest = HttpRequest.newBuilder(URI.create("https://github.com/login/oauth/access_token"))
                    .header("Accept", "application/json").header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            JsonNode tokenJson = mapper.readTree(http.send(tokenRequest, HttpResponse.BodyHandlers.ofString()).body());
            String accessToken = tokenJson.path("access_token").asText(null);
            if (accessToken == null) throw new IllegalStateException("GitHub did not return an access token");
            HttpRequest userRequest = HttpRequest.newBuilder(URI.create("https://api.github.com/user"))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-GitHub-Api-Version", "2022-11-28").GET().build();
            JsonNode user = mapper.readTree(http.send(userRequest, HttpResponse.BodyHandlers.ofString()).body());
            return new GitHubUser(user.path("id").asLong(), user.path("login").asText(), user.path("avatar_url").asText(null), accessToken);
        } catch (Exception e) {
            throw new IllegalStateException("GitHub OAuth exchange failed", e);
        }
    }

    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    public record GitHubUser(long id, String login, String avatarUrl, String accessToken) {}
}
