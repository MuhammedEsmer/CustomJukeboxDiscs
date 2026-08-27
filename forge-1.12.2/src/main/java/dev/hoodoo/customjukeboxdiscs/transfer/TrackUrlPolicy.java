package dev.hoodoo.customjukeboxdiscs.transfer;

import com.google.common.net.InetAddresses;
import dev.hoodoo.customjukeboxdiscs.content.disc.AudioFormat;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public final class TrackUrlPolicy {
    private final List<String> allowedHosts;
    private final boolean allowPrivateAddresses;

    public TrackUrlPolicy(List<String> allowedHosts) {
        this(allowedHosts, false);
    }

    public TrackUrlPolicy(List<String> allowedHosts, boolean allowPrivateAddresses) {
        this.allowPrivateAddresses = allowPrivateAddresses;
        this.allowedHosts = allowedHosts.stream()
                .map(host -> host.toLowerCase(Locale.ROOT).trim())
                .filter(host -> !host.isEmpty())
                .collect(Collectors.toList());
    }

    public UploadError check(String url) {
        URI uri = parse(url);
        if (uri == null) {
            return UploadError.URL_NOT_ALLOWED;
        }
        String host = hostOf(uri);
        if (!isAllowedHost(host) || (!allowPrivateAddresses && isInternalName(host))) {
            return UploadError.URL_NOT_ALLOWED;
        }
        return UploadError.NONE;
    }

    public Optional<AudioFormat> formatOf(String url) {
        URI uri = parse(url);
        if (uri == null) {
            return Optional.empty();
        }
        String path = uri.getPath().toLowerCase(Locale.ROOT);
        if (path.endsWith(".mp3")) {
            return Optional.of(AudioFormat.MP3);
        }
        return path.endsWith(".ogg") ? Optional.of(AudioFormat.OGG) : Optional.empty();
    }

    public boolean refusesAddress(InetAddress address) {
        return !allowPrivateAddresses && isInternalAddress(address);
    }

    public static boolean isInternalAddress(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress();
    }

    private static URI parse(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            boolean usable = "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getPath() != null;
            return usable ? uri : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String hostOf(URI uri) {
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
    }

    private boolean isAllowedHost(String host) {
        return allowedHosts.stream().anyMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed));
    }

    private static boolean isInternalName(String host) {
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local")) {
            return true;
        }
        return InetAddresses.isInetAddress(host) && isInternalAddress(InetAddresses.forString(host));
    }
}
