package gg.auroramc.aurora.expansions.itemstash;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.aurora.api.message.Chat;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.user.change.StashAddItemsChange;
import gg.auroramc.aurora.api.user.change.StashClearChange;
import gg.auroramc.aurora.api.util.ItemUtils;
import gg.auroramc.aurora.expansions.item.ItemExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@CommandAlias("%stashAlias")
public class StashCommand extends BaseCommand {
    private final ItemStashExpansion expansion;

    public StashCommand(ItemStashExpansion expansion) {
        this.expansion = expansion;
    }

    @Default
    @CommandPermission("aurora.core.user.stash.open")
    public void onDefault(Player player) {
        expansion.open(player);
    }

    @Subcommand("view")
    @CommandPermission("aurora.core.admin.stash.other")
    @CommandCompletion("@players @nothing")
    public void onView(Player sender, String target) {
        var player = findPlayer(target);
        if (player == null) {
            Chat.sendMessage(sender, Aurora.getMsg(sender).getPlayerNotFound());
            return;
        }
        expansion.open(sender, player);
    }

    @Subcommand("add")
    @CommandCompletion("@players * @range:1-64 true|false @nothing")
    @CommandPermission("aurora.core.admin.stash")
    public void onAdd(CommandSender sender, String target, String itemId, @Default("1") Integer amount, @Default("false") Boolean silent) {
        var messages = Aurora.getMsg(sender);
        var player = findPlayer(target);
        if (player == null) {
            Chat.sendMessage(sender, messages.getPlayerNotFound());
            return;
        }

        var item = Aurora.getExpansionManager().getExpansion(ItemExpansion.class)
                .getItemManager().resolveItem(TypeId.fromDefault(itemId));

        if (item == null || item.getType() == Material.AIR) {
            if (!silent) {
                Chat.sendMessage(sender, messages.getItemNotFound(), Placeholder.of("{id}", itemId));
            }
            return;
        }

        var stacks = ItemUtils.createStacksFromAmount(item, amount);
        Aurora.getUserManager().applyChange(player.getUniqueId(), new StashAddItemsChange(List.of(stacks)));

        if (!silent) {
            Chat.sendMessage(sender, messages.getStashItemAdded(), Placeholder.of("{player}", target));
        }
    }

    @Subcommand("clear")
    @CommandCompletion("@players @nothing")
    @CommandPermission("aurora.core.admin.stash")
    public void onClear(CommandSender sender, String target) {
        var messages = Aurora.getMsg(sender);
        var player = findPlayer(target);
        if (player == null) {
            Chat.sendMessage(sender, messages.getPlayerNotFound());
            return;
        }

        if (player.getPlayer() != null) {
            player.getPlayer().closeInventory();
        }
        Aurora.getUserManager().applyChange(player.getUniqueId(), new StashClearChange());

        Chat.sendMessage(sender, messages.getStashItemsCleared());
    }

    /**
     * Finds a player by UUID or by name, even if they are offline.
     * Names only work for players who have joined this server before, use the UUID for anyone else.
     */
    @Nullable
    private OfflinePlayer findPlayer(String nameOrUuid) {
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(nameOrUuid));
        } catch (IllegalArgumentException e) {
            return Bukkit.getOfflinePlayerIfCached(nameOrUuid);
        }
    }
}
