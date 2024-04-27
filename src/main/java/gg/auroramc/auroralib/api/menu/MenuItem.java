package gg.auroramc.auroralib.api.menu;

import gg.auroramc.auroralib.api.command.CommandDispatcher;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MenuItem {
    private final ItemBuilder itemBuilder;
    @Getter
    private ItemStack itemStack;
    @Getter
    private final int slot;
    @Getter
    private final Player player;

    public MenuItem(Player player, ItemBuilder builder, ItemStack itemStack, int slot) {
        this.itemBuilder = builder;
        this.itemStack = itemStack;
        this.player = player;
        this.slot = slot;
    }

    public void applyToInventory(Inventory inventory) {
        inventory.setItem(slot, itemStack);
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
        return itemBuilder.getConfig().getRefresh();
    }
}
