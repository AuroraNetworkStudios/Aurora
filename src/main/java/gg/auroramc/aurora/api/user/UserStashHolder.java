package gg.auroramc.aurora.api.user;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import gg.auroramc.aurora.api.events.itemstash.StashItemAddEvent;
import gg.auroramc.aurora.api.events.itemstash.StashItemRemoveEvent;
import gg.auroramc.aurora.api.util.InventorySerializer;
import gg.auroramc.aurora.api.util.NamespacedId;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@Getter
public class UserStashHolder extends UserDataHolder {
    private final List<ItemStack> items = Lists.newCopyOnWriteArrayList();

    @Override
    public NamespacedId getId() {
        return NamespacedId.fromDefault("stash");
    }

    @Override
    public void serializeInto(ConfigurationSection data) {
        data.set("items", InventorySerializer.serializeItemsAsBase64(items));
    }

    @Override
    public void initFrom(@Nullable ConfigurationSection data) {
        if (data == null) return;
        if (data.contains("items")) {
            items.addAll(Arrays.stream(InventorySerializer.deserializeItemsFromBase64(data.getString("items"))).toList());
        }
    }

    public boolean addItem(ItemStack item) {
        var event = new StashItemAddEvent(getUniqueId(), item);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        for (var current : items) {
            if (current.isSimilar(item)) {
                if (current.getMaxStackSize() >= current.getAmount() + item.getAmount()) {
                    current.setAmount(current.getAmount() + item.getAmount());
                    dirty.set(true);
                    return true;
                } else {
                    item.setAmount(item.getAmount() - (current.getMaxStackSize() - current.getAmount()));
                    current.setAmount(current.getMaxStackSize());
                    if (item.getAmount() == 0) {
                        dirty.set(true);
                        return true;
                    }
                }
            }
        }

        items.add(item);
        dirty.set(true);
        return true;
    }

    public boolean removeItem(ItemStack item) {
        // Check if the item exists in the collection by reference
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == item) { // Reference comparison
                index = i;
                break;
            }
        }

        if (index == -1) return false; // Item not found by reference


        // Fire the event
        var event = new StashItemRemoveEvent(getUniqueId(), item);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        items.remove(index);
        dirty.set(true);
        return true;
    }

    public boolean clear() {
        items.clear();
        dirty.set(true);
        return true;
    }

}
