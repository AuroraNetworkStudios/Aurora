package gg.auroramc.aurora.api.user.change;

import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.util.InventorySerializer;
import gg.auroramc.aurora.api.util.NamespacedId;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;

public class StashAddItemsChange implements UserChange {
    public static final NamespacedId TYPE = NamespacedId.fromDefault("stash_add_items");

    private final List<ItemStack> items;

    public StashAddItemsChange(Collection<ItemStack> items) {
        this.items = List.copyOf(items);
    }

    public StashAddItemsChange(ConfigurationSection data) {
        this.items = List.of(InventorySerializer.deserializeItemsFromBase64(data.getString("items")));
    }

    @Override
    public NamespacedId getType() {
        return TYPE;
    }

    @Override
    public void serializeInto(ConfigurationSection data) {
        data.set("items", InventorySerializer.serializeItemsAsBase64(items));
    }

    @Override
    public boolean apply(AuroraUser user) {
        for (var item : items) {
            user.getStashData().addItem(item);
        }
        return true;
    }
}
