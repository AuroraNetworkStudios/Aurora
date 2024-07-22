package gg.auroramc.aurora.api.menu;

import gg.auroramc.aurora.api.command.CommandDispatcher;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MenuItem {
    private final ItemBuilder itemBuilder;
    @Getter
    private ItemStack itemStack;
    @Getter
    private final Player player;

    public MenuItem(Player player, ItemBuilder builder, ItemStack itemStack) {
        this.itemBuilder = builder;
        this.itemStack = itemStack;
        this.player = player;
    }

    public void applyToInventory(Inventory inventory) {
        if(itemBuilder.getConfig().getSlot() != null) {
            inventory.setItem(itemBuilder.getConfig().getSlot(), itemStack);
        } else if(itemBuilder.getConfig().getSlots() != null && !itemBuilder.getConfig().getSlots().isEmpty()) {
            itemBuilder.getConfig().getSlots().forEach(s -> inventory.setItem(s, itemStack));
        }
    }

    public void runOnClickCommands(Player player) {
        if(itemBuilder.getConfig().getOnClick() == null) return;
        itemBuilder.getConfig().getOnClick().forEach(c -> CommandDispatcher.dispatch(player, c));
    }

    public void runOnLeftClickCommands(Player player) {
        if(itemBuilder.getConfig().getOnLeftClick() == null) return;
        itemBuilder.getConfig().getOnLeftClick().forEach(c -> CommandDispatcher.dispatch(player, c));
    }

    public void runOnRightClickCommands(Player player) {
        if(itemBuilder.getConfig().getOnRightClick() == null) return;
        itemBuilder.getConfig().getOnRightClick().forEach(c -> CommandDispatcher.dispatch(player, c));
    }

    public void refresh() {
        itemStack = itemBuilder.build(player).getItemStack();
        if(player.getOpenInventory().getTopInventory() instanceof AuroraMenu menu) {
            applyToInventory(menu.getInventory());
        }
    }

    public boolean isRefreshEnabled() {
        return itemBuilder.getConfig().isRefresh();
    }

    public List<Integer> getSlots() {
        return itemBuilder.getConfig().getSlot() != null ? List.of(itemBuilder.getConfig().getSlot()) : itemBuilder.getConfig().getSlots();
    }
}
