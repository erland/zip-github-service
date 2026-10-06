package info.isaksson.erland.zipgithub.mcp;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpFileDownloadServiceTest {
    private final McpFileDownloadService service =
            new McpFileDownloadService("files.oaiusercontent.com,.example-cdn.test", HttpClient.newHttpClient());

    @Test
    void acceptsConfiguredHttpsHosts() {
        assertDoesNotThrow(() -> service.validateUri(URI.create("https://files.oaiusercontent.com/file/123?sig=x")));
        assertDoesNotThrow(() -> service.validateUri(URI.create("https://a.example-cdn.test/file.zip")));
    }

    @Test
    void rejectsNonHttpsAndUnconfiguredHosts() {
        assertThrows(McpFileDownloadService.DownloadException.class,
                () -> service.validateUri(URI.create("http://files.oaiusercontent.com/file/123")));
        assertThrows(McpFileDownloadService.DownloadException.class,
                () -> service.validateUri(URI.create("https://127.0.0.1/internal")));
        assertThrows(McpFileDownloadService.DownloadException.class,
                () -> service.validateUri(URI.create("https://files.oaiusercontent.com.evil.test/file")));
    }
}
