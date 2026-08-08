package info.isaksson.erland.zipgithub.upload;

/** Deterministic Git mode resolution shared by browser and StagingImport comparison. */
public final class GitFileModeResolver {
    private GitFileModeResolver() { }

    public static String effectiveMode(GitFileMode suppliedMode, String repositoryMode, boolean repositoryFileExists) {
        if (suppliedMode != null) return suppliedMode.gitMode();
        if (repositoryFileExists && ("100644".equals(repositoryMode) || "100755".equals(repositoryMode))) return repositoryMode;
        return "100644";
    }
}
