package dev.hoodoo.customjukeboxdiscs.command;

import com.mojang.authlib.GameProfile;
import dev.hoodoo.customjukeboxdiscs.config.ForgeServerConfig;
import dev.hoodoo.customjukeboxdiscs.permission.AccessMode;
import dev.hoodoo.customjukeboxdiscs.permission.AccessService;
import dev.hoodoo.customjukeboxdiscs.server.ServerRuntime;
import dev.hoodoo.customjukeboxdiscs.storage.TrackCatalogSavedData;
import dev.hoodoo.customjukeboxdiscs.storage.TrackMetadata;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

public class CommandCustomDiscs extends CommandBase {

    @Override
    public String getName() {
        return "customdiscs";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.customjukeboxdiscs.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }

        String sub = args[0].toLowerCase();
        if ("reload".equals(sub)) {
            ForgeServerConfig.sync();
            ServerRuntime.getInstance();
            sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.reloaded"));
        } else if ("access".equals(sub)) {
            handleAccess(server, sender, args);
        } else if ("tracks".equals(sub)) {
            handleTracks(server, sender, args);
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    private void handleAccess(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.access_mode",
                    ServerRuntime.access().mode().serializedName()));
            return;
        }

        AccessService service = ServerRuntime.access();
        String action = args[1].toLowerCase();

        if ("mode".equals(action)) {
            if (args.length < 3) {
                sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.access_mode", service.mode().serializedName()));
                return;
            }
            AccessMode mode = AccessMode.fromSerializedName(args[2]);
            service.setMode(mode);
            sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.mode_set", mode.serializedName()));
        } else if ("status".equals(action)) {
            if (args.length < 3) {
                sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.access_mode", service.mode().serializedName()));
                return;
            }
            GameProfile profile = server.getPlayerProfileCache().getGameProfileForUsername(args[2]);
            if (profile == null) {
                throw new CommandException("commands.generic.player.notFound", args[2]);
            }
            UUID id = profile.getId();
            String state = service.isDenied(id) ? "denied" : service.isAllowed(id) ? "allowed" : "default";
            sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.player_status",
                    profile.getName(),
                    new TextComponentTranslation("command.customjukeboxdiscs.state." + state),
                    service.mode().serializedName()));
        } else if ("allow".equals(action) || "deny".equals(action) || "remove".equals(action)) {
            if (args.length < 3) {
                throw new WrongUsageException("commands.customjukeboxdiscs.access.player.usage");
            }
            int count = 0;
            for (int i = 2; i < args.length; i++) {
                GameProfile profile = server.getPlayerProfileCache().getGameProfileForUsername(args[i]);
                if (profile != null) {
                    UUID id = profile.getId();
                    if ("allow".equals(action)) service.allow(id);
                    else if ("deny".equals(action)) service.deny(id);
                    else if ("remove".equals(action)) service.remove(id);
                    count++;
                }
            }
            sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.access_updated", count));
        }
    }

    private void handleTracks(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(getUsage(sender));
        }

        String action = args[1].toLowerCase();
        TrackCatalogSavedData catalog = ServerRuntime.catalog();

        if ("list".equals(action)) {
            int page = 1;
            if (args.length >= 3) {
                page = parseInt(args[2], 1);
            }
            TrackCatalogSavedData.CatalogPage catalogPage = catalog.page(page, 8);
            sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.tracks_header",
                    catalogPage.getPage(), catalogPage.getPageCount(), catalogPage.getTotalTracks()));
            for (TrackMetadata metadata : catalogPage.getEntries()) {
                sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.tracks_entry",
                        metadata.reference().getSha256().substring(0, 12),
                        metadata.reference().getTitle(),
                        metadata.reference().getUploaderName()));
            }
        } else if ("info".equals(action)) {
            if (args.length < 3) {
                throw new WrongUsageException("commands.customjukeboxdiscs.tracks.info.usage");
            }
            String hash = args[2];
            Optional<TrackMetadata> metadata = catalog.find(hash);
            if (!metadata.isPresent()) {
                sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.tracks_unknown", hash));
                return;
            }
            TrackMetadata meta = metadata.get();
            sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.tracks_info",
                    meta.reference().getSha256(),
                    meta.reference().getTitle(),
                    meta.reference().getUploaderName(),
                    meta.reference().getFormat().serializedName(),
                    meta.reference().getDurationMillis() / 1000L,
                    meta.byteCount(),
                    meta.createdAt().toString()));
        } else if ("delete".equals(action)) {
            if (args.length < 3) {
                throw new WrongUsageException("commands.customjukeboxdiscs.tracks.delete.usage");
            }
            String hash = args[2];
            try {
                if (ServerRuntime.maintenance().delete(hash)) {
                    sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.tracks_deleted", hash));
                } else {
                    sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.tracks_unknown", hash));
                }
            } catch (IOException e) {
                sender.sendMessage(new TextComponentTranslation("command.customjukeboxdiscs.tracks_delete_failed", hash));
            }
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "access", "tracks", "reload");
        }
        if (args.length == 2) {
            if ("access".equalsIgnoreCase(args[0])) {
                return getListOfStringsMatchingLastWord(args, "mode", "allow", "deny", "remove", "status");
            }
            if ("tracks".equalsIgnoreCase(args[0])) {
                return getListOfStringsMatchingLastWord(args, "list", "info", "delete");
            }
        }
        if (args.length == 3 && "access".equalsIgnoreCase(args[0]) && "mode".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, "ops", "allowlist", "everyone");
        }
        if (args.length >= 3 && "access".equalsIgnoreCase(args[0]) && ("allow".equalsIgnoreCase(args[1]) || "deny".equalsIgnoreCase(args[1]) || "remove".equalsIgnoreCase(args[1]) || "status".equalsIgnoreCase(args[1]))) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }
}
