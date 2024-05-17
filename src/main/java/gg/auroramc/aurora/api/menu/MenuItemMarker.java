package gg.auroramc.aurora.api.menu;

import gg.auroramc.aurora.Aurora;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class MenuItemMarker {
    private final NamespacedKey mark;

    public MenuItemMarker(@NotNull Aurora plugin, @NotNull String mark) {
        this.mark = new NamespacedKey(plugin, mark);
    }

    @NotNull
    public ItemStack mark(@NotNull ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null)
            return itemStack;
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.set(this.mark, PersistentDataType.BYTE, Byte.valueOf((byte) 1));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @NotNull
    public ItemStack unmark(@NotNull ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null)
            return itemStack;
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.remove(this.mark);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean isMarked(@NotNull ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null)
            return false;
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        return container.has(this.mark, PersistentDataType.BYTE);
    }
}
