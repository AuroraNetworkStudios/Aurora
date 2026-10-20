package gg.auroramc.aurora.api.user.change;

import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.util.InventorySerializer;
import gg.auroramc.aurora.api.util.NamespacedId;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;

/**
 * Takes these exact stacks (same item and amount) from the stash, all of them or none.
 * Use it with {@link gg.auroramc.aurora.api.user.UserManager#requestChange} and give the items away only if it was applied.
 */
public class StashTakeItemsChange implements UserChange {
    public static final NamespacedId TYPE = NamespacedId.fromDefault("stash_take_items");

    private final List<ItemStack> items;

    public StashTakeItemsChange(Collection<ItemStack> items) {
        this.items = List.copyOf(items);
    }

    public StashTakeItemsChange(ConfigurationSection data) {
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
        return user.getStashData().takeItems(items);
    }
}
