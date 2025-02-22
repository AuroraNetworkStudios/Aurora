package gg.auroramc.aurora.expansions.itemstash;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.aurora.api.message.Chat;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.util.ItemUtils;
import gg.auroramc.aurora.expansions.item.ItemExpansion;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
    public void onView(Player sender, @Flags("other") Player target) {
        expansion.open(sender, target);
    }

    @Subcommand("add")
    @CommandCompletion("@players * @range:1-64 true|false @nothing")
    @CommandPermission("aurora.core.admin.stash")
    public void onAdd(CommandSender sender, @Flags("other") Player player, String itemId, @Default("1") Integer amount, @Default("false") Boolean silent) {
        var item = Aurora.getExpansionManager().getExpansion(ItemExpansion.class)
                .getItemManager().resolveItem(TypeId.fromDefault(itemId));

        if (item == null || item.getType() == Material.AIR) {
            if (!silent) {
                Chat.sendMessage(sender, Aurora.getMessageConfig().getItemNotFound(), Placeholder.of("{id}", itemId));
            }
            return;
        }

        var stacks = ItemUtils.createStacksFromAmount(item, amount);
        var stashHolder = Aurora.getUserManager().getUser(player).getStashData();

        for (var stack : stacks) {
            stashHolder.addItem(stack);
        }

        if (!silent) {
            Chat.sendMessage(sender, Aurora.getMessageConfig().getStashItemAdded(), Placeholder.of("{player}", player.getName()));
        }
    }

    @Subcommand("clear")
    @CommandCompletion("@players @nothing")
    @CommandPermission("aurora.core.admin.stash")
    public void onClear(CommandSender sender, @Flags("other") Player player) {
        player.closeInventory();
        Aurora.getUserManager().getUser(player).getStashData().clear();

        Chat.sendMessage(sender, Aurora.getMessageConfig().getStashItemsCleared());
    }
}
