package gg.auroramc.aurora.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.command.ArgumentParser;
import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.aurora.api.message.Chat;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.util.ItemUtils;
import gg.auroramc.aurora.config.MessageConfig;
import gg.auroramc.aurora.expansions.gui.GuiExpansion;
import gg.auroramc.aurora.expansions.item.ItemExpansion;
import gg.auroramc.aurora.expansions.leaderboard.LeaderboardExpansion;
import gg.auroramc.aurora.expansions.region.RegionExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

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
        var messages = Aurora.getMsg(sender);
        Chat.sendMessage(sender, messages.getReloaded());
    }

    @Subcommand("debug blockinfo")
    @CommandPermission("aurora.core.admin.debug.blockinfo")
    public void onBlockInfo(Player player) {
        var regionExpansion = Aurora.getExpansionManager().getExpansion(RegionExpansion.class);
        var block = player.getTargetBlockExact(25);
        if (block == null) return;
        if (!regionExpansion.isPlacedBlock(block)) {
            Chat.sendMessage(player, "&cBlock is not placed by a player.");
            return;
        }
        Chat.sendMessage(player, "&eBlock placed by a player");
    }

    @Subcommand("debug latency")
    @CommandPermission("aurora.core.admin.debug.latency")
    public void onLatencyInfo(CommandSender sender) {
        Chat.sendMessage(sender, "Save: " + Aurora.getUserManager().getSaveLatencyMeasure().prettyPrint());
        Chat.sendMessage(sender, "Load: " + Aurora.getUserManager().getLoadLatencyMeasure().prettyPrint());
        Chat.sendMessage(sender, "Sync flag create: " + Aurora.getUserManager().getSyncFlagLatencyMeasure().prettyPrint());
    }

    @Subcommand("debug blockremove")
    @CommandPermission("aurora.core.admin.debug.blockremove")
    public void onBlockRemove(Player player) {
        var regionExpansion = Aurora.getExpansionManager().getExpansion(RegionExpansion.class);
        var block = player.getTargetBlockExact(25);
        if (block == null) return;
        if (!regionExpansion.isPlacedBlock(block)) {
            Chat.sendMessage(player, "&cBlock is not placed by a player.");
            return;
        }
        regionExpansion.removePlacedBlock(block);
        Chat.sendMessage(player, "&eBlock is now not registered as player placed.");
    }

    @Subcommand("debug itemid")
    @CommandPermission("aurora.core.admin.debug.itemid")
    public void onItemId(Player player) {
        var id = AuroraAPI.getItemManager().resolveId(player.getInventory().getItemInMainHand());
        Chat.sendMessage(player, "&aItem id: " + id.toString());
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
        var messages = Aurora.getMsg(sender);

        Chat.sendMessage(sender, messages.getDbMigrateStarted());

        Aurora.getUserManager().attemptMigration(threadCount).thenAccept(success -> {
            var msg = success ? messages.getDbMigrateFinished() : messages.getDbMigrateFailed();
            if (sender instanceof Player player) {
                if (player.isOnline()) {
                    Chat.sendMessage(player, msg);
                }
            }

            if (success) {
                Aurora.logger().info(messages.getDbMigrateFinished());
            } else {
                Aurora.logger().severe(messages.getDbMigrateFailed());
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
        var messages = Aurora.getMsg(sender);

        var guiExpansion = Aurora.getExpansionManager().getExpansion(GuiExpansion.class);
        guiExpansion.reload();
        Chat.sendMessage(sender, messages.getGuiReloaded(), Placeholder.of("{amount}", guiExpansion.getGuiIds().size()));
    }

    @Subcommand("meta set")
    @CommandCompletion("@players @userMetaKeys @nothing true|false @nothing")
    @CommandPermission("aurora.core.admin.meta")
    public void onMetaSet(CommandSender sender, @Flags("other") Player player, String key, String value, @Default("false") Boolean silent) {
        var messages = Aurora.getMsg(sender);
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
                Chat.sendMessage(sender, messages.getMetaSet(), Placeholder.of("{key}", key), Placeholder.of("{value}", value));
            } else {
                Chat.sendMessage(sender, messages.getDataNotLoadedYet());
            }
        }
    }

    @Subcommand("meta get")
    @CommandCompletion("@players @userMetaKeys @nothing")
    @CommandPermission("aurora.core.admin.meta")
    public void onMetaGet(CommandSender sender, @Flags("other") Player player, String key) {
        var messages = Aurora.getMsg(sender);

        var user = Aurora.getUserManager().getUser(player);
        if (!user.isLoaded()) {
            Chat.sendMessage(sender, messages.getDataNotLoadedYet());
            return;
        }

        var value = user.getMetaData().getMeta(key);
        if (key == null || key.isEmpty()) return;
        if (value == null) {
            Chat.sendMessage(sender, messages.getMetaNotFound(), Placeholder.of("{key}", key));
            return;
        }
        sender.sendMessage(value.toString());
    }

    @Subcommand("meta remove")
    @CommandCompletion("@players @userMetaKeys true|false @nothing")
    @CommandPermission("aurora.core.admin.meta")
    public void onMetaRemove(CommandSender sender, @Flags("other") Player player, String key, @Default("false") Boolean silent) {
        var messages = Aurora.getMsg(sender);

        var user = Aurora.getUserManager().getUser(player);
        var success = user.getMetaData().removeMeta(key);

        if (!silent) {
            if (success) {
                Chat.sendMessage(sender, messages.getMetaRemoved(), Placeholder.of("{key}", key));
            } else {
                Chat.sendMessage(sender, messages.getDataNotLoadedYet());
            }
        }
    }

    @Subcommand("meta increment")
    @CommandCompletion("@players @userMetaKeys @range:1-100 true|false @nothing")
    @CommandPermission("aurora.core.admin.meta")
    public void onMetaIncrement(CommandSender sender, @Flags("other") Player player, String key, @Default("1") Double value, @Default("false") Boolean silent) {
        var messages = Aurora.getMsg(sender);

        var user = Aurora.getUserManager().getUser(player);
        if (key == null || key.isEmpty()) return;
        var success = user.getMetaData().incrementMeta(key, value);

        if (!silent) {
            if (success) {
                Chat.sendMessage(sender, messages.getMetaIncremented(),
                        Placeholder.of("{key}", key), Placeholder.of("{value}", value),
                        Placeholder.of("{current}", user.getMetaData().getMeta(key, 0.0)));
            } else {
                Chat.sendMessage(sender, messages.getDataNotLoadedYet());
            }
        }
    }

    @Subcommand("meta decrement")
    @CommandCompletion("@players @userMetaKeys @range:1-100 true|false true|false @nothing")
    @CommandPermission("aurora.core.admin.meta")
    public void onMetaDecrement(CommandSender sender, @Flags("other") Player player, String key, @Default("1") Double value, @Default("false") Boolean allowNegative, @Default("false") Boolean silent) {
        var messages = Aurora.getMsg(sender);

        var user = Aurora.getUserManager().getUser(player);
        if (key == null || key.isEmpty()) return;
        var success = user.getMetaData().decrementMeta(key, value, allowNegative);

        if (!silent) {
            if (success) {
                Chat.sendMessage(sender, messages.getMetaDecremented(),
                        Placeholder.of("{key}", key), Placeholder.of("{value}", value),
                        Placeholder.of("{current}", user.getMetaData().getMeta(key, 0.0)));
            } else {
                Chat.sendMessage(sender, messages.getDataNotLoadedYet());
            }
        }
    }

    @Subcommand("registeritem")
    @CommandCompletion("@nothing")
    @CommandPermission("aurora.core.admin.registeritem")
    public void onRegisterItem(Player player, String id) {
        if (id == null || id.isEmpty()) return;

        var messages = Aurora.getMsg(player);

        player.getScheduler().run(Aurora.getInstance(), (task) -> {
            var item = player.getInventory().getItemInMainHand();
            var expansion = Aurora.getExpansionManager().getExpansion(ItemExpansion.class);
            expansion.getItemStore().addItem(id, item);
            expansion.getItemStore().saveItems();
            Chat.sendMessage(player, messages.getItemRegistered(), Placeholder.of("{id}", "aurora:" + id));
        }, null);
    }

    @Subcommand("unregisteritem")
    @CommandCompletion("@nothing")
    @CommandPermission("aurora.core.admin.unregisteritem")
    public void onUnRegisterItem(CommandSender sender, String id) {
        if (id == null || id.isEmpty()) return;
        var messages = Aurora.getMsg(sender);
        var expansion = Aurora.getExpansionManager().getExpansion(ItemExpansion.class);
        expansion.getItemStore().removeItem(id);
        expansion.getItemStore().saveItems();
        Chat.sendMessage(sender, messages.getItemUnregistered(), Placeholder.of("{id}", "aurora:" + id));
    }

    @Subcommand("giveitem")
    @CommandCompletion("@players @nothing @range:1-64 @nothing")
    @CommandPermission("aurora.core.admin.giveitem")
    public void onGiveItem(CommandSender sender, @Flags("other") Player player, String id, @Default("1") Integer amount) {
        if (id == null || id.isEmpty()) return;

        var expansion = Aurora.getExpansionManager().getExpansion(ItemExpansion.class);
        var item = expansion.getItemManager().resolveItem(TypeId.fromDefault(id));

        if (item == null || item.getType() == Material.AIR) {
            Chat.sendMessage(sender, "&cItem with id: &4" + id + " &cwas not found.");
            return;
        }

        final ItemStack[] items = ItemUtils.createStacksFromAmount(item, amount);

        player.getScheduler().run(Aurora.getInstance(),
                (task) -> {
                    var failed = player.getInventory().addItem(items);
                    if (!failed.isEmpty()) {
                        Bukkit.getRegionScheduler().run(Aurora.getInstance(), player.getLocation(), (t) -> {
                            failed.forEach((index, itemStack) -> player.getWorld().dropItem(player.getLocation(), itemStack));
                        });
                    }
                }, null);
    }

    @Subcommand("leaderboard clear")
    @CommandCompletion("@leaderboards @nothing")
    @CommandPermission("aurora.core.admin.leaderboard.clear")
    public void onLeaderboardClear(CommandSender sender, String leaderboard) {
        var expansion = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
        var messages = Aurora.getMsg(sender);

        if (expansion.getBoardDescriptor(leaderboard) == null) {
            Chat.sendMessage(sender, messages.getLeaderboardNotExists(), Placeholder.of("{board}", leaderboard));
            return;
        }

        expansion.clearBoard(leaderboard).thenRun(() ->
                Chat.sendMessage(sender, messages.getLeaderboardCleared(), Placeholder.of("{board}", leaderboard)));
    }
}
