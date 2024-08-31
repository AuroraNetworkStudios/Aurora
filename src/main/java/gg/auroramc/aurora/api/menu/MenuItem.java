package gg.auroramc.aurora.api.menu;

import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.message.Placeholder;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
public class MenuItem {
    private final ItemBuilder itemBuilder;
    private ItemStack itemStack;
    private final Player player;

    public MenuItem(Player player, ItemBuilder builder, ItemStack itemStack) {
        this.itemBuilder = builder;
        this.itemStack = itemStack;
        this.player = player;
    }

    @Deprecated(forRemoval = true)
    public void applyToInventory(Inventory inventory) {
        if (itemBuilder.getConfig().getSlot() != null) {
            inventory.setItem(itemBuilder.getConfig().getSlot(), itemStack);
        } else if (itemBuilder.getConfig().getSlots() != null && !itemBuilder.getConfig().getSlots().isEmpty()) {
            itemBuilder.getConfig().getSlots().forEach(s -> inventory.setItem(s, itemStack));
        }
    }

    public void runOnClickCommands(Player player) {
        if (itemBuilder.getConfig().getOnClick() == null) return;
        var placeholder = Placeholder.of("{player}", player.getName());
        itemBuilder.getConfig().getOnClick().forEach(c -> CommandDispatcher.dispatch(player, c, placeholder));
    }

    public void runOnLeftClickCommands(Player player) {
        if (itemBuilder.getConfig().getOnLeftClick() == null) return;
        var placeholder = Placeholder.of("{player}", player.getName());
        itemBuilder.getConfig().getOnLeftClick().forEach(c -> CommandDispatcher.dispatch(player, c, placeholder));
    }

    public void runOnRightClickCommands(Player player) {
        if (itemBuilder.getConfig().getOnRightClick() == null) return;
        var placeholder = Placeholder.of("{player}", player.getName());
        itemBuilder.getConfig().getOnRightClick().forEach(c -> CommandDispatcher.dispatch(player, c, placeholder));
    }

    public void refresh() {
        itemStack = itemBuilder.build(player).getItemStack();
    }

    public boolean isRefreshEnabled() {
        return itemBuilder.getConfig().isRefresh();
    }

    public List<Integer> getSlots() {
        return itemBuilder.getConfig().getSlot() != null ? List.of(itemBuilder.getConfig().getSlot()) : itemBuilder.getConfig().getSlots();
    }
}
