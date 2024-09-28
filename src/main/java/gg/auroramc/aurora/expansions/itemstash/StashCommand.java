package gg.auroramc.aurora.expansions.itemstash;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.aurora.api.message.Chat;
import gg.auroramc.aurora.expansions.item.ItemExpansion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("%stashAlias")
public class StashCommand extends BaseCommand {
    private final ItemStashExpansion expansion;

    public StashCommand(ItemStashExpansion expansion) {
        this.expansion = expansion;
    }

    @Default
    public void onDefault(Player player) {
        expansion.open(player);
    }

    @Subcommand("add")
    @CommandCompletion("@players * @range:1-64 @nothing")
    @CommandPermission("aurora.core.admin.stash")
    public void onAdd(CommandSender sender, @Flags("other") Player player, String itemId, @Default("1") Integer amount) {
        var item = Aurora.getExpansionManager().getExpansion(ItemExpansion.class)
                .getItemManager().resolveItem(TypeId.fromDefault(itemId));
        item.setAmount(amount);
        Aurora.getUserManager().getUser(player).getStashData().addItem(item);

        Chat.sendMessage(sender, Aurora.getMessageConfig().getStashItemAdded());
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
