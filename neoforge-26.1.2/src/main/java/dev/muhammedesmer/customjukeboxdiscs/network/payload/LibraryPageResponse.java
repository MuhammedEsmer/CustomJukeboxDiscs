package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LibraryPageResponse(int page, int pageCount, int totalTracks, List<TrackReference> tracks)
        implements CustomPacketPayload {
    public static final int MAX_TRACKS = 20;
    public static final Type<LibraryPageResponse> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "library_page_response"));
    public static final StreamCodec<FriendlyByteBuf, LibraryPageResponse> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.page);
                buffer.writeVarInt(value.pageCount);
                buffer.writeVarInt(value.totalTracks);
                buffer.writeVarInt(value.tracks.size());
                value.tracks.forEach(track -> TrackReference.STREAM_CODEC.encode(buffer, track));
            },
            buffer -> {
                int page = buffer.readVarInt();
                int pageCount = buffer.readVarInt();
                int totalTracks = buffer.readVarInt();
                int count = buffer.readVarInt();
                if (count < 0 || count > MAX_TRACKS) throw new IllegalArgumentException("invalid library page size");
                List<TrackReference> tracks = new ArrayList<>(count);
                for (int index = 0; index < count; index++) tracks.add(TrackReference.STREAM_CODEC.decode(buffer));
                return new LibraryPageResponse(page, pageCount, totalTracks, tracks);
            });

    public LibraryPageResponse {
        if (page < 1 || pageCount < 1 || page > pageCount || totalTracks < 0) {
            throw new IllegalArgumentException("invalid library page metadata");
        }
        tracks = List.copyOf(tracks);
        if (tracks.size() > MAX_TRACKS) throw new IllegalArgumentException("library page is too large");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
