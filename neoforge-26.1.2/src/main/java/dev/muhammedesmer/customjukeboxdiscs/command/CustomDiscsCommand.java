package dev.muhammedesmer.customjukeboxdiscs.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.muhammedesmer.customjukeboxdiscs.permission.AccessMode;
import dev.muhammedesmer.customjukeboxdiscs.permission.AccessService;
import dev.muhammedesmer.customjukeboxdiscs.server.ServerRuntime;
import dev.muhammedesmer.customjukeboxdiscs.storage.TrackCatalogData;
import dev.muhammedesmer.customjukeboxdiscs.storage.TrackMetadata;
import java.io.IOException;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CustomDiscsCommand {
    private static final int PAGE_SIZE = 8;

    private CustomDiscsCommand() { }

    public static void register(RegisterCommandsEvent event) {
        var access = Commands.literal("access")
                .then(Commands.literal("mode")
                        .then(Commands.literal("ops").executes(context -> mode(context, AccessMode.OPS)))
                        .then(Commands.literal("allowlist").executes(context -> mode(context, AccessMode.ALLOWLIST)))
                        .then(Commands.literal("everyone").executes(context -> mode(context, AccessMode.EVERYONE))))
                .then(Commands.literal("allow").then(Commands.argument("players", GameProfileArgument.gameProfile())
                        .executes(context -> players(context, Action.ALLOW))))
                .then(Commands.literal("deny").then(Commands.argument("players", GameProfileArgument.gameProfile())
                        .executes(context -> players(context, Action.DENY))))
                .then(Commands.literal("remove").then(Commands.argument("players", GameProfileArgument.gameProfile())
                        .executes(context -> players(context, Action.REMOVE))))
                .then(Commands.literal("status")
                        .executes(CustomDiscsCommand::status)
                        .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                .executes(CustomDiscsCommand::playerStatus)));
        var tracks = Commands.literal("tracks")
                .then(Commands.literal("list")
                        .executes(context -> list(context, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> list(context, IntegerArgumentType.getInteger(context, "page")))))
                .then(Commands.literal("info").then(Commands.argument("sha256", StringArgumentType.word())
                        .executes(CustomDiscsCommand::info)))
                .then(Commands.literal("delete").then(Commands.argument("sha256", StringArgumentType.word())
                        .executes(CustomDiscsCommand::delete)));
        event.getDispatcher().register(Commands.literal("customdiscs")
                .requires(source -> source.hasPermission(3))
                .then(access)
                .then(tracks)
                .then(Commands.literal("reload").executes(CustomDiscsCommand::reload)));
    }

    private static int mode(CommandContext<CommandSourceStack> context, AccessMode mode) {
        ServerRuntime.access().setMode(mode);
        return reply(context, "command.customjukeboxdiscs.mode_set", mode.serializedName());
    }

    private static int players(CommandContext<CommandSourceStack> context, Action action)
            throws CommandSyntaxException {
        AccessService service = ServerRuntime.access();
        int changed = 0;
        for (GameProfile profile : GameProfileArgument.getGameProfiles(context, "players")) {
            switch (action) {
                case ALLOW -> service.allow(profile.getId());
                case DENY -> service.deny(profile.getId());
                case REMOVE -> service.remove(profile.getId());
            }
            changed++;
        }
        reply(context, "command.customjukeboxdiscs.access_updated", changed);
        return changed;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        return reply(context, "command.customjukeboxdiscs.access_mode",
                ServerRuntime.access().mode().serializedName());
    }

    private static int playerStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AccessService service = ServerRuntime.access();
        int reported = 0;
        for (GameProfile profile : GameProfileArgument.getGameProfiles(context, "players")) {
            UUID playerId = profile.getId();
            String state = service.isDenied(playerId) ? "denied" : service.isAllowed(playerId) ? "allowed" : "default";
            reply(context, "command.customjukeboxdiscs.player_status", profile.getName(),
                    Component.translatable("command.customjukeboxdiscs.state." + state),
                    service.mode().serializedName());
            reported++;
        }
        return reported;
    }

    private static int list(CommandContext<CommandSourceStack> context, int page) {
        TrackCatalogData.CatalogPage catalogPage = ServerRuntime.catalog().page(page, PAGE_SIZE);
        reply(context, "command.customjukeboxdiscs.tracks_header",
                catalogPage.page(), catalogPage.pageCount(), catalogPage.totalTracks());
        for (TrackMetadata metadata : catalogPage.entries()) {
            reply(context, "command.customjukeboxdiscs.tracks_entry",
                    metadata.reference().sha256().substring(0, 12),
                    metadata.reference().title(),
                    metadata.reference().uploaderName());
        }
        return catalogPage.entries().size();
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        String hash = StringArgumentType.getString(context, "sha256");
        TrackMetadata metadata = ServerRuntime.catalog().find(hash).orElse(null);
        if (metadata == null) {
            return reply(context, "command.customjukeboxdiscs.tracks_unknown", hash);
        }
        reply(context, "command.customjukeboxdiscs.tracks_info",
                metadata.reference().sha256(),
                metadata.reference().title(),
                metadata.reference().uploaderName(),
                metadata.reference().format().serializedName(),
                metadata.reference().durationMillis() / 1_000L,
                metadata.byteCount(),
                metadata.createdAt().toString());
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> context) {
        String hash = StringArgumentType.getString(context, "sha256");
        try {
            return ServerRuntime.maintenance().delete(hash)
                    ? reply(context, "command.customjukeboxdiscs.tracks_deleted", hash)
                    : reply(context, "command.customjukeboxdiscs.tracks_unknown", hash);
        } catch (IOException exception) {
            return reply(context, "command.customjukeboxdiscs.tracks_delete_failed", hash);
        }
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        ServerRuntime.reloadLimits();
        return reply(context, "command.customjukeboxdiscs.reloaded");
    }

    private static int reply(CommandContext<CommandSourceStack> context, String key, Object... arguments) {
        context.getSource().sendSuccess(() -> Component.translatable(key, arguments), true);
        return 1;
    }

    private enum Action { ALLOW, DENY, REMOVE }
}
