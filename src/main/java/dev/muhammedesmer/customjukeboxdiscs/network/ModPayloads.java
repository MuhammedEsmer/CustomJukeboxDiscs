package dev.muhammedesmer.customjukeboxdiscs.network;

import dev.muhammedesmer.customjukeboxdiscs.network.payload.UploadBeginRequest;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.UploadBeginResponse;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.UploadChunk;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.UploadFinish;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.UploadResult;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.UrlUploadRequest;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.DownloadChunk;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.JukeboxPlay;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.JukeboxStop;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.PlaybackPreference;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.TrackBegin;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.TrackRequest;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.TrackUnavailable;
import java.util.Objects;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ModPayloads {
    public static final String PROTOCOL_VERSION = "1";

    private static volatile ServerHandler serverHandler = ServerHandler.NOT_READY;
    private static volatile ClientHandler clientHandler = ClientHandler.NO_OP;

    private ModPayloads() {
    }

    public static void installServerHandler(ServerHandler handler) {
        serverHandler = Objects.requireNonNull(handler, "handler");
    }

    public static void installClientHandler(ClientHandler handler) {
        clientHandler = Objects.requireNonNull(handler, "handler");
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(UploadBeginRequest.TYPE, UploadBeginRequest.STREAM_CODEC,
                (payload, context) -> serverHandler.handle(payload, context));
        registrar.playToServer(UploadChunk.TYPE, UploadChunk.STREAM_CODEC,
                (payload, context) -> serverHandler.handle(payload, context));
        registrar.playToServer(UploadFinish.TYPE, UploadFinish.STREAM_CODEC,
                (payload, context) -> serverHandler.handle(payload, context));
        registrar.playToServer(TrackRequest.TYPE, TrackRequest.STREAM_CODEC,
                (payload, context) -> serverHandler.handle(payload, context));
        registrar.playToServer(PlaybackPreference.TYPE, PlaybackPreference.STREAM_CODEC,
                (payload, context) -> serverHandler.handle(payload, context));
        registrar.playToServer(UrlUploadRequest.TYPE, UrlUploadRequest.STREAM_CODEC,
                (payload, context) -> serverHandler.handle(payload, context));
        registrar.playToClient(UploadBeginResponse.TYPE, UploadBeginResponse.STREAM_CODEC,
                (payload, context) -> clientHandler.handle(payload, context));
        registrar.playToClient(UploadResult.TYPE, UploadResult.STREAM_CODEC,
                (payload, context) -> clientHandler.handle(payload, context));
        registrar.playToClient(JukeboxPlay.TYPE, JukeboxPlay.STREAM_CODEC,
                (payload, context) -> clientHandler.handle(payload, context));
        registrar.playToClient(JukeboxStop.TYPE, JukeboxStop.STREAM_CODEC,
                (payload, context) -> clientHandler.handle(payload, context));
        registrar.playToClient(TrackBegin.TYPE, TrackBegin.STREAM_CODEC,
                (payload, context) -> clientHandler.handle(payload, context));
        registrar.playToClient(DownloadChunk.TYPE, DownloadChunk.STREAM_CODEC,
                (payload, context) -> clientHandler.handle(payload, context));
        registrar.playToClient(TrackUnavailable.TYPE, TrackUnavailable.STREAM_CODEC,
                (payload, context) -> clientHandler.handle(payload, context));
    }

    public interface ServerHandler {
        ServerHandler NOT_READY = new ServerHandler() {
            @Override
            public void handle(UploadBeginRequest payload, IPayloadContext context) {
                context.disconnect(net.minecraft.network.chat.Component.literal("Custom Jukebox Discs is not ready"));
            }

            @Override
            public void handle(UploadChunk payload, IPayloadContext context) {
                context.disconnect(net.minecraft.network.chat.Component.literal("Custom Jukebox Discs is not ready"));
            }

            @Override
            public void handle(UploadFinish payload, IPayloadContext context) {
                context.disconnect(net.minecraft.network.chat.Component.literal("Custom Jukebox Discs is not ready"));
            }

            @Override
            public void handle(TrackRequest payload, IPayloadContext context) {
                context.disconnect(net.minecraft.network.chat.Component.literal("Custom Jukebox Discs is not ready"));
            }

            @Override
            public void handle(PlaybackPreference payload, IPayloadContext context) {
            }

            @Override
            public void handle(UrlUploadRequest payload, IPayloadContext context) {
                context.disconnect(net.minecraft.network.chat.Component.literal("Custom Jukebox Discs is not ready"));
            }
        };

        void handle(UploadBeginRequest payload, IPayloadContext context);

        void handle(UploadChunk payload, IPayloadContext context);

        void handle(UploadFinish payload, IPayloadContext context);

        void handle(TrackRequest payload, IPayloadContext context);

        void handle(PlaybackPreference payload, IPayloadContext context);

        void handle(UrlUploadRequest payload, IPayloadContext context);
    }

    public interface ClientHandler {
        ClientHandler NO_OP = new ClientHandler() {
            @Override
            public void handle(UploadBeginResponse payload, IPayloadContext context) {
            }

            @Override
            public void handle(UploadResult payload, IPayloadContext context) {
            }

            @Override public void handle(JukeboxPlay payload, IPayloadContext context) { }
            @Override public void handle(JukeboxStop payload, IPayloadContext context) { }
            @Override public void handle(TrackBegin payload, IPayloadContext context) { }
            @Override public void handle(DownloadChunk payload, IPayloadContext context) { }
            @Override public void handle(TrackUnavailable payload, IPayloadContext context) { }
        };

        void handle(UploadBeginResponse payload, IPayloadContext context);

        void handle(UploadResult payload, IPayloadContext context);

        void handle(JukeboxPlay payload, IPayloadContext context);
        void handle(JukeboxStop payload, IPayloadContext context);
        void handle(TrackBegin payload, IPayloadContext context);
        void handle(DownloadChunk payload, IPayloadContext context);
        void handle(TrackUnavailable payload, IPayloadContext context);
    }
}
