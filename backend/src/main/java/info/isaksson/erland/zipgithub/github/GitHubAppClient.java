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
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GitHubAppClient implements GitHubProjectCatalog, GitHubInstallationTokenProvider, GitHubPullRequestClient, GitHubCheckStatusClient, GitHubActionsClient, GitHubActionsDetailsClient, GitHubActionsControlClient, GitHubCommitHistoryClient, GitHubBranchClient {
    @ConfigProperty(name = "zipgithub.github.app-id") long appId;
    @ConfigProperty(name = "zipgithub.github.app-private-key") Optional<String> privateKeyPem;
    @Inject ObjectMapper mapper;

    private final HttpClient http = HttpClient.newHttpClient();

    /** Lists installations visible to the authenticated GitHub user for this GitHub App. */
    public List<GitHubInstallation> listUserInstallations(String userAccessToken) {
        List<GitHubInstallation> result = new ArrayList<>();
        for (int page = 1; page <= 10; page++) {
            JsonNode root = getJson("https://api.github.com/user/installations?per_page=100&page=" + page, userAccessToken);
            JsonNode items = root.path("installations");
            for (JsonNode item : items) {
                JsonNode account = item.path("account");
                result.add(new GitHubInstallation(
                        item.path("id").asLong(),
                        account.path("id").asLong(),
                        account.path("login").asText(),
                        account.path("type").asText(),
                        item.path("repository_selection").asText(),
                        item.path("html_url").asText(null),
                        item.path("permissions").path("contents").asText("")));
            }
            if (!items.isArray() || items.size() < 100) return List.copyOf(result);
        }
        throw new IllegalStateException("Too many GitHub App installations to enumerate safely");
    }

    /** Lists repositories visible to the user for one installation, with bounded complete pagination. */
    public List<GitHubRepository> listUserInstallationRepositories(String userAccessToken, long installationId) {
        List<GitHubRepository> result = new ArrayList<>();
        for (int page = 1; page <= 20; page++) {
            JsonNode root = getJson("https://api.github.com/user/installations/" + installationId + "/repositories?per_page=100&page=" + page, userAccessToken);
            JsonNode repositories = root.path("repositories");
            for (JsonNode repo : repositories) {
                result.add(new GitHubRepository(
                        repo.path("id").asLong(),
                        repo.path("full_name").asText(),
                        repo.path("private").asBoolean(),
                        repo.path("default_branch").isTextual() ? repo.path("default_branch").asText() : "",
                        repo.path("html_url").asText()));
            }
            if (!repositories.isArray() || repositories.size() < 100) return List.copyOf(result);
        }
        throw new IllegalStateException("Too many installation repositories to enumerate safely");
    }


    @Override
    public GitHubPullRequest createDraftPullRequest(String accessToken, String repositoryFullName,
                                                     String title, String headBranch, String baseBranch, String body) {
        try {
            String payload = mapper.createObjectNode()
                    .put("title", title)
                    .put("head", headBranch)
                    .put("base", baseBranch)
                    .put("body", body)
                    .put("draft", true)
                    .toString();
            HttpRequest request = baseRequest(URI.create("https://api.github.com/repos/" + repositoryFullName + "/pulls"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            JsonNode json = sendJson(request);
            return new GitHubPullRequest(json.path("number").asLong(), json.path("html_url").asText(),
                    json.path("state").asText(), json.path("draft").asBoolean(), json.path("merged").asBoolean(false),
                    json.path("head").path("sha").asText(null));
        } catch (Exception e) {
            throw new IllegalStateException("Could not create draft pull request", e);
        }
    }


    @Override
    public Optional<GitHubPullRequest> findOpenPullRequest(String accessToken, String repositoryFullName,
                                                           String headBranch, String baseBranch) {
        try {
            String head = java.net.URLEncoder.encode(repositoryFullName.split("/", 2)[0] + ":" + headBranch, StandardCharsets.UTF_8);
            String base = java.net.URLEncoder.encode(baseBranch, StandardCharsets.UTF_8);
            JsonNode root = getJson("https://api.github.com/repos/" + repositoryFullName
                    + "/pulls?state=open&head=" + head + "&base=" + base + "&per_page=10", accessToken);
            for (JsonNode json : root) {
                return Optional.of(new GitHubPullRequest(json.path("number").asLong(), json.path("html_url").asText(),
                        json.path("state").asText(), json.path("draft").asBoolean(), json.path("merged").asBoolean(false),
                        json.path("head").path("sha").asText(null)));
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalStateException("Could not search for an existing pull request", e);
        }
    }



    @Override
    public Optional<GitHubPullRequest> findOpenPullRequestForHead(String accessToken, String repositoryFullName, String headBranch) {
        try {
            String owner = repositoryFullName.split("/", 2)[0];
            String head = java.net.URLEncoder.encode(owner + ":" + headBranch, StandardCharsets.UTF_8);
            JsonNode root = getJson("https://api.github.com/repos/" + repositoryFullName
                    + "/pulls?state=open&head=" + head + "&per_page=1", accessToken);
            if (root.isArray() && root.size() > 0) {
                JsonNode json = root.get(0);
                return Optional.of(new GitHubPullRequest(json.path("number").asLong(), json.path("html_url").asText(),
                        json.path("state").asText(), json.path("draft").asBoolean(), json.path("merged").asBoolean(false),
                        json.path("head").path("sha").asText(null)));
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalStateException("Could not inspect open pull requests for branch", e);
        }
    }

    @Override
    public boolean hasOpenPullRequestForHead(String accessToken, String repositoryFullName, String headBranch) {
        return findOpenPullRequestForHead(accessToken, repositoryFullName, headBranch).isPresent();
    }

    @Override
    public GitHubPullRequest getPullRequest(String accessToken, String repositoryFullName, long pullRequestNumber) {
        try {
            JsonNode json = getJson("https://api.github.com/repos/" + repositoryFullName + "/pulls/" + pullRequestNumber, accessToken);
            return new GitHubPullRequest(json.path("number").asLong(), json.path("html_url").asText(),
                    json.path("state").asText(), json.path("draft").asBoolean(), json.path("merged").asBoolean(false),
                    json.path("head").path("sha").asText(null));
        } catch (Exception e) {
            throw new IllegalStateException("Could not read pull request", e);
        }
    }


    @Override
    public List<GitHubCommitHistoryClient.Commit> listBranchCommits(String installationToken, String repositoryFullName, String branch, int limit) {
        try {
            int pageSize = Math.max(1, Math.min(limit, 100));
            String encodedBranch = java.net.URLEncoder.encode(branch, StandardCharsets.UTF_8).replace("+", "%20");
            JsonNode root = getJson("https://api.github.com/repos/" + repositoryFullName
                    + "/commits?sha=" + encodedBranch + "&per_page=" + pageSize, installationToken);
            List<GitHubCommitHistoryClient.Commit> result = new ArrayList<>();
            for (JsonNode item : root) {
                JsonNode commit = item.path("commit");
                JsonNode author = commit.path("author");
                String date = author.path("date").asText(null);
                result.add(new GitHubCommitHistoryClient.Commit(
                        item.path("sha").asText(),
                        commit.path("message").asText(),
                        author.path("name").asText("Unknown author"),
                        author.path("email").asText(""),
                        date == null || date.isBlank() ? Instant.EPOCH : Instant.parse(date),
                        item.path("html_url").asText(null)));
            }
            return List.copyOf(result);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read branch commit history", e);
        }
    }

    @Override
    public GitHubCheckStatus readCommitChecks(String installationToken, String repositoryFullName, String commitSha) {
        String detailsUrl = "https://github.com/" + repositoryFullName + "/commit/" + commitSha + "/checks";
        try {
            JsonNode root = getJson("https://api.github.com/repos/" + repositoryFullName
                    + "/commits/" + commitSha + "/check-runs?per_page=100", installationToken);
            List<GitHubCheckStatusAggregator.Run> runs = new ArrayList<>();
            for (JsonNode run : root.path("check_runs")) {
                runs.add(new GitHubCheckStatusAggregator.Run(run.path("status").asText(),
                        run.path("conclusion").asText("")));
            }
            return GitHubCheckStatusAggregator.aggregate(runs, detailsUrl);
        } catch (Exception e) {
            return new GitHubCheckStatus("unavailable", true, 0, 0, 0, 0, 0, detailsUrl);
        }
    }


    @Override
    public GitHubActionsStatus readCommitActions(String installationToken, String repositoryFullName, String commitSha) {
        String detailsUrl = "https://github.com/" + repositoryFullName + "/commit/" + commitSha + "/checks";
        try {
            String encodedSha = java.net.URLEncoder.encode(commitSha, StandardCharsets.UTF_8);
            JsonNode runsRoot = getJson("https://api.github.com/repos/" + repositoryFullName
                    + "/actions/runs?head_sha=" + encodedSha + "&per_page=10", installationToken);
            List<GitHubActionsClient.WorkflowRun> workflows = new ArrayList<>();
            List<GitHubActionsStatusMapper.State> aggregateStates = new ArrayList<>();
            for (JsonNode run : runsRoot.path("workflow_runs")) {
                if (!commitSha.equals(run.path("head_sha").asText())) continue;
                long runId = run.path("id").asLong();
                var runState = GitHubActionsStatusMapper.map(run.path("status").asText(), run.path("conclusion").asText(""));
                aggregateStates.add(runState);
                List<GitHubActionsClient.Job> jobs = new ArrayList<>();
                try {
                    JsonNode jobsRoot = getJson("https://api.github.com/repos/" + repositoryFullName
                            + "/actions/runs/" + runId + "/jobs?per_page=50", installationToken);
                    for (JsonNode job : jobsRoot.path("jobs")) {
                        var jobState = GitHubActionsStatusMapper.map(job.path("status").asText(), job.path("conclusion").asText(""));
                        jobs.add(new GitHubActionsClient.Job(job.path("id").asLong(), job.path("name").asText("Unnamed job"),
                                jobState.value(), jobState.terminal(), job.path("html_url").asText(null),
                                parseInstant(job.path("started_at").asText(null)), parseInstant(job.path("completed_at").asText(null))));
                    }
                } catch (RuntimeException ignored) {
                    // The run itself is still useful and must remain visible if the jobs endpoint is temporarily unavailable.
                }
                workflows.add(new GitHubActionsClient.WorkflowRun(runId, run.path("workflow_id").asLong(), run.path("path").asText(""),
                        run.path("head_branch").asText(""), run.path("head_sha").asText(""), run.path("name").asText("Unnamed workflow"),
                        runState.value(), runState.terminal(), run.path("event").asText(""), run.path("html_url").asText(null),
                        parseInstant(run.path("created_at").asText(null)), parseInstant(run.path("updated_at").asText(null)), List.copyOf(jobs)));
            }

            List<GitHubActionsClient.CheckRun> checks = new ArrayList<>();
            try {
                JsonNode checksRoot = getJson("https://api.github.com/repos/" + repositoryFullName
                        + "/commits/" + commitSha + "/check-runs?per_page=50", installationToken);
                for (JsonNode check : checksRoot.path("check_runs")) {
                    var checkState = GitHubActionsStatusMapper.map(check.path("status").asText(), check.path("conclusion").asText(""));
                    aggregateStates.add(checkState);
                    checks.add(new GitHubActionsClient.CheckRun(check.path("id").asLong(), check.path("name").asText("Unnamed check"),
                            checkState.value(), checkState.terminal(), check.path("html_url").asText(null),
                            check.path("app").path("name").asText(""), parseInstant(check.path("started_at").asText(null)),
                            parseInstant(check.path("completed_at").asText(null))));
                }
            } catch (RuntimeException ignored) {
                // Workflow runs are independently useful. A Checks permission/transient failure must not hide them.
            }
            int itemCount = workflows.size() + checks.size();
            String state = GitHubActionsStatusMapper.aggregate(true, itemCount, aggregateStates);
            boolean terminal = !workflows.isEmpty() && aggregateStates.stream().allMatch(GitHubActionsStatusMapper.State::terminal);
            return new GitHubActionsStatus(state, terminal, detailsUrl, List.copyOf(workflows), List.copyOf(checks), null, null);
        } catch (Exception e) {
            String diagnosticCode = containsHttpStatus(e, 403) ? "ACTIONS_PERMISSION_REQUIRED" : "GITHUB_ACTIONS_UNAVAILABLE";
            String diagnosticMessage = containsHttpStatus(e, 403)
                    ? "GitHub App-installationen saknar behörighet att läsa Actions för repositoryt. Kontrollera Repository permissions -> Actions och godkänn eventuella nya App-behörigheter för installationen."
                    : "GitHub Actions kunde inte läsas just nu. Försök uppdatera status eller öppna körningarna på GitHub.";
            return new GitHubActionsStatus("unavailable", false, detailsUrl, List.of(), List.of(), diagnosticCode, diagnosticMessage);
        }
    }

    @Override
    public GitHubActionsDetails readCommitActionDetails(String installationToken, String repositoryFullName, String commitSha) {
        String detailsUrl = "https://github.com/" + repositoryFullName + "/commit/" + commitSha + "/checks";
        final int maxRuns = 10;
        final int maxArtifacts = 20;
        final int maxFailures = 3;
        try {
            String encodedSha = java.net.URLEncoder.encode(commitSha, StandardCharsets.UTF_8);
            JsonNode runsRoot = getJson("https://api.github.com/repos/" + repositoryFullName
                    + "/actions/runs?head_sha=" + encodedSha + "&per_page=" + maxRuns, installationToken);
            List<GitHubActionsDetailsClient.Artifact> artifacts = new ArrayList<>();
            List<GitHubActionsDetailsClient.FailureExcerpt> failures = new ArrayList<>();

            for (JsonNode run : runsRoot.path("workflow_runs")) {
                if (!commitSha.equals(run.path("head_sha").asText())) continue;
                long runId = run.path("id").asLong();
                String workflowName = run.path("name").asText("Unnamed workflow");
                String runUrl = run.path("html_url").asText("https://github.com/" + repositoryFullName + "/actions/runs/" + runId);

                if (artifacts.size() < maxArtifacts) {
                    try {
                        JsonNode artifactRoot = getJson("https://api.github.com/repos/" + repositoryFullName
                                + "/actions/runs/" + runId + "/artifacts?per_page=" + Math.min(100, maxArtifacts), installationToken);
                        for (JsonNode artifact : artifactRoot.path("artifacts")) {
                            if (artifacts.size() >= maxArtifacts) break;
                            artifacts.add(new GitHubActionsDetailsClient.Artifact(
                                    artifact.path("id").asLong(), artifact.path("name").asText("Unnamed artifact"),
                                    artifact.path("size_in_bytes").asLong(), artifact.path("expired").asBoolean(false),
                                    parseInstant(artifact.path("created_at").asText(null)), parseInstant(artifact.path("expires_at").asText(null)),
                                    runId, workflowName, runUrl));
                        }
                    } catch (Exception ignored) {
                        // A missing/disabled artifact endpoint must not hide the remaining Actions result.
                    }
                }

                if (failures.size() >= maxFailures || !"failure".equals(run.path("conclusion").asText(""))) continue;
                try {
                    JsonNode jobsRoot = getJson("https://api.github.com/repos/" + repositoryFullName
                            + "/actions/runs/" + runId + "/jobs?per_page=50", installationToken);
                    for (JsonNode job : jobsRoot.path("jobs")) {
                        if (failures.size() >= maxFailures) break;
                        if (!"failure".equals(job.path("conclusion").asText(""))) continue;
                        long jobId = job.path("id").asLong();
                        String stepName = "Failed step";
                        for (JsonNode step : job.path("steps")) {
                            if ("failure".equals(step.path("conclusion").asText(""))) {
                                stepName = step.path("name").asText(stepName);
                                break;
                            }
                        }
                        BoundedJobLog log = downloadBoundedJobLog(installationToken, repositoryFullName, jobId, 128 * 1024);
                        var condensed = ActionLogCondensor.condense(log.text());
                        var contextLines = ActionLogCondensor.context(log.text(), 40, 12);
                        var jobLogLines = ActionLogCondensor.sanitizedLines(log.text(), 1600);
                        failures.add(new GitHubActionsDetailsClient.FailureExcerpt(runId, workflowName, jobId,
                                job.path("name").asText("Unnamed job"), stepName, condensed.tool(), condensed.lines(),
                                contextLines, jobLogLines, log.truncated(), job.path("html_url").asText(runUrl)));
                    }
                } catch (Exception ignored) {
                    // Log summarization is optional and bounded; GitHub remains available through the run URL.
                }
            }
            return new GitHubActionsDetails(detailsUrl, List.copyOf(artifacts), List.copyOf(failures));
        } catch (Exception e) {
            return new GitHubActionsDetails(detailsUrl, List.of(), List.of());
        }
    }

    private BoundedJobLog downloadBoundedJobLog(String installationToken, String repositoryFullName, long jobId, int maxBytes) throws Exception {
        HttpRequest request = baseRequest(URI.create("https://api.github.com/repos/" + repositoryFullName
                + "/actions/jobs/" + jobId + "/logs"))
                .header("Authorization", "Bearer " + installationToken).GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 302 || response.statusCode() == 301 || response.statusCode() == 307 || response.statusCode() == 308) {
            response.body().close();
            String location = response.headers().firstValue("Location")
                    .orElseThrow(() -> new IllegalStateException("GitHub log redirect had no Location header"));
            URI target = URI.create(location);
            String host = target.getHost() == null ? "" : target.getHost().toLowerCase();
            if (!"https".equalsIgnoreCase(target.getScheme())
                    || !(host.equals("github.com") || host.equals("api.github.com") || host.endsWith(".githubusercontent.com") || host.endsWith(".blob.core.windows.net"))) {
                throw new IllegalStateException("GitHub log redirect target was not trusted");
            }
            HttpRequest redirected = HttpRequest.newBuilder(target).header("User-Agent", "zip-github-service").GET().build();
            response = http.send(redirected, HttpResponse.BodyHandlers.ofInputStream());
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IllegalStateException("GitHub job log returned HTTP " + response.statusCode());
        }
        try (InputStream input = response.body()) {
            byte[] bytes = input.readNBytes(maxBytes + 1);
            int length = Math.min(bytes.length, maxBytes);
            return new BoundedJobLog(new String(bytes, 0, length, StandardCharsets.UTF_8), bytes.length > maxBytes);
        }
    }

    private record BoundedJobLog(String text, boolean truncated) {}


    @Override
    public boolean hasActionsWritePermission(long installationId) {
        JsonNode json = getJson("https://api.github.com/app/installations/" + installationId, createAppJwt());
        return "write".equalsIgnoreCase(json.path("permissions").path("actions").asText(""));
    }

    @Override
    public GitHubActionsControlClient.Workflow workflow(String installationToken, String repositoryFullName, String workflowIdentifier) {
        try {
            String encoded = java.net.URLEncoder.encode(workflowApiIdentifier(workflowIdentifier), StandardCharsets.UTF_8).replace("+", "%20");
            JsonNode json = getJson("https://api.github.com/repos/" + repositoryFullName + "/actions/workflows/" + encoded, installationToken);
            return new GitHubActionsControlClient.Workflow(json.path("id").asLong(), json.path("name").asText("Unnamed workflow"),
                    json.path("path").asText(""), json.path("state").asText(""), json.path("html_url").asText(null));
        } catch (Exception e) {
            throw new IllegalStateException("Could not read workflow", e);
        }
    }

    @Override
    public GitHubActionsControlClient.WorkflowRun workflowRun(String installationToken, String repositoryFullName, long runId) {
        try {
            JsonNode json = getJson("https://api.github.com/repos/" + repositoryFullName + "/actions/runs/" + runId, installationToken);
            return new GitHubActionsControlClient.WorkflowRun(json.path("id").asLong(), json.path("workflow_id").asLong(),
                    json.path("path").asText(""), json.path("name").asText("Unnamed workflow"), json.path("head_sha").asText(""),
                    json.path("head_branch").asText(""), json.path("status").asText(""), json.path("conclusion").asText(""),
                    json.path("html_url").asText(null));
        } catch (Exception e) {
            throw new IllegalStateException("Could not read workflow run", e);
        }
    }

    @Override
    public GitHubActionsControlClient.DispatchResult dispatch(String installationToken, String repositoryFullName,
                                                               String workflowIdentifier, String ref) {
        try {
            String encoded = java.net.URLEncoder.encode(workflowApiIdentifier(workflowIdentifier), StandardCharsets.UTF_8).replace("+", "%20");
            String payload = mapper.createObjectNode().put("ref", ref).toString();
            HttpRequest request = baseRequest(URI.create("https://api.github.com/repos/" + repositoryFullName
                    + "/actions/workflows/" + encoded + "/dispatches"))
                    .header("Authorization", "Bearer " + installationToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("GitHub API returned HTTP " + response.statusCode());
            if (response.body() == null || response.body().isBlank())
                return new GitHubActionsControlClient.DispatchResult(null, "https://github.com/" + repositoryFullName + "/actions");
            JsonNode json = mapper.readTree(response.body());
            Long id = json.path("workflow_run_id").isNumber() ? json.path("workflow_run_id").asLong() : null;
            return new GitHubActionsControlClient.DispatchResult(id, json.path("html_url").asText("https://github.com/" + repositoryFullName + "/actions"));
        } catch (Exception e) {
            throw new IllegalStateException("Could not dispatch workflow", e);
        }
    }

    @Override
    public GitHubActionsControlClient.RerunResult rerunFailedJobs(String installationToken, String repositoryFullName, long runId) {
        try {
            HttpRequest request = baseRequest(URI.create("https://api.github.com/repos/" + repositoryFullName
                    + "/actions/runs/" + runId + "/rerun-failed-jobs"))
                    .header("Authorization", "Bearer " + installationToken)
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("GitHub API returned HTTP " + response.statusCode());
            return new GitHubActionsControlClient.RerunResult(runId, "https://github.com/" + repositoryFullName + "/actions/runs/" + runId);
        } catch (Exception e) {
            throw new IllegalStateException("Could not rerun failed workflow jobs", e);
        }
    }


    @Override
    public List<GitHubBranchClient.Branch> listBranches(String installationToken, String repositoryFullName) {
        try {
            List<GitHubBranchClient.Branch> result = new ArrayList<>();
            for (int page = 1; page <= 20; page++) {
                JsonNode root = getJson("https://api.github.com/repos/" + repositoryFullName + "/branches?per_page=100&page=" + page, installationToken);
                for (JsonNode item : root) {
                    result.add(new GitHubBranchClient.Branch(item.path("name").asText(), item.path("commit").path("sha").asText(), item.path("protected").asBoolean(false)));
                }
                if (!root.isArray() || root.size() < 100) return List.copyOf(result);
            }
            throw new IllegalStateException("Too many repository branches to enumerate safely");
        } catch (Exception e) {
            throw new IllegalStateException("Could not list repository branches", e);
        }
    }

    @Override
    public String branchHeadSha(String installationToken, String repositoryFullName, String branchName) {
        try {
            String encoded = java.net.URLEncoder.encode(branchName, StandardCharsets.UTF_8).replace("+", "%20");
            JsonNode json = getJson("https://api.github.com/repos/" + repositoryFullName + "/git/ref/heads/" + encoded, installationToken);
            String sha = json.path("object").path("sha").asText(null);
            if (sha == null || !sha.matches("[0-9a-fA-F]{40,64}")) throw new IllegalStateException("GitHub returned an invalid branch SHA");
            return sha.toLowerCase();
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve repository branch", e);
        }
    }

    @Override
    public java.util.Set<String> changedPaths(String installationToken, String repositoryFullName, String baseCommitSha, String headCommitSha) {
        if (baseCommitSha == null || headCommitSha == null || baseCommitSha.equalsIgnoreCase(headCommitSha)) return java.util.Set.of();
        try {
            JsonNode root = getJson("https://api.github.com/repos/" + repositoryFullName + "/compare/" + baseCommitSha + "..." + headCommitSha + "?per_page=100", installationToken);
            java.util.Set<String> paths = new java.util.TreeSet<>();
            for (JsonNode file : root.path("files")) {
                String name = file.path("filename").asText(null);
                if (name != null && !name.isBlank()) paths.add(name);
                String previous = file.path("previous_filename").asText(null);
                if (previous != null && !previous.isBlank()) paths.add(previous);
            }
            return java.util.Set.copyOf(paths);
        } catch (Exception e) {
            throw new IllegalStateException("Could not compare repository commits", e);
        }
    }

    @Override
    public void createBranch(String installationToken, String repositoryFullName, String branchName, String fromCommitSha) {
        try {
            String payload = mapper.createObjectNode().put("ref", "refs/heads/" + branchName).put("sha", fromCommitSha).toString();
            HttpRequest request = baseRequest(URI.create("https://api.github.com/repos/" + repositoryFullName + "/git/refs"))
                    .header("Authorization", "Bearer " + installationToken).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
            sendJson(request);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create repository branch", e);
        }
    }

    @Override
    public void deleteBranch(String installationToken, String repositoryFullName, String branchName) {
        try {
            String encoded = java.net.URLEncoder.encode(branchName, StandardCharsets.UTF_8).replace("+", "%20");
            HttpRequest request = baseRequest(URI.create("https://api.github.com/repos/" + repositoryFullName + "/git/refs/heads/" + encoded))
                    .header("Authorization", "Bearer " + installationToken).DELETE().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 204 && response.statusCode() != 404)
                throw new IllegalStateException("GitHub API returned HTTP " + response.statusCode());
        } catch (Exception e) {
            throw new IllegalStateException("Could not delete repository branch", e);
        }
    }

    @Override
    public boolean branchExists(String userAccessToken, String repositoryFullName, String branch) {
        try {
            String encodedBranch = java.net.URLEncoder.encode(branch, StandardCharsets.UTF_8).replace("+", "%20");
            getJson("https://api.github.com/repos/" + repositoryFullName + "/branches/" + encodedBranch, userAccessToken);
            return true;
        } catch (IllegalStateException e) {
            if (hasHttpStatus(e, 404)) return false;
            throw e;
        }
    }

    static boolean hasHttpStatus(Throwable error, int statusCode) {
        String marker = "HTTP " + statusCode;
        for (Throwable current = error; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && message.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean repositoryHasBranches(String userAccessToken, String repositoryFullName) {
        try {
            JsonNode root = getJson("https://api.github.com/repos/" + repositoryFullName + "/branches?per_page=1", userAccessToken);
            return root.isArray() && root.size() > 0;
        } catch (Exception e) {
            throw new IllegalStateException("Could not determine whether repository has branches", e);
        }
    }

    @Override
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
        ensureAppCredentialsConfigured();
        try {
            Instant now = Instant.now();
            String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
            String payload = base64Url("{\"iat\":" + now.minusSeconds(60).getEpochSecond()
                    + ",\"exp\":" + now.plusSeconds(540).getEpochSecond()
                    + ",\"iss\":\"" + appId + "\"}");
            String unsigned = header + "." + payload;
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(parsePrivateKey(privateKeyPem.orElseThrow()));
            signer.update(unsigned.getBytes(StandardCharsets.US_ASCII));
            return unsigned + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign GitHub App JWT", e);
        }
    }

    private void ensureAppCredentialsConfigured() {
        if (appId <= 0 || privateKeyPem.isEmpty() || privateKeyPem.get().isBlank()) {
            throw new IllegalStateException(
                    "GitHub App credentials are not configured. Set GITHUB_APP_ID and GITHUB_APP_PRIVATE_KEY before requesting an installation token.");
        }
    }

    private static String workflowApiIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        int at = value.indexOf('@');
        if (at >= 0) value = value.substring(0, at);
        if (value.matches("[0-9]+")) return value;
        int slash = value.lastIndexOf('/');
        return slash >= 0 ? value.substring(slash + 1) : value;
    }

    private static boolean containsHttpStatus(Throwable error, int status) {
        Throwable current = error;
        String needle = "HTTP " + status;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(needle)) return true;
            current = current.getCause();
        }
        return false;
    }

    private Instant parseInstant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }

    protected JsonNode getJson(String url, String token) {
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
                                     String repositorySelection, String htmlUrl, String contentsPermission) {
        public GitHubInstallation(long id, long accountId, String accountLogin, String accountType,
                                  String repositorySelection, String htmlUrl) {
            this(id, accountId, accountLogin, accountType, repositorySelection, htmlUrl, "");
        }

        public boolean contentsWritable() {
            return "write".equalsIgnoreCase(contentsPermission == null ? "" : contentsPermission.trim());
        }
    }
    public record GitHubRepository(long id, String fullName, boolean privateRepository,
                                   String defaultBranch, String htmlUrl) {}
}
