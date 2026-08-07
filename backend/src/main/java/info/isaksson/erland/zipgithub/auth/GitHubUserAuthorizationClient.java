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
public class GitHubUserAuthorizationClient {
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
            HttpResponse<String> tokenResponse = http.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            if (tokenResponse.statusCode() < 200 || tokenResponse.statusCode() >= 300) {
                throw new IllegalStateException("GitHub App user token exchange returned HTTP " + tokenResponse.statusCode());
            }
            JsonNode tokenJson = mapper.readTree(tokenResponse.body());
            String accessToken = tokenJson.path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                String error = tokenJson.path("error_description").asText(tokenJson.path("error").asText("unknown error"));
                throw new IllegalStateException("GitHub App did not return a user access token: " + error);
            }
            HttpRequest userRequest = HttpRequest.newBuilder(URI.create("https://api.github.com/user"))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-GitHub-Api-Version", "2022-11-28").GET().build();
            HttpResponse<String> userResponse = http.send(userRequest, HttpResponse.BodyHandlers.ofString());
            if (userResponse.statusCode() < 200 || userResponse.statusCode() >= 300) {
                throw new IllegalStateException("GitHub App user lookup returned HTTP " + userResponse.statusCode());
            }
            JsonNode user = mapper.readTree(userResponse.body());
            return new GitHubUser(user.path("id").asLong(), user.path("login").asText(),
                    user.path("name").isNull() ? null : user.path("name").asText(null),
                    user.path("email").isNull() ? null : user.path("email").asText(null),
                    user.path("avatar_url").asText(null), accessToken);
        } catch (Exception e) {
            throw new IllegalStateException("GitHub App user authorization exchange failed", e);
        }
    }

    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    public record GitHubUser(long id, String login, String name, String email, String avatarUrl, String accessToken) {}
}
