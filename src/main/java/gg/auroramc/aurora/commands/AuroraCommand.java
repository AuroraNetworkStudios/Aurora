package gg.auroramc.aurora.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.command.ArgumentParser;
import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.message.Chat;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.expansions.gui.GuiExpansion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("aurora")
public class AuroraCommand extends BaseCommand {
    private final Aurora plugin;

    public AuroraCommand(Aurora plugin) {
        this.plugin = plugin;
    }

    @Subcommand("reload")
    @CommandPermission("aurora.core.admin.reload")
    public void onReload(CommandSender sender) {
        plugin.reload();
        Chat.sendMessage(sender, Aurora.getMessageConfig().getReloaded());
    }

    @Subcommand("dispatch")
    @CommandCompletion("@players @commandActions @nothing")
    @CommandPermission("aurora.core.admin.dispatch")
    public void onDispatch(CommandSender sender, @Flags("other") Player player, String action, String... args) {
        var command = String.join(" ", args);
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        CommandDispatcher.dispatch(player, action + " " + command);
    }

    @Subcommand("dbmigrate")
    @CommandCompletion("@range:1-16")
    @CommandPermission("aurora.core.admin.dbmigrate")
    public void onDBMigrate(CommandSender sender, @Default("5") Integer threadCount) {
        Chat.sendMessage(sender, Aurora.getMessageConfig().getDbMigrateStarted());

        Aurora.getUserManager().attemptMigration(threadCount).thenAccept(success -> {
            var msg = success ? Aurora.getMessageConfig().getDbMigrateFinished() : Aurora.getMessageConfig().getDbMigrateFailed();
            if (sender instanceof Player player) {
                if (player.isOnline()) {
                    Chat.sendMessage(player, msg);
                }
            }

            if (success) {
                Aurora.logger().info(Aurora.getMessageConfig().getDbMigrateFinished());
            } else {
                Aurora.logger().severe(Aurora.getMessageConfig().getDbMigrateFailed());
            }
        });
    }

    @Subcommand("gui open")
    @CommandCompletion("@players @guiIds @nothing")
    @CommandPermission("aurora.core.admin.gui")
    public void onGuiOpen(CommandSender sender, @Flags("other") Player player, String id, String... args) {
        Aurora.getExpansionManager().getExpansion(GuiExpansion.class).openGui(id, player, ArgumentParser.parseString(String.join(" ", args)));
    }

    @Subcommand("gui reload")
    @CommandPermission("aurora.core.admin.gui")
    public void onGuiReload(CommandSender sender) {
        var guiExpansion = Aurora.getExpansionManager().getExpansion(GuiExpansion.class);
        guiExpansion.reload();
        Chat.sendMessage(sender, Aurora.getMessageConfig().getGuiReloaded(), Placeholder.of("{amount}", guiExpansion.getGuiIds().size()));
    }

    @Subcommand("meta set")
    @CommandCompletion("@players @userMetaKeys @nothing true|false @nothing")
    @CommandPermission("aurora.core.admin.meta")
    public void onMetaSet(CommandSender sender, @Flags("other") Player player, String key, String value, @Default("false") Boolean silent) {
        var user = Aurora.getUserManager().getUser(player);
        boolean success;
        try {
            var number = Double.parseDouble(value);
            success = user.getMetaData().setMeta(key, number);
        } catch (NumberFormatException e) {
            success = user.getMetaData().setMeta(key, value);
        }
        if (!silent) {
            if (success) {
                Chat.sendMessage(sender, Aurora.getMessageConfig().getMetaSet(), Placeholder.of("{key}", key), Placeholder.of("{value}", value));
            } else {
                Chat.sendMessage(sender, Aurora.getMessageConfig().getDataNotLoadedYet());
            }
        }
    }

    @Subcommand("meta get")
    @CommandCompletion("@players @userMetaKeys @nothing")
    @CommandPermission("aurora.core.admin.meta")
    public void onMetaGet(CommandSender sender, @Flags("other") Player player, String key) {
        var user = Aurora.getUserManager().getUser(player);
        if (!user.isLoaded()) {
            Chat.sendMessage(sender, Aurora.getMessageConfig().getDataNotLoadedYet());
            return;
        }

        var value = user.getMetaData().getMeta(key);
        if (key == null || key.isEmpty()) return;
        if (value == null) {
            Chat.sendMessage(sender, Aurora.getMessageConfig().getMetaNotFound(), Placeholder.of("{key}", key));
            return;
        }
        sender.sendMessage(value.toString());
    }

    @Subcommand("meta remove")
    @CommandCompletion("@players @userMetaKeys true|false @nothing")
    @CommandPermission("aurora.core.admin.meta")
    public void onMetaRemove(CommandSender sender, @Flags("other") Player player, String key, @Default("false") Boolean silent) {
        var user = Aurora.getUserManager().getUser(player);
        var success = user.getMetaData().removeMeta(key);
        if (!silent) {
            if (success) {
                Chat.sendMessage(sender, Aurora.getMessageConfig().getMetaRemoved(), Placeholder.of("{key}", key));
            } else {
                Chat.sendMessage(sender, Aurora.getMessageConfig().getDataNotLoadedYet());
            }
        }
    }

    @Subcommand("meta increment")
    @CommandCompletion("@players @userMetaKeys @range:1-100 true|false @nothing")
    @CommandPermission("aurora.core.admin.meta")
    public void onMetaIncrement(CommandSender sender, @Flags("other") Player player, String key, @Default("1") Double value, @Default("false") Boolean silent) {
        var user = Aurora.getUserManager().getUser(player);
        if (key == null || key.isEmpty()) return;
        var success = user.getMetaData().incrementMeta(key, value);
        if (!silent) {
            if (success) {
                Chat.sendMessage(sender, Aurora.getMessageConfig().getMetaIncremented(),
                        Placeholder.of("{key}", key), Placeholder.of("{value}", value),
                        Placeholder.of("{current}", user.getMetaData().getMeta(key, 0.0)));
            } else {
                Chat.sendMessage(sender, Aurora.getMessageConfig().getDataNotLoadedYet());
            }
        }
    }

    @Subcommand("meta decrement")
    @CommandCompletion("@players @userMetaKeys @range:1-100 true|false true|false @nothing")
    @CommandPermission("aurora.core.admin.meta")
    public void onMetaDecrement(CommandSender sender, @Flags("other") Player player, String key, @Default("1") Double value, @Default("false") Boolean allowNegative, @Default("false") Boolean silent) {
        var user = Aurora.getUserManager().getUser(player);
        if (key == null || key.isEmpty()) return;
        var success = user.getMetaData().decrementMeta(key, value, allowNegative);
        if (!silent) {
            if (success) {
                Chat.sendMessage(sender, Aurora.getMessageConfig().getMetaDecremented(),
                        Placeholder.of("{key}", key), Placeholder.of("{value}", value),
                        Placeholder.of("{current}", user.getMetaData().getMeta(key, 0.0)));
            } else {
                Chat.sendMessage(sender, Aurora.getMessageConfig().getDataNotLoadedYet());
            }
        }
    }
}
