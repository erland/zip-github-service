package info.isaksson.erland.zipgithub.upload;

/** Git-relevant mode for ordinary files carried by upload metadata. */
public enum GitFileMode {
    REGULAR("100644"),
    EXECUTABLE("100755");

    private final String gitMode;
    GitFileMode(String gitMode) { this.gitMode = gitMode; }
    public String gitMode() { return gitMode; }

    public static GitFileMode fromUnixMode(int unixMode) {
        if (unixMode == 0) return null;
        return (unixMode & 0111) != 0 ? EXECUTABLE : REGULAR;
    }
}
