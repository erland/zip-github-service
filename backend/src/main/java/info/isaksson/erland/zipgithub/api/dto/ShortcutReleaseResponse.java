package info.isaksson.erland.zipgithub.api.dto;

public record ShortcutReleaseResponse(boolean available, String version, String generation,
                                      String filename, Long sizeBytes, String sha256, String downloadUrl) {}
