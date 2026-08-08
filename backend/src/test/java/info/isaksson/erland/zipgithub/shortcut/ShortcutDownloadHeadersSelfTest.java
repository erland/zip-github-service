package info.isaksson.erland.zipgithub.shortcut;

public final class ShortcutDownloadHeadersSelfTest {
    public static void main(String[] args) {
        if (!"Skicka till zip-github.shortcut".equals(ShortcutDownloadHeaders.DOWNLOAD_FILENAME)) {
            throw new AssertionError("unexpected Shortcut download filename");
        }
        if (!"attachment; filename=\"Skicka till zip-github.shortcut\"".equals(ShortcutDownloadHeaders.contentDisposition())) {
            throw new AssertionError("unexpected Content-Disposition value");
        }
        System.out.println("ShortcutDownloadHeadersSelfTest OK");
    }
}
