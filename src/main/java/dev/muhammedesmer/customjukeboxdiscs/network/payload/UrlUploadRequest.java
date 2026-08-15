package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Asks the server to download a track from a link and write it onto the disc in the writer. */
public record UrlUploadRequest(String url, String title, long inputFingerprint) implements CustomPacketPayload {
    public static final int MAX_URL_LENGTH = 512;

    public static final Type<UrlUploadRequest> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "url_upload_request"));
    public static final StreamCodec<FriendlyByteBuf, UrlUploadRequest> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeUtf(value.url, MAX_URL_LENGTH);
                buffer.writeUtf(value.title, TrackReference.MAX_TITLE_CODE_POINTS * 4);
                buffer.writeLong(value.inputFingerprint);
            },
            buffer -> new UrlUploadRequest(
                    buffer.readUtf(MAX_URL_LENGTH),
                    buffer.readUtf(TrackReference.MAX_TITLE_CODE_POINTS * 4),
                    buffer.readLong()));

    public UrlUploadRequest {
        if (url == null || url.isBlank() || url.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("url must contain 1-" + MAX_URL_LENGTH + " characters");
        }
        PayloadValidation.requireCodePoints(title, TrackReference.MAX_TITLE_CODE_POINTS, "title");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
