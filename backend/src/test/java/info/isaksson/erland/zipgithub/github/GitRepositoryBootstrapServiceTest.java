package info.isaksson.erland.zipgithub.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitRepositoryBootstrapServiceTest {
    @Test
    void initializesThroughContentsApiAndImmediatelyRemovesMarker() throws Exception {
        List<String> methods = new ArrayList<>();
        List<String> bodies = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/repos/erland/repo-fleet/contents/.zip-github-bootstrap", exchange -> {
            methods.add(exchange.getRequestMethod());
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response;
            if ("PUT".equals(exchange.getRequestMethod())) {
                response = "{\"content\":{\"sha\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"},\"commit\":{\"sha\":\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\"}}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(201, response.length);
            } else {
                response = "{\"commit\":{\"sha\":\"cccccccccccccccccccccccccccccccccccccccc\"}}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
            }
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            GitRepositoryBootstrapService service = new GitRepositoryBootstrapService();
            service.mapper = new ObjectMapper();
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            String sha = service.bootstrapEmptyRepository(base, "erland/repo-fleet", "token", "main");

            assertEquals("cccccccccccccccccccccccccccccccccccccccc", sha);
            assertEquals(List.of("PUT", "DELETE"), methods);
            assertFalse(bodies.get(0).contains("\"branch\""), "first Contents write must use GitHub's configured default branch");
            assertTrue(bodies.get(1).contains("\"branch\":\"main\""));
            assertTrue(bodies.get(1).contains("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        } finally {
            server.stop(0);
        }
    }
}
