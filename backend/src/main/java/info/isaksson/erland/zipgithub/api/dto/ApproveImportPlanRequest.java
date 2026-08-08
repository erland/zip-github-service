package info.isaksson.erland.zipgithub.api.dto;

public record ApproveImportPlanRequest(String planDigestSha256, String selectionDigestSha256, String commitMessage) { }
