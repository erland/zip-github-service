package info.isaksson.erland.zipgithub.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import info.isaksson.erland.zipgithub.security.FixedWindowRateLimiter;
import info.isaksson.erland.zipgithub.staging.StagingCapacityExceededException;
import info.isaksson.erland.zipgithub.staging.StagingUploadService;
import info.isaksson.erland.zipgithub.upload.UploadTooLargeException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

/**
 * Stateless Streamable-HTTP-compatible MCP endpoint.
 *
 * The first MCP surface is intentionally narrow: it can only stage one ZIP and
 * return the ordinary browser claim/review URL. It cannot choose a repository,
 * approve a plan, write Git, or create a pull request.
 */
@Path("/mcp")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class McpResource {
    private static final String LATEST_PROTOCOL = "2025-11-25";
    private static final String SERVER_DESCRIPTION =
            "Stages user-provided ZIP archives for safe review against an authorized GitHub repository. " +
            "Repository selection, review, approval and Git delivery remain in the zip-GitHub web UI.";
    private static final String SERVER_WEBSITE = "https://zip-github.apphome.one/about";
    private static final String SERVER_ICON_DATA_URI = "data:image/png;base64,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";
    private static final Set<String> SUPPORTED_PROTOCOLS =
            Set.of("2024-11-05", "2025-03-26", "2025-06-18", "2025-11-25");

    private final FixedWindowRateLimiter globalUploads = new FixedWindowRateLimiter(30, Duration.ofMinutes(1));
    private final FixedWindowRateLimiter subjectUploads = new FixedWindowRateLimiter(6, Duration.ofMinutes(1));

    @Inject ObjectMapper mapper;
    @Inject McpFileDownloadService downloads;
    @Inject StagingUploadService uploads;

    @POST
    public Response handle(JsonNode request) {
        if (request == null || !request.isObject() || !"2.0".equals(request.path("jsonrpc").asText())) {
            return json(error(null, -32600, "Invalid Request"));
        }

        JsonNode id = request.get("id");
        String method = request.path("method").asText("");
        JsonNode params = request.path("params");

        if (id == null || id.isNull()) {
            // MCP notifications intentionally receive no JSON-RPC response body.
            return Response.status(Response.Status.ACCEPTED).build();
        }

        return switch (method) {
            case "initialize" -> json(success(id, initializeResult(params)));
            case "ping" -> json(success(id, mapper.createObjectNode()));
            case "tools/list" -> json(success(id, toolsList()));
            case "tools/call" -> json(success(id, callTool(params)));
            default -> json(error(id, -32601, "Method not found"));
        };
    }

    private ObjectNode initializeResult(JsonNode params) {
        String requested = params.path("protocolVersion").asText("");
        String negotiated = SUPPORTED_PROTOCOLS.contains(requested) ? requested : LATEST_PROTOCOL;

        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", negotiated);
        result.set("capabilities", mapper.createObjectNode().set("tools",
                mapper.createObjectNode().put("listChanged", false)));
        ObjectNode serverInfo = mapper.createObjectNode()
                .put("name", "zip-github")
                .put("title", "zip-GitHub")
                .put("version", "1.0.0-rc.132")
                .put("description", SERVER_DESCRIPTION)
                .put("websiteUrl", SERVER_WEBSITE);
        if ("2025-11-25".equals(negotiated)) {
            serverInfo.set("icons", mapper.createArrayNode().add(mapper.createObjectNode()
                    .put("src", SERVER_ICON_DATA_URI)
                    .put("mimeType", "image/png")
                    .set("sizes", mapper.createArrayNode().add("64x64"))));
        }
        result.set("serverInfo", serverInfo);
        result.put("instructions",
                "Use stage_zip only to transfer a user-provided ZIP into temporary zip-github staging. " +
                "After it succeeds, give the user review_url and let the ordinary zip-github web UI handle " +
                "sign-in, repository selection, review, approval, Git delivery and pull requests.");
        return result;
    }

    private ObjectNode toolsList() {
        ArrayNode tools = mapper.createArrayNode();
        tools.add(stageZipDescriptor());
        return mapper.createObjectNode().set("tools", tools);
    }

    private ObjectNode stageZipDescriptor() {
        ObjectNode tool = mapper.createObjectNode();
        tool.put("name", "stage_zip");
        tool.put("title", "Stage ZIP for zip-github review");
        tool.put("description",
                "Transfers one user-provided ZIP into temporary zip-github staging and returns a browser review URL. " +
                "This tool does not select a GitHub repository, approve changes, commit, push, or create a pull request.");

        ObjectNode openAiFile = mapper.createObjectNode();
        openAiFile.put("type", "object");
        openAiFile.set("properties", mapper.createObjectNode()
                .set("download_url", mapper.createObjectNode().put("type", "string")));
        ((ObjectNode) openAiFile.get("properties")).set("file_id", mapper.createObjectNode().put("type", "string"));
        ((ObjectNode) openAiFile.get("properties")).set("mime_type", mapper.createObjectNode().put("type", "string"));
        ((ObjectNode) openAiFile.get("properties")).set("file_name", mapper.createObjectNode().put("type", "string"));
        openAiFile.set("required", mapper.createArrayNode().add("download_url").add("file_id"));
        openAiFile.put("additionalProperties", false);

        ObjectNode input = mapper.createObjectNode();
        input.put("type", "object");
        input.set("$defs", mapper.createObjectNode().set("OpenAIFile", openAiFile));
        input.set("properties", mapper.createObjectNode().set("file",
                mapper.createObjectNode().put("$ref", "#/$defs/OpenAIFile")));
        input.set("required", mapper.createArrayNode().add("file"));
        input.put("additionalProperties", false);
        tool.set("inputSchema", input);

        ObjectNode outputProps = mapper.createObjectNode();
        outputProps.set("staging_id", mapper.createObjectNode().put("type", "string"));
        outputProps.set("filename", mapper.createObjectNode().put("type", "string"));
        outputProps.set("size_bytes", mapper.createObjectNode().put("type", "integer"));
        outputProps.set("sha256", mapper.createObjectNode().put("type", "string"));
        outputProps.set("expires_at", mapper.createObjectNode().put("type", "string"));
        outputProps.set("review_url", mapper.createObjectNode().put("type", "string"));
        ObjectNode output = mapper.createObjectNode()
                .put("type", "object")
                .set("properties", outputProps);
        output.set("required", mapper.createArrayNode()
                .add("staging_id").add("filename").add("size_bytes")
                .add("sha256").add("expires_at").add("review_url"));
        output.put("additionalProperties", false);
        tool.set("outputSchema", output);

        tool.set("annotations", mapper.createObjectNode()
                .put("readOnlyHint", false)
                .put("destructiveHint", false)
                .put("openWorldHint", true)
                .put("idempotentHint", false));

        ObjectNode meta = mapper.createObjectNode();
        meta.set("openai/fileParams", mapper.createArrayNode().add("file"));
        meta.set("securitySchemes", mapper.createArrayNode().add(mapper.createObjectNode().put("type", "noauth")));
        meta.put("openai/toolInvocation/invoking", "Transferring ZIP…");
        meta.put("openai/toolInvocation/invoked", "ZIP ready for review");
        tool.set("_meta", meta);
        return tool;
    }

    private ObjectNode callTool(JsonNode params) {
        String name = params.path("name").asText("");
        if (!"stage_zip".equals(name)) return toolError("Unknown tool: " + name);

        String subject = params.path("_meta").path("openai/subject").asText("anonymous");
        if (!globalUploads.allow("all") || !subjectUploads.allow(subject)) {
            return toolError("Too many staging requests. Retry after a short delay.");
        }

        JsonNode file = params.path("arguments").path("file");
        String downloadUrl = file.path("download_url").asText("");
        String fileId = file.path("file_id").asText("");
        if (downloadUrl.isBlank() || fileId.isBlank()) {
            return toolError("A ChatGPT file parameter with download_url and file_id is required.");
        }

        String filename;
        try {
            filename = zipFilename(file.path("file_name").asText(""));
        } catch (IllegalArgumentException e) {
            return toolError(e.getMessage());
        }

        try (McpFileDownloadService.Download downloaded = downloads.download(URI.create(downloadUrl))) {
            var created = uploads.create(filename, downloaded.contentLength(), downloaded.body());
            ObjectNode structured = mapper.createObjectNode()
                    .put("staging_id", created.stagingId().toString())
                    .put("filename", created.originalFilename())
                    .put("size_bytes", created.sizeBytes())
                    .put("sha256", created.sha256())
                    .put("expires_at", created.expiresAt().toString())
                    .put("review_url", created.claimUrl());
            ObjectNode result = mapper.createObjectNode();
            result.set("content", mapper.createArrayNode().add(mapper.createObjectNode()
                    .put("type", "text")
                    .put("text", "ZIP staged. Open the returned review_url to continue in zip-github.")));
            result.set("structuredContent", structured);
            return result;
        } catch (McpFileDownloadService.DownloadException e) {
            return toolError(e.getMessage());
        } catch (StagingCapacityExceededException e) {
            return toolError("Staging capacity is temporarily full. Retry later.");
        } catch (UploadTooLargeException e) {
            return toolError("The ZIP exceeds the configured upload limit.");
        } catch (IllegalArgumentException e) {
            return toolError("The supplied file could not be accepted as a ZIP.");
        } catch (Exception e) {
            return toolError("The ZIP transfer failed.");
        }
    }

    static String zipFilename(String supplied) {
        String name = supplied == null ? "" : supplied.trim().replace('\\', '/');
        if (name.contains("/")) name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replace("\r", "").replace("\n", "");
        if (name.isBlank()) name = "chatgpt-upload.zip";
        if (name.length() > 255) throw new IllegalArgumentException("The ZIP filename is too long.");
        if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
            throw new IllegalArgumentException("The selected file must be a .zip archive.");
        }
        return name;
    }

    private ObjectNode toolError(String message) {
        ObjectNode result = mapper.createObjectNode();
        result.set("content", mapper.createArrayNode().add(mapper.createObjectNode()
                .put("type", "text").put("text", message)));
        result.put("isError", true);
        return result;
    }

    private ObjectNode success(JsonNode id, JsonNode result) {
        ObjectNode response = mapper.createObjectNode().put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    private ObjectNode error(JsonNode id, int code, String message) {
        ObjectNode response = mapper.createObjectNode().put("jsonrpc", "2.0");
        if (id == null) response.putNull("id"); else response.set("id", id);
        response.set("error", mapper.createObjectNode().put("code", code).put("message", message));
        return response;
    }

    private Response json(JsonNode body) {
        return Response.ok(body, MediaType.APPLICATION_JSON_TYPE).build();
    }
}
