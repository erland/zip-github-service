package info.isaksson.erland.zipgithub.github;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class GitRepositoryBootstrapServiceTest {
    @Test
    void createsOneEmptyRootCommitOnRequestedDefaultBranch() throws Exception {
        Path root = Files.createTempDirectory("zip-github-bootstrap-test-");
        try {
            Path remote = root.resolve("remote.git");
            run(root, "git", "init", "--quiet", "--bare", remote.toString());
            GitRepositoryBootstrapService service = new GitRepositoryBootstrapService();

            String sha = service.bootstrapEmptyRepository(remote.toUri(), "", "main");

            assertEquals(sha, run(root, "git", "--git-dir=" + remote, "rev-parse", "refs/heads/main").trim());
            assertEquals("", run(root, "git", "--git-dir=" + remote, "ls-tree", "-r", "refs/heads/main").trim());
            String rootCommit = run(root, "git", "--git-dir=" + remote, "rev-list", "--parents", "-n", "1", "refs/heads/main").trim();
            assertEquals(1, rootCommit.split("\\s+").length, "bootstrap commit must be a root commit without parents");
        } finally {
            try (var stream = Files.walk(root)) {
                for (Path item : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(item);
            }
        }
    }

    private static String run(Path directory, String... command) throws Exception {
        Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) fail("Git command failed: " + output);
        return output;
    }
}
