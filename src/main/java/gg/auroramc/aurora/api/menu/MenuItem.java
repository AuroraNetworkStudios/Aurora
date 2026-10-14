package gg.auroramc.aurora.api.menu;

import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.message.Placeholder;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@Getter
public class MenuItem {
    private final ItemBuilder itemBuilder;
    @Getter
    private List<Placeholder<?>> placeholders;
    private ItemStack itemStack;
    private final Player player;

    public MenuItem(Player player, ItemBuilder builder, ItemStack itemStack) {
        this.itemBuilder = builder;
        this.itemStack = itemStack;
        this.player = player;
        var placeholders = new ArrayList<>(itemBuilder.getPlaceholders());
        placeholders.add(Placeholder.of("{player}", player.getName()));
        this.placeholders = placeholders;
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
        runOnClickCommands(player, null);
    }

    /**
     * @param requirements checked again after every [delay], see {@link CommandDispatcher#dispatchAll(Player, List, List, BooleanSupplier)}
     */
    public void runOnClickCommands(Player player, @Nullable BooleanSupplier requirements) {
        if (itemBuilder.getConfig().getOnClick() == null) return;
        CommandDispatcher.dispatchAll(player, itemBuilder.getConfig().getOnClick(), placeholders, requirements);
    }

    public void runOnLeftClickCommands(Player player) {
        runOnLeftClickCommands(player, null);
    }

    public void runOnLeftClickCommands(Player player, @Nullable BooleanSupplier requirements) {
        if (itemBuilder.getConfig().getOnLeftClick() == null) return;
        CommandDispatcher.dispatchAll(player, itemBuilder.getConfig().getOnLeftClick(), placeholders, requirements);
    }

    public void runOnRightClickCommands(Player player) {
        runOnRightClickCommands(player, null);
    }

    public void runOnRightClickCommands(Player player, @Nullable BooleanSupplier requirements) {
        if (itemBuilder.getConfig().getOnRightClick() == null) return;
        CommandDispatcher.dispatchAll(player, itemBuilder.getConfig().getOnRightClick(), placeholders, requirements);
    }

    public void runOnShiftLeftClickCommands(Player player) {
        runOnShiftLeftClickCommands(player, null);
    }

    public void runOnShiftLeftClickCommands(Player player, @Nullable BooleanSupplier requirements) {
        if (itemBuilder.getConfig().getOnShiftLeftClick() == null) return;
        CommandDispatcher.dispatchAll(player, itemBuilder.getConfig().getOnShiftLeftClick(), placeholders, requirements);
    }

    public void runOnShiftRightClickCommands(Player player) {
        runOnShiftRightClickCommands(player, null);
    }

    public void runOnShiftRightClickCommands(Player player, @Nullable BooleanSupplier requirements) {
        if (itemBuilder.getConfig().getOnShiftRightClick() == null) return;
        CommandDispatcher.dispatchAll(player, itemBuilder.getConfig().getOnShiftRightClick(), placeholders, requirements);
    }

    public void refresh() {
        itemStack = itemBuilder.build(player).getItemStack();
        var placeholders = new ArrayList<>(itemBuilder.getPlaceholders());
        placeholders.add(Placeholder.of("{player}", player.getName()));
        this.placeholders = placeholders;
    }

    public boolean isRefreshEnabled() {
        return itemBuilder.getConfig().isRefresh();
    }

    public List<Integer> getSlots() {
        return itemBuilder.getConfig().getSlot() != null ? List.of(itemBuilder.getConfig().getSlot()) : itemBuilder.getConfig().getSlots();
    }
}
