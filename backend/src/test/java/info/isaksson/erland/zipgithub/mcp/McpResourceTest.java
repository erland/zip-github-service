package info.isaksson.erland.zipgithub.mcp;

import info.isaksson.erland.zipgithub.staging.StagingUploadService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class McpResourceTest {
    @InjectMock McpFileDownloadService downloads;
    @InjectMock StagingUploadService uploads;

    @Test
    void exposesOnlyStageZipWithChatGptFileParameterMetadata() {
        given().contentType("application/json").accept("application/json")
                .body("""
                        {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
                        """)
                .when().post("/mcp")
                .then().statusCode(200)
                .body("result.tools", hasSize(1))
                .body("result.tools[0].name", equalTo("stage_zip"))
                .body("result.tools[0]._meta.'openai/fileParams'[0]", equalTo("file"))
                .body("result.tools[0].annotations.readOnlyHint", equalTo(false))
                .body("result.tools[0].annotations.destructiveHint", equalTo(false))
                .body("result.tools[0].inputSchema.'$defs'.OpenAIFile.required", containsInAnyOrder("download_url", "file_id"));
    }

    @Test
    void initializesAsToolOnlyServer() {
        given().contentType("application/json")
                .body("""
                        {"jsonrpc":"2.0","id":"init","method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}
                        """)
                .when().post("/mcp")
                .then().statusCode(200)
                .body("result.protocolVersion", equalTo("2025-11-25"))
                .body("result.capabilities.tools.listChanged", equalTo(false))
                .body("result.serverInfo.name", equalTo("zip-github"))
                .body("result.serverInfo.title", equalTo("zip-GitHub"))
                .body("result.serverInfo.version", equalTo("1.0.0-rc.132"))
                .body("result.serverInfo.description", containsString("Stages user-provided ZIP archives"))
                .body("result.serverInfo.websiteUrl", equalTo("https://zip-github.apphome.one/about"))
                .body("result.serverInfo.icons", hasSize(1))
                .body("result.serverInfo.icons[0].src", org.hamcrest.Matchers.startsWith("data:image/png;base64,"))
                .body("result.serverInfo.icons[0].mimeType", equalTo("image/png"))
                .body("result.serverInfo.icons[0].sizes[0]", equalTo("64x64"));
    }

    @Test
    void omitsIconsWhenNegotiatingAnOlderProtocolRevision() {
        given().contentType("application/json")
                .body("""
                        {"jsonrpc":"2.0","id":"init-old","method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}
                        """)
                .when().post("/mcp")
                .then().statusCode(200)
                .body("result.protocolVersion", equalTo("2025-06-18"))
                .body("result.serverInfo.icons", nullValue());
    }

    @Test
    void stageZipStreamsFileIntoExistingStagingServiceAndReturnsReviewUrl() {
        UUID id = UUID.randomUUID();
        var body = new ByteArrayInputStream("zip".getBytes(StandardCharsets.UTF_8));
        when(downloads.download(URI.create("https://files.oaiusercontent.com/file/abc")))
                .thenReturn(new McpFileDownloadService.Download(body, 3, "application/zip"));
        when(uploads.create(eq("project.zip"), eq(3L), any())).thenReturn(new StagingUploadService.CreatedStagingUpload(
                id, "project.zip", 3, "a".repeat(64), Instant.parse("2026-10-06T02:30:00Z"),
                "https://zip-github.apphome.one/staging/claim#token=opaque"));

        given().contentType("application/json")
                .body("""
                        {
                          "jsonrpc":"2.0",
                          "id":2,
                          "method":"tools/call",
                          "params":{
                            "name":"stage_zip",
                            "arguments":{"file":{
                              "download_url":"https://files.oaiusercontent.com/file/abc",
                              "file_id":"file_123",
                              "mime_type":"application/zip",
                              "file_name":"project.zip"
                            }},
                            "_meta":{"openai/subject":"subject-1"}
                          }
                        }
                        """)
                .when().post("/mcp")
                .then().statusCode(200)
                .body("result.isError", nullValue())
                .body("result.structuredContent.staging_id", equalTo(id.toString()))
                .body("result.structuredContent.review_url", org.hamcrest.Matchers.endsWith("#token=opaque"))
                .body("result.structuredContent.filename", equalTo("project.zip"));

        verify(uploads).create(eq("project.zip"), eq(3L), same(body));
    }

    @Test
    void rejectsNonZipFilenameBeforeDownloading() {
        given().contentType("application/json")
                .body("""
                        {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"stage_zip","arguments":{"file":{
                        "download_url":"https://files.oaiusercontent.com/file/abc","file_id":"file_123","file_name":"notes.txt"}}}}
                        """)
                .when().post("/mcp")
                .then().statusCode(200)
                .body("result.isError", equalTo(true))
                .body("result.content[0].text", containsString(".zip"));

        verifyNoInteractions(downloads, uploads);
    }
}
