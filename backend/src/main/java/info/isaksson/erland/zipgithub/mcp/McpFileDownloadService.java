package info.isaksson.erland.zipgithub.mcp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Downloads host-provided MCP file parameters without becoming a general URL fetcher.
 *
 * Only HTTPS URLs on explicitly configured hosts are accepted. Redirects are
 * followed manually and every redirect target is checked against the same
 * allowlist before a connection is attempted.
 */
@ApplicationScoped
public class McpFileDownloadService {
    private static final int MAX_REDIRECTS = 3;

    private final List<String> allowedHosts;
    private final HttpClient client;

    @Inject
    public McpFileDownloadService(
            @ConfigProperty(name = "zipgithub.mcp.file-download-hosts", defaultValue = ".oaiusercontent.com")
            String allowedHosts) {
        this(allowedHosts, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    McpFileDownloadService(String allowedHosts, HttpClient client) {
        this.allowedHosts = Arrays.stream(allowedHosts.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        if (this.allowedHosts.isEmpty()) throw new IllegalArgumentException("At least one MCP file download host is required.");
        this.client = client;
    }

    public Download download(URI uri) {
        URI current = uri;
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            validateUri(current);
            HttpRequest request = HttpRequest.newBuilder(current)
                    .timeout(Duration.ofSeconds(90))
                    .header("User-Agent", "zip-github-mcp/1")
                    .GET()
                    .build();
            try {
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    response.body().close();
                    if (redirect == MAX_REDIRECTS) throw new DownloadException("The file download redirected too many times.");
                    String location = response.headers().firstValue("location")
                            .orElseThrow(() -> new DownloadException("The file download returned an invalid redirect."));
                    current = current.resolve(location);
                    continue;
                }
                if (status != 200) {
                    response.body().close();
                    throw new DownloadException("The temporary file download URL could not be read (HTTP " + status + ").");
                }
                long length = response.headers().firstValueAsLong("content-length").orElse(-1);
                String contentType = response.headers().firstValue("content-type").orElse(null);
                return new Download(response.body(), length, contentType);
            } catch (IOException e) {
                throw new DownloadException("The temporary file download failed.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DownloadException("The temporary file download was interrupted.", e);
            }
        }
        throw new DownloadException("The temporary file download failed.");
    }

    void validateUri(URI uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new DownloadException("Only HTTPS file download URLs are accepted.");
        }
        if (uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new DownloadException("The file download URL is not allowed.");
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        boolean allowed = allowedHosts.stream().anyMatch(rule -> {
            if (rule.startsWith(".")) {
                String suffix = rule.substring(1);
                return host.equals(suffix) || host.endsWith("." + suffix);
            }
            return host.equals(rule);
        });
        if (!allowed) throw new DownloadException("The file download host is not allowed.");
    }

    public record Download(InputStream body, long contentLength, String contentType) implements AutoCloseable {
        @Override public void close() throws IOException { body.close(); }
    }

    public static class DownloadException extends RuntimeException {
        public DownloadException(String message) { super(message); }
        public DownloadException(String message, Throwable cause) { super(message, cause); }
    }
}
