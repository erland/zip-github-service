package info.isaksson.erland.zipgithub.github;

import java.util.List;

/** Installation-token scoped branch lifecycle operations used by persistent Work sessions. */
public interface GitHubBranchClient {
    record Branch(String name, String commitSha, boolean protectedBranch) {}
    List<Branch> listBranches(String installationToken, String repositoryFullName);
    String branchHeadSha(String installationToken, String repositoryFullName, String branchName);
    void createBranch(String installationToken, String repositoryFullName, String branchName, String fromCommitSha);
    void deleteBranch(String installationToken, String repositoryFullName, String branchName);
}
