package dev.hoodoo.customjukeboxdiscs.youtube;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YouTubeUrl {
    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Pattern QUERY_VALUE = Pattern.compile("(?:^|&)v=([^&]+)");

    private YouTubeUrl() {
    }

    public static Optional<URI> canonicalize(URI uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            return Optional.empty();
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String query = uri.getRawQuery() == null ? "" : uri.getRawQuery();
        if (query.matches("(?:^|.*&)list=.*")) return Optional.empty();

        String id = null;
        if ("youtu.be".equals(host)) {
            id = firstPathPart(uri.getPath());
        } else if (isYouTubeHost(host)) {
            if ("/watch".equals(uri.getPath())) {
                Matcher matcher = QUERY_VALUE.matcher(query);
                if (matcher.find()) id = matcher.group(1);
            } else if (uri.getPath().startsWith("/shorts/")) {
                id = firstPathPart(uri.getPath().substring("/shorts".length()));
            }
        }
        if (id == null || !VIDEO_ID.matcher(id).matches()) return Optional.empty();
        return Optional.of(URI.create("https://www.youtube.com/watch?v=" + id));
    }

    private static boolean isYouTubeHost(String host) {
        return "youtube.com".equals(host)
                || "www.youtube.com".equals(host)
                || "m.youtube.com".equals(host)
                || "music.youtube.com".equals(host);
    }

    private static String firstPathPart(String path) {
        if (path == null || path.length() < 2) return null;
        int nextSlash = path.indexOf('/', 1);
        return nextSlash < 0 ? path.substring(1) : path.substring(1, nextSlash);
    }
}
