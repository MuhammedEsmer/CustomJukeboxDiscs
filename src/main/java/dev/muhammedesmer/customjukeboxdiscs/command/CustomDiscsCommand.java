package dev.muhammedesmer.customjukeboxdiscs.command;

import com.mojang.authlib.GameProfile;
import dev.muhammedesmer.customjukeboxdiscs.permission.AccessMode;
import dev.muhammedesmer.customjukeboxdiscs.permission.AccessService;
import dev.muhammedesmer.customjukeboxdiscs.server.ServerRuntime;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CustomDiscsCommand {
    private CustomDiscsCommand() { }

    public static void register(RegisterCommandsEvent event) {
        var access = Commands.literal("access")
                .then(Commands.literal("mode")
                        .then(Commands.literal("ops").executes(context -> mode(AccessMode.OPS)))
                        .then(Commands.literal("allowlist").executes(context -> mode(AccessMode.ALLOWLIST)))
                        .then(Commands.literal("everyone").executes(context -> mode(AccessMode.EVERYONE))))
                .then(Commands.literal("allow").then(Commands.argument("players", GameProfileArgument.gameProfile())
                        .executes(context -> players(context, Action.ALLOW))))
                .then(Commands.literal("deny").then(Commands.argument("players", GameProfileArgument.gameProfile())
                        .executes(context -> players(context, Action.DENY))))
                .then(Commands.literal("remove").then(Commands.argument("players", GameProfileArgument.gameProfile())
                        .executes(context -> players(context, Action.REMOVE))));
        event.getDispatcher().register(Commands.literal("customdiscs")
                .requires(source -> source.hasPermission(3))
                .then(access));
    }

    private static int mode(AccessMode mode) {
        ServerRuntime.access().setMode(mode);
        return 1;
    }

    private static int players(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
                               Action action) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
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
        int changedCount = changed;
        context.getSource().sendSuccess(() -> Component.translatable("command.customjukeboxdiscs.access_updated", changedCount), true);
        return changed;
    }

    private enum Action { ALLOW, DENY, REMOVE }
}
