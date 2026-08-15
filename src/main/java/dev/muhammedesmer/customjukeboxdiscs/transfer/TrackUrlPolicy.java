package dev.muhammedesmer.customjukeboxdiscs.transfer;

import com.google.common.net.InetAddresses;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Decides whether the server is willing to fetch a track from a link.
 *
 * <p>Only plain audio files over https are accepted, and only from hosts the operator listed. This
 * class stays free of name resolution so it can be reasoned about without a network; the address a
 * host actually resolves to is screened by {@link #isInternalAddress(InetAddress)} when fetching.
 */
public final class TrackUrlPolicy {
    private final List<String> allowedHosts;
    private final boolean allowPrivateAddresses;

    public TrackUrlPolicy(List<String> allowedHosts) {
        this(allowedHosts, false);
    }

    /**
     * @param allowPrivateAddresses lets the server fetch from its own network, which an operator needs
     *                              when the audio source runs on the same machine
     */
    public TrackUrlPolicy(List<String> allowedHosts, boolean allowPrivateAddresses) {
        this.allowPrivateAddresses = allowPrivateAddresses;
        this.allowedHosts = allowedHosts.stream()
                .map(host -> host.toLowerCase(Locale.ROOT).strip())
                .filter(host -> !host.isEmpty())
                .toList();
    }

    /** {@return {@link UploadError#NONE} when the link may be fetched} */
    public UploadError check(String url) {
        URI uri = parse(url);
        if (uri == null) {
            return UploadError.URL_NOT_ALLOWED;
        }
        String host = hostOf(uri);
        if (!isAllowedHost(host) || (!allowPrivateAddresses && isInternalName(host))) {
            return UploadError.URL_NOT_ALLOWED;
        }
        // The extension is only a hint. What the bytes actually are is settled by the audio inspector
        // once the file is here, which is what lets a download link without a .mp3 suffix work.
        return UploadError.NONE;
    }

    /** {@return the format the link hints at through its file extension, if it has one} */
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

    /** {@return whether a resolved address must be refused} */
    public boolean refusesAddress(InetAddress address) {
        return !allowPrivateAddresses && isInternalAddress(address);
    }

    /** {@return whether this address belongs to the machine or its private network} */
    public static boolean isInternalAddress(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress();
    }

    private static URI parse(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.strip());
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
        // A literal IPv6 host keeps its brackets in the URI.
        return host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
    }

    private boolean isAllowedHost(String host) {
        return allowedHosts.stream().anyMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed));
    }

    /** Blocks the obvious ways of naming the server itself without asking a name server. */
    private static boolean isInternalName(String host) {
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local")) {
            return true;
        }
        return InetAddresses.isInetAddress(host) && isInternalAddress(InetAddresses.forString(host));
    }
}
