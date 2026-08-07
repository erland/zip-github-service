package info.isaksson.erland.zipgithub.security;

import java.net.URI;
import java.util.Locale;

public final class SameOriginPolicy {
    private SameOriginPolicy() {}

    public static boolean matches(String configuredFrontendUrl, String suppliedOrigin) {
        if (configuredFrontendUrl == null || suppliedOrigin == null) return false;
        try {
            URI expected = URI.create(configuredFrontendUrl);
            URI actual = URI.create(suppliedOrigin);
            return normalizeScheme(expected.getScheme()).equals(normalizeScheme(actual.getScheme()))
                    && normalizeHost(expected.getHost()).equals(normalizeHost(actual.getHost()))
                    && effectivePort(expected) == effectivePort(actual)
                    && actual.getRawUserInfo() == null
                    && (actual.getRawPath() == null || actual.getRawPath().isEmpty() || "/".equals(actual.getRawPath()))
                    && actual.getRawQuery() == null
                    && actual.getRawFragment() == null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String normalizeScheme(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
    private static String normalizeHost(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
