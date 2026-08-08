package info.isaksson.erland.zipgithub.api.dto;

public record AbandonWorkRequest(Boolean deleteBranch) {
    public boolean shouldDeleteBranch() { return Boolean.TRUE.equals(deleteBranch); }
}
