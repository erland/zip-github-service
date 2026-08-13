package info.isaksson.erland.zipgithub.api.dto;

public record CreateImportRequest(String authorName, String authorEmail, Boolean confirmOpenPullRequest) {
    public boolean confirmsOpenPullRequest() { return Boolean.TRUE.equals(confirmOpenPullRequest); }
}
